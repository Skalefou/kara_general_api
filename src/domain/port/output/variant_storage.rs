//! Driven port: persist a processed variant.

use async_trait::async_trait;

use crate::domain::error::DomainError;
use crate::domain::model::{ProcessedVariant, StoredObject};

/// Writes a processed variant to the (public) target bucket at a deterministic
/// key, overwriting idempotently. Returns the stored key and size.
#[async_trait]
pub trait VariantStoragePort: Send + Sync {
    async fn store(
        &self,
        bucket: &str,
        key: &str,
        variant: &ProcessedVariant,
    ) -> Result<StoredObject, DomainError>;
}
