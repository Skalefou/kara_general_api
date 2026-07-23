//! Prometheus metrics: counters and histograms exported over HTTP.
//!
//! The exporter listens on `METRICS_PORT`. All helpers are thin wrappers around
//! the `metrics` facade so the rest of the code stays instrumentation-agnostic.

use std::net::SocketAddr;

use ::metrics::{counter, describe_counter, describe_histogram, histogram};
use metrics_exporter_prometheus::PrometheusBuilder;

const JOBS_RECEIVED: &str = "worker_jobs_received_total";
const JOBS_SUCCEEDED: &str = "worker_jobs_succeeded_total";
const JOBS_FAILED: &str = "worker_jobs_failed_total";
const JOBS_POISON: &str = "worker_jobs_poison_total";
const VARIANTS_UPLOADED: &str = "worker_variants_uploaded_total";
const PROCESSING_SECONDS: &str = "worker_processing_duration_seconds";

/// Install the Prometheus exporter and register metric descriptions.
///
/// Must be called from within the Tokio runtime (it spawns an HTTP listener).
pub fn init(port: u16) -> anyhow::Result<()> {
    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    PrometheusBuilder::new()
        .with_http_listener(addr)
        .install()
        .map_err(|e| anyhow::anyhow!("failed to install prometheus exporter: {e}"))?;

    describe_counter!(JOBS_RECEIVED, "Jobs consumed from the image-jobs queue");
    describe_counter!(JOBS_SUCCEEDED, "Jobs that produced an ok result");
    describe_counter!(
        JOBS_FAILED,
        "Jobs that produced a failed result, by error code"
    );
    describe_counter!(JOBS_POISON, "Undeserializable messages routed to the DLQ");
    describe_counter!(VARIANTS_UPLOADED, "Variants successfully uploaded");
    describe_histogram!(PROCESSING_SECONDS, "End-to-end per-job processing duration");
    Ok(())
}

/// A job was consumed from the queue.
pub fn job_received() {
    counter!(JOBS_RECEIVED).increment(1);
}

/// A job completed successfully.
pub fn job_succeeded() {
    counter!(JOBS_SUCCEEDED).increment(1);
}

/// A job failed with the given contract error code.
pub fn job_failed(code: &str) {
    counter!(JOBS_FAILED, "code" => code.to_owned()).increment(1);
}

/// A poison message was dead-lettered.
pub fn job_poison() {
    counter!(JOBS_POISON).increment(1);
}

/// A variant was uploaded to the target bucket.
pub fn variant_uploaded() {
    counter!(VARIANTS_UPLOADED).increment(1);
}

/// Record end-to-end processing time for a job, in seconds.
pub fn processing_seconds(seconds: f64) {
    histogram!(PROCESSING_SECONDS).record(seconds);
}
