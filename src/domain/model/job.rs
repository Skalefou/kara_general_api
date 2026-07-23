//! The job aggregate: a single image-processing request in domain terms.

use crate::domain::model::refs::{SourceRef, TargetRef};
use crate::domain::model::variant::VariantSpec;

/// Correlation identifier, echoed verbatim in the result.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct JobId(String);

impl JobId {
    /// Wrap a raw correlation id.
    pub fn new(value: impl Into<String>) -> Self {
        JobId(value.into())
    }

    /// Borrow the underlying string (used for logging and result correlation).
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl std::fmt::Display for JobId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

/// A single image-processing job, expressed purely in domain terms.
///
/// It carries only what the pipeline needs: the correlation id, where to read
/// the original, where to write the variants, and the requested variants.
#[derive(Debug, Clone)]
pub struct Job {
    pub job_id: JobId,
    pub source: SourceRef,
    pub target: TargetRef,
    pub variants: Vec<VariantSpec>,
}
