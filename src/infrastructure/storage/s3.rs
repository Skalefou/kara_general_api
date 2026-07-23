//! Driven adapter: S3 client for Google Cloud Storage (S3 interop, path-style,
//! HMAC creds), implementing [`SourceImagePort`] and [`VariantStoragePort`].
//!
//! Reads originals from the private source bucket, writes variants to the public
//! target bucket. Transient storage failures (timeouts, 5xx, throttling,
//! network) are retried in-process via the shared retry helper; 4xx service
//! errors are permanent. Failures are classified into a [`DomainError`] before
//! they cross back into the domain.

use async_trait::async_trait;
use aws_sdk_s3::Client;
use aws_sdk_s3::config::{BehaviorVersion, Credentials, Region};
use aws_sdk_s3::error::SdkError;
use aws_sdk_s3::primitives::ByteStream;

use crate::domain::error::DomainError;
use crate::domain::model::{ProcessedVariant, SourceRef, StoredObject};
use crate::domain::port::output::{SourceImagePort, VariantStoragePort};
use crate::infrastructure::config::Config;
use crate::infrastructure::metrics;
use crate::infrastructure::retry::{RetryConfig, retry_async};

/// Object-storage gateway. Cheaply cloneable (`Client` is an `Arc` inside).
#[derive(Clone)]
pub struct S3Storage {
    client: Client,
    retry: RetryConfig,
}

impl S3Storage {
    /// Build the S3 client from configuration (GCS interop endpoint, path-style,
    /// HMAC keys mapped onto static AWS credentials).
    pub fn new(cfg: &Config) -> Self {
        let credentials = Credentials::new(
            cfg.s3_access_key.clone(),
            cfg.s3_secret_key.clone(),
            None,
            None,
            "kara-image-worker",
        );

        let s3_config = aws_sdk_s3::config::Builder::default()
            .behavior_version(BehaviorVersion::latest())
            .region(Region::new(cfg.s3_region.clone()))
            .endpoint_url(cfg.s3_endpoint.clone())
            .force_path_style(true)
            .credentials_provider(credentials)
            .build();

        Self {
            client: Client::from_conf(s3_config),
            retry: cfg.retry(),
        }
    }
}

#[async_trait]
impl SourceImagePort for S3Storage {
    /// Download the original object from the (private) source bucket.
    async fn fetch(&self, source: &SourceRef) -> Result<Vec<u8>, DomainError> {
        retry_async(&self.retry, || async {
            let output = self
                .client
                .get_object()
                .bucket(&source.bucket)
                .key(&source.key)
                .send()
                .await
                .map_err(classify_download)?;

            let bytes = output
                .body
                .collect()
                .await
                // A broken body stream mid-download is transient.
                .map_err(|e| DomainError::download(format!("body stream: {e}"), true))?;

            Ok(bytes.into_bytes().to_vec())
        })
        .await
    }
}

#[async_trait]
impl VariantStoragePort for S3Storage {
    /// Upload a variant to the (public) target bucket with retry on transient
    /// failures. Overwrites idempotently (deterministic keys).
    async fn store(
        &self,
        bucket: &str,
        key: &str,
        variant: &ProcessedVariant,
    ) -> Result<StoredObject, DomainError> {
        retry_async(&self.retry, || async {
            self.client
                .put_object()
                .bucket(bucket)
                .key(key)
                .body(ByteStream::from(variant.bytes.clone()))
                .content_type(variant.content_type())
                .send()
                .await
                .map_err(classify_upload)?;
            Ok(())
        })
        .await?;

        metrics::variant_uploaded();
        Ok(StoredObject {
            key: key.to_string(),
            size_bytes: variant.size_bytes(),
        })
    }
}

/// Map a GET error to a download [`DomainError`] with the right transient flag.
fn classify_download<E: std::fmt::Debug>(err: SdkError<E>) -> DomainError {
    DomainError::download(format!("{err:?}"), is_transient(&err))
}

/// Map a PUT error to an upload [`DomainError`] with the right transient flag.
fn classify_upload<E: std::fmt::Debug>(err: SdkError<E>) -> DomainError {
    DomainError::upload(format!("{err:?}"), is_transient(&err))
}

/// Decide whether an SDK error is transient (retryable).
///
/// Timeouts, dispatch failures, malformed responses and 5xx/429/408 service
/// errors are transient; every other service error (e.g. 404, 403) is permanent.
fn is_transient<E>(err: &SdkError<E>) -> bool {
    match err {
        SdkError::TimeoutError(_) | SdkError::DispatchFailure(_) | SdkError::ResponseError(_) => {
            true
        }
        _ => err
            .raw_response()
            .map(|resp| {
                let status = resp.status().as_u16();
                status >= 500 || status == 429 || status == 408
            })
            .unwrap_or(false),
    }
}
