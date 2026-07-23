//! RabbitMQ integration test via `testcontainers`.
//!
//! Marked `#[ignore]` because it needs a Docker daemon and pulls a RabbitMQ
//! image. Run explicitly with:
//!
//! ```bash
//! cargo test --test integration_rabbitmq -- --ignored --nocapture
//! ```
//!
//! It exercises the messaging plumbing that does **not** require object storage:
//! a poison (undeserializable) message published to `image-jobs` must be routed
//! to the `image-jobs.dlq` dead-letter queue by the consumer, without ever
//! calling the use case or S3.

use std::sync::Arc;
use std::time::Duration;

use futures_lite::stream::StreamExt;
use kara_image_worker::application::ProcessImageService;
use kara_image_worker::domain::port::input::ProcessImageUseCase;
use kara_image_worker::domain::port::output::{
    ImageTransformerPort, ResultPublisherPort, SourceImagePort, VariantStoragePort,
};
use kara_image_worker::infrastructure::config::Config;
use kara_image_worker::infrastructure::image::transformer::WebpTransformer;
use kara_image_worker::infrastructure::messaging::consumer;
use kara_image_worker::infrastructure::messaging::publisher::RabbitResultPublisher;
use kara_image_worker::infrastructure::storage::s3::S3Storage;
use lapin::options::{BasicConsumeOptions, BasicPublishOptions, QueueDeclareOptions};
use lapin::types::FieldTable;
use lapin::{BasicProperties, Connection, ConnectionProperties};
use testcontainers::GenericImage;
use testcontainers::core::{IntoContainerPort, WaitFor};
use testcontainers::runners::AsyncRunner;

fn test_config(rabbitmq_url: String) -> Config {
    // Values are provided via env just like production; `Config::load` reads
    // them back and validates. S3 creds are dummies (the poison path never
    // touches storage).
    // SAFETY: single-threaded test setup before any consumer spawns.
    unsafe {
        std::env::set_var("RABBITMQ_URL", &rabbitmq_url);
        std::env::set_var("S3_ACCESS_KEY", "test-access");
        std::env::set_var("S3_SECRET_KEY", "test-secret");
        std::env::set_var("PREFETCH_COUNT", "4");
        std::env::set_var("MAX_CONCURRENCY", "4");
        // Any valid (> 0) port satisfies config validation; the metrics server is
        // never started in this test, so nothing binds it.
        std::env::set_var("METRICS_PORT", "9100");
    }
    Config::load().expect("valid test configuration")
}

/// Build the use case with real adapters. The poison path never invokes it, so
/// the dummy S3 credentials are fine.
fn build_use_case(cfg: &Config, publish_channel: lapin::Channel) -> Arc<dyn ProcessImageUseCase> {
    let s3 = Arc::new(S3Storage::new(cfg));
    let source: Arc<dyn SourceImagePort> = s3.clone();
    let storage: Arc<dyn VariantStoragePort> = s3;
    let transformer: Arc<dyn ImageTransformerPort> =
        Arc::new(WebpTransformer::new(cfg.max_image_bytes, cfg.max_dimension));
    let publisher: Arc<dyn ResultPublisherPort> = Arc::new(RabbitResultPublisher::new(
        publish_channel,
        cfg.queue_results.clone(),
    ));
    Arc::new(ProcessImageService::new(
        source,
        storage,
        transformer,
        publisher,
    ))
}

#[tokio::test]
#[ignore = "requires a Docker daemon; run with --ignored"]
async fn poison_message_is_dead_lettered() {
    let container = GenericImage::new("rabbitmq", "3.13-alpine")
        .with_exposed_port(5672.tcp())
        .with_wait_for(WaitFor::message_on_stdout("Server startup complete"))
        .start()
        .await
        .expect("start rabbitmq container");

    let host = container.get_host().await.expect("host");
    let port = container
        .get_host_port_ipv4(5672.tcp())
        .await
        .expect("mapped amqp port");
    let url = format!("amqp://guest:guest@{host}:{port}/%2f");

    let cfg = test_config(url.clone());

    // Run the consumer in the background; it declares the topology (incl. DLQ).
    let cfg_bg = cfg.clone();
    let handle = tokio::spawn(async move {
        let connection = Connection::connect(&url, ConnectionProperties::default())
            .await
            .expect("consumer connection");
        let publish_channel = connection.create_channel().await.expect("publish channel");
        let use_case = build_use_case(&cfg_bg, publish_channel);
        // Ignore the eventual result: we cancel this task at the end.
        let _ = consumer::run(&connection, &cfg_bg, use_case).await;
    });

    // Give the consumer a moment to declare queues.
    tokio::time::sleep(Duration::from_secs(2)).await;

    // Publish a poison (non-JSON) message to the jobs queue.
    let producer = Connection::connect(&cfg.rabbitmq_url, ConnectionProperties::default())
        .await
        .expect("producer connection");
    let channel = producer.create_channel().await.expect("producer channel");
    channel
        .queue_declare(
            cfg.queue_jobs.as_str().into(),
            QueueDeclareOptions {
                durable: true,
                ..Default::default()
            },
            {
                // Must match the consumer's declaration exactly (the API owns the
                // topology): dead-letter to the `{queue_jobs}.dlx` exchange with the
                // jobs-queue routing key, otherwise the broker returns 406.
                let mut args = FieldTable::default();
                args.insert(
                    "x-dead-letter-exchange".into(),
                    lapin::types::AMQPValue::LongString(
                        format!("{}.dlx", cfg.queue_jobs).as_str().into(),
                    ),
                );
                args.insert(
                    "x-dead-letter-routing-key".into(),
                    lapin::types::AMQPValue::LongString(cfg.queue_jobs.as_str().into()),
                );
                args
            },
        )
        .await
        .expect("declare jobs queue");
    channel
        .basic_publish(
            "".into(),
            cfg.queue_jobs.as_str().into(),
            BasicPublishOptions::default(),
            b"this-is-not-json",
            BasicProperties::default(),
        )
        .await
        .expect("publish poison")
        .await
        .expect("publish confirm");

    // The consumer should reject it to the DLQ. Consume from the DLQ to confirm.
    let dlq = format!("{}.dlq", cfg.queue_jobs);
    let mut dlq_consumer = channel
        .basic_consume(
            dlq.as_str().into(),
            "test-dlq-consumer".into(),
            BasicConsumeOptions::default(),
            FieldTable::default(),
        )
        .await
        .expect("consume dlq");

    let got = tokio::time::timeout(Duration::from_secs(10), dlq_consumer.next()).await;
    assert!(
        matches!(got, Ok(Some(Ok(_)))),
        "poison message should have been dead-lettered to {dlq}"
    );

    handle.abort();
}
