//! Driven port: fetch the original image bytes.

use async_trait::async_trait;

use crate::domain::error::DomainError;
use crate::domain::model::SourceRef;

/// Reads the original object from the (private) source bucket.
///
/// Implementations own their own transient-retry policy; the domain only sees a
/// success (the bytes) or a classified [`DomainError`].
#[async_trait]
pub trait SourceImagePort: Send + Sync {
    async fn fetch(&self, source: &SourceRef) -> Result<Vec<u8>, DomainError>;
}
