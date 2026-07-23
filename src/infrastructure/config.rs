//! Worker configuration, loaded and **validated at startup** (fail-fast).
//!
//! Every variable listed in `CLAUDE.md` §Configuration is read from the
//! environment (a `.env` file is loaded first by `main`). A missing required
//! value or an invalid number aborts the boot instead of failing on the first
//! message.

use anyhow::{Context, ensure};
use serde::Deserialize;

use crate::infrastructure::retry::RetryConfig;

/// Fully-resolved, validated worker configuration.
#[derive(Debug, Clone, Deserialize)]
pub struct Config {
    // RabbitMQ
    pub rabbitmq_url: String,
    #[serde(default = "default_queue_jobs")]
    pub queue_jobs: String,
    #[serde(default = "default_queue_results")]
    pub queue_results: String,
    #[serde(default = "default_prefetch")]
    pub prefetch_count: u16,
    #[serde(default = "default_concurrency")]
    pub max_concurrency: usize,

    // Object storage (GCS interop S3)
    #[serde(default = "default_s3_endpoint")]
    pub s3_endpoint: String,
    #[serde(default = "default_s3_region")]
    pub s3_region: String,
    pub s3_access_key: String,
    pub s3_secret_key: String,

    // Validation limits
    #[serde(default = "default_max_image_bytes")]
    pub max_image_bytes: u64,
    #[serde(default = "default_max_dimension")]
    pub max_dimension: u32,

    // Retry policy
    #[serde(default = "default_retry_max_attempts")]
    pub retry_max_attempts: u32,
    #[serde(default = "default_retry_base_ms")]
    pub retry_base_ms: u64,

    // Observability
    #[serde(default = "default_metrics_port")]
    pub metrics_port: u16,
    #[serde(default = "default_rust_log")]
    pub rust_log: String,
}

impl Config {
    /// Load the configuration from the process environment and validate it.
    ///
    /// Returns an error (aborting the boot) as soon as a required variable is
    /// missing or a value is out of range.
    pub fn load() -> anyhow::Result<Self> {
        let raw = ::config::Config::builder()
            .add_source(::config::Environment::default().try_parsing(true))
            .build()
            .context("failed to read environment configuration")?;

        let cfg: Config = raw
            .try_deserialize()
            .context("invalid worker configuration (check required env vars)")?;

        cfg.validate()?;
        Ok(cfg)
    }

    /// Validate the loaded configuration (fail-fast at startup).
    fn validate(&self) -> anyhow::Result<()> {
        ensure!(
            !self.rabbitmq_url.trim().is_empty(),
            "RABBITMQ_URL must not be empty"
        );
        ensure!(
            !self.queue_jobs.trim().is_empty(),
            "QUEUE_JOBS must not be empty"
        );
        ensure!(
            !self.queue_results.trim().is_empty(),
            "QUEUE_RESULTS must not be empty"
        );
        ensure!(self.prefetch_count > 0, "PREFETCH_COUNT must be > 0");
        ensure!(self.max_concurrency > 0, "MAX_CONCURRENCY must be > 0");
        ensure!(
            !self.s3_endpoint.trim().is_empty(),
            "S3_ENDPOINT must not be empty"
        );
        ensure!(
            !self.s3_region.trim().is_empty(),
            "S3_REGION must not be empty"
        );
        ensure!(
            !self.s3_access_key.trim().is_empty(),
            "S3_ACCESS_KEY is required"
        );
        ensure!(
            !self.s3_secret_key.trim().is_empty(),
            "S3_SECRET_KEY is required"
        );
        ensure!(self.max_image_bytes > 0, "MAX_IMAGE_BYTES must be > 0");
        ensure!(self.max_dimension > 0, "MAX_DIMENSION must be > 0");
        ensure!(
            self.retry_max_attempts >= 1,
            "RETRY_MAX_ATTEMPTS must be >= 1"
        );
        ensure!(self.retry_base_ms > 0, "RETRY_BASE_MS must be > 0");
        ensure!(self.metrics_port > 0, "METRICS_PORT must be > 0");
        Ok(())
    }

    /// Retry policy derived from the loaded configuration.
    pub fn retry(&self) -> RetryConfig {
        RetryConfig {
            max_attempts: self.retry_max_attempts,
            base_ms: self.retry_base_ms,
        }
    }
}

fn default_queue_jobs() -> String {
    "image-jobs".to_string()
}
fn default_queue_results() -> String {
    "image-results".to_string()
}
fn default_prefetch() -> u16 {
    8
}
fn default_concurrency() -> usize {
    8
}
fn default_s3_endpoint() -> String {
    "https://storage.googleapis.com".to_string()
}
fn default_s3_region() -> String {
    "auto".to_string()
}
fn default_max_image_bytes() -> u64 {
    5_242_880
}
fn default_max_dimension() -> u32 {
    8000
}
fn default_retry_max_attempts() -> u32 {
    5
}
fn default_retry_base_ms() -> u64 {
    200
}
fn default_metrics_port() -> u16 {
    9100
}
fn default_rust_log() -> String {
    "info".to_string()
}
