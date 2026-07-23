//! Binary entrypoint — the composition root.
//!
//! The only place that knows all three layers: it loads config, constructs the
//! concrete adapters (S3 storage, WebP transformer, RabbitMQ publisher), injects
//! them into the [`ProcessImageService`] use case, wires the consumer, and runs
//! the consume loop, shutting down cleanly on Ctrl-C.
//!
//! Order: load `.env` -> load + validate config (fail-fast) -> init tracing ->
//! install metrics exporter -> build adapters -> connect to RabbitMQ -> wire the
//! use case -> run the consume loop.

use std::sync::Arc;

use anyhow::Context;
use kara_image_worker::application::ProcessImageService;
use kara_image_worker::domain::port::input::ProcessImageUseCase;
use kara_image_worker::domain::port::output::{
    ImageTransformerPort, ResultPublisherPort, SourceImagePort, VariantStoragePort,
};
use kara_image_worker::infrastructure::config::Config;
use kara_image_worker::infrastructure::image::transformer::WebpTransformer;
use kara_image_worker::infrastructure::messaging::consumer;
use kara_image_worker::infrastructure::messaging::publisher::RabbitResultPublisher;
use kara_image_worker::infrastructure::metrics;
use kara_image_worker::infrastructure::storage::s3::S3Storage;
use lapin::{Connection, ConnectionProperties};
use tracing::{error, info};
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // `.env` is best-effort; real deployments inject env vars directly.
    dotenvy::dotenv().ok();

    let config = Config::load().context("failed to load worker configuration")?;

    init_tracing(&config.rust_log);
    metrics::init(config.metrics_port).context("failed to start metrics exporter")?;

    info!(
        metrics_port = config.metrics_port,
        s3_endpoint = %config.s3_endpoint,
        "kara-image-worker starting"
    );

    // Driven adapters (secondary). S3Storage implements both storage ports.
    let s3 = Arc::new(S3Storage::new(&config));
    let source: Arc<dyn SourceImagePort> = s3.clone();
    let storage: Arc<dyn VariantStoragePort> = s3;
    let transformer: Arc<dyn ImageTransformerPort> = Arc::new(WebpTransformer::new(
        config.max_image_bytes,
        config.max_dimension,
    ));

    let connection = Connection::connect(&config.rabbitmq_url, ConnectionProperties::default())
        .await
        .context("failed to connect to RabbitMQ")?;
    info!("connected to RabbitMQ");

    let publish_channel = connection
        .create_channel()
        .await
        .context("failed to open publish channel")?;
    let publisher: Arc<dyn ResultPublisherPort> = Arc::new(RabbitResultPublisher::new(
        publish_channel,
        config.queue_results.clone(),
    ));

    // Wire the use case with its four driven ports.
    let use_case: Arc<dyn ProcessImageUseCase> = Arc::new(ProcessImageService::new(
        source,
        storage,
        transformer,
        publisher,
    ));

    // Run the consumer (driving adapter) until it ends or Ctrl-C is received.
    tokio::select! {
        result = consumer::run(&connection, &config, Arc::clone(&use_case)) => {
            if let Err(e) = result {
                error!(error = %e, "consumer loop terminated with an error");
                return Err(e);
            }
            info!("consumer loop ended");
        }
        _ = tokio::signal::ctrl_c() => {
            info!("shutdown signal received");
        }
    }

    if let Err(e) = connection.close(0, "worker shutting down".into()).await {
        error!(error = %e, "error while closing RabbitMQ connection");
    }
    info!("kara-image-worker stopped");
    Ok(())
}

/// Initialize `tracing` from the configured `RUST_LOG` filter (falling back to
/// `info`), emitting structured log records.
fn init_tracing(rust_log: &str) {
    let filter = EnvFilter::try_new(rust_log).unwrap_or_else(|_| EnvFilter::new("info"));
    tracing_subscriber::fmt()
        .with_env_filter(filter)
        .with_target(true)
        .init();
}
