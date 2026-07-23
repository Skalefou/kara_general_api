//! Driven port: publish the outcome of a job.

use async_trait::async_trait;

use crate::domain::error::DomainError;
use crate::domain::model::{JobId, JobOutcome};

/// Publishes the terminal outcome of a job (ok or failed) on the results queue,
/// carrying the `jobId` through unchanged. A transport failure is surfaced as a
/// [`DomainError`] so the caller can decide to requeue.
#[async_trait]
pub trait ResultPublisherPort: Send + Sync {
    async fn publish(&self, job_id: &JobId, outcome: &JobOutcome) -> Result<(), DomainError>;
}
