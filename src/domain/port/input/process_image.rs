//! Primary port: process one image job to completion.

use async_trait::async_trait;

use crate::domain::model::{Job, JobReport};

/// The worker's single business use case.
///
/// `process` **always** produces an outcome (ok or failed) and attempts to
/// publish it; the returned [`JobReport`] reports whether the response was
/// delivered. The use case never panics and never returns an error to its
/// caller — a failure to *process* becomes a `failed` outcome, and a failure to
/// *deliver* is reported via `JobReport::published == false`.
#[async_trait]
pub trait ProcessImageUseCase: Send + Sync {
    async fn process(&self, job: Job) -> JobReport;
}
