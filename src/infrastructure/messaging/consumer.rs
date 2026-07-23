//! Driving adapter: consume the `image-jobs` queue (Backend -> Worker).
//!
//! Declares the topology (jobs queue with a dead-letter route, its DLQ, and the
//! results queue), applies prefetch, and for each delivery: deserialize the wire
//! job, map it to a domain [`Job`], invoke the [`ProcessImageUseCase`], then ack
//! **only after** the outcome was published (the use case reports that via
//! `JobReport::published`). An undeserializable message is dead-lettered without
//! processing; a publish failure requeues the (idempotent) job.

use std::sync::Arc;
use std::time::Instant;

use futures_lite::stream::StreamExt;
use lapin::options::{
    BasicAckOptions, BasicConsumeOptions, BasicNackOptions, BasicQosOptions, BasicRejectOptions,
    ExchangeDeclareOptions, QueueBindOptions, QueueDeclareOptions,
};
use lapin::types::{AMQPValue, FieldTable, LongString};
use lapin::{Connection, ExchangeKind, message::Delivery};
use tokio::sync::Semaphore;
use tracing::{error, info, warn};

use crate::domain::model::{JobOutcome, JobReport};
use crate::domain::port::input::ProcessImageUseCase;
use crate::infrastructure::config::Config;
use crate::infrastructure::messaging::contract::{JobMessage, from_wire};
use crate::infrastructure::metrics;

/// Consumer tag used when subscribing to the jobs queue.
const CONSUMER_TAG: &str = "kara-image-worker";

/// Declare topology and run the consume loop until the connection ends.
pub async fn run(
    connection: &Connection,
    cfg: &Config,
    use_case: Arc<dyn ProcessImageUseCase>,
) -> anyhow::Result<()> {
    let channel = connection.create_channel().await?;
    channel
        .basic_qos(cfg.prefetch_count, BasicQosOptions::default())
        .await?;

    declare_topology(&channel, cfg).await?;

    let mut consumer = channel
        .basic_consume(
            cfg.queue_jobs.as_str().into(),
            CONSUMER_TAG.into(),
            BasicConsumeOptions::default(),
            FieldTable::default(),
        )
        .await?;

    let semaphore = Arc::new(Semaphore::new(cfg.max_concurrency));
    info!(
        queue_jobs = %cfg.queue_jobs,
        queue_results = %cfg.queue_results,
        prefetch = cfg.prefetch_count,
        concurrency = cfg.max_concurrency,
        "worker consuming"
    );

    while let Some(delivery) = consumer.next().await {
        let delivery = match delivery {
            Ok(d) => d,
            Err(e) => {
                error!(error = %e, "consumer stream error; stopping loop");
                break;
            }
        };

        let permit = match Arc::clone(&semaphore).acquire_owned().await {
            Ok(p) => p,
            Err(_) => break, // semaphore closed
        };

        let use_case = Arc::clone(&use_case);
        tokio::spawn(async move {
            let _permit = permit; // held for the lifetime of the task
            handle_delivery(delivery, use_case.as_ref()).await;
        });
    }

    Ok(())
}

/// Declare the topology **exactly** as the Backend (`kara_general_api`
/// `RabbitConfig`) declares it — the API owns the contract, so every argument
/// must match or the broker rejects the redeclaration with PRECONDITION_FAILED
/// (406). All durable so the topology survives a broker restart.
///
/// Names are derived from `cfg.queue_jobs` so an override stays consistent:
/// DLX = `{queue_jobs}.dlx` (durable direct exchange), DLQ = `{queue_jobs}.dlq`
/// (durable), bound with routing key `{queue_jobs}`. The jobs queue dead-letters
/// rejected (poison) messages to the DLX with that same routing key.
async fn declare_topology(channel: &lapin::Channel, cfg: &Config) -> anyhow::Result<()> {
    let durable = QueueDeclareOptions {
        durable: true,
        ..QueueDeclareOptions::default()
    };

    let dlx_name = format!("{}.dlx", cfg.queue_jobs);
    let dlq_name = format!("{}.dlq", cfg.queue_jobs);
    let dl_routing_key = cfg.queue_jobs.as_str();

    // 1. Dead-letter exchange: durable direct exchange (mirrors the API DirectExchange).
    channel
        .exchange_declare(
            dlx_name.as_str().into(),
            ExchangeKind::Direct,
            ExchangeDeclareOptions {
                durable: true,
                ..ExchangeDeclareOptions::default()
            },
            FieldTable::default(),
        )
        .await?;

    // 2. Dead-letter queue (durable).
    channel
        .queue_declare(dlq_name.as_str().into(), durable, FieldTable::default())
        .await?;

    // 3. Bind the DLQ to the DLX with the jobs-queue routing key.
    channel
        .queue_bind(
            dlq_name.as_str().into(),
            dlx_name.as_str().into(),
            dl_routing_key.into(),
            QueueBindOptions::default(),
            FieldTable::default(),
        )
        .await?;

    // 4. Jobs queue: durable, dead-lettering rejected (poison) messages to the DLX
    //    with the jobs-queue routing key — identical to the API's declaration.
    let mut jobs_args = FieldTable::default();
    jobs_args.insert(
        "x-dead-letter-exchange".into(),
        AMQPValue::LongString(LongString::from(dlx_name.as_str())),
    );
    jobs_args.insert(
        "x-dead-letter-routing-key".into(),
        AMQPValue::LongString(LongString::from(dl_routing_key)),
    );
    channel
        .queue_declare(cfg.queue_jobs.as_str().into(), durable, jobs_args)
        .await?;

    // 5. Results queue (durable).
    channel
        .queue_declare(
            cfg.queue_results.as_str().into(),
            durable,
            FieldTable::default(),
        )
        .await?;

    Ok(())
}

/// Handle one delivery end-to-end. Never panics: any processing failure is
/// turned into a `failed` outcome by the use case, and a poison message is
/// dead-lettered.
async fn handle_delivery(delivery: Delivery, use_case: &dyn ProcessImageUseCase) {
    metrics::job_received();

    let message: JobMessage = match serde_json::from_slice(&delivery.data) {
        Ok(job) => job,
        Err(e) => {
            warn!(error = %e, "undeserializable message; dead-lettering (poison)");
            metrics::job_poison();
            if let Err(e) = delivery.reject(BasicRejectOptions { requeue: false }).await {
                error!(error = %e, "failed to reject poison message");
            }
            return;
        }
    };

    let job = from_wire(message);
    let job_id = job.job_id.as_str().to_string();
    let started = Instant::now();

    let JobReport { outcome, published } = use_case.process(job).await;

    match &outcome {
        JobOutcome::Ok(variants) => {
            metrics::job_succeeded();
            info!(job_id = %job_id, variants = variants.len(), "job succeeded");
        }
        JobOutcome::Failed { code, message } => {
            metrics::job_failed(&format!("{code:?}"));
            warn!(job_id = %job_id, code = ?code, error = %message, "job failed");
        }
    }
    metrics::processing_seconds(started.elapsed().as_secs_f64());

    // Ack only after the outcome was published. If publishing failed, requeue so
    // the job is retried later (idempotent output keys make this safe).
    if published {
        if let Err(e) = delivery.ack(BasicAckOptions::default()).await {
            error!(job_id = %job_id, error = %e, "failed to ack after publishing result");
        }
    } else {
        error!(job_id = %job_id, "result not published; requeuing job");
        if let Err(e) = delivery
            .nack(BasicNackOptions {
                requeue: true,
                ..BasicNackOptions::default()
            })
            .await
        {
            error!(job_id = %job_id, error = %e, "failed to nack after publish failure");
        }
    }
}
