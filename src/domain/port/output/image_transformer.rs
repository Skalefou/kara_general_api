//! Driven port: validate and transform an original image into variants.

use async_trait::async_trait;

use crate::domain::error::DomainError;
use crate::domain::model::{ProcessedVariant, VariantSpec};

/// Validates the original bytes (magic bytes, allowed format, size/dimension
/// caps) and produces one [`ProcessedVariant`] per requested spec.
///
/// Implementations run the CPU-bound work off the async reactor. The domain
/// only sees the encoded variants or a classified [`DomainError`]
/// (`UNSUPPORTED_FORMAT` / `DECODE_FAILED` / `RESIZE_FAILED`).
#[async_trait]
pub trait ImageTransformerPort: Send + Sync {
    async fn transform(
        &self,
        original: &[u8],
        variants: &[VariantSpec],
    ) -> Result<Vec<ProcessedVariant>, DomainError>;
}
