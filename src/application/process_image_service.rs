//! `ProcessImageService` — the concrete use case that orchestrates the pipeline.

use std::sync::Arc;

use async_trait::async_trait;

use crate::domain::error::DomainError;
use crate::domain::model::{Job, JobOutcome, JobReport, VariantResult};
use crate::domain::port::input::ProcessImageUseCase;
use crate::domain::port::output::{
    ImageTransformerPort, ResultPublisherPort, SourceImagePort, VariantStoragePort,
};

/// Orchestrates the four driven ports to process one job.
///
/// Retry policy: transient failures are retried inside the driven adapters
/// (backoff, bounded attempts); a permanent failure aborts the pipeline
/// immediately and becomes a `failed` outcome. Output keys are deterministic
/// (`{key_prefix}/{name}.{ext}`), so a replay overwrites the same objects —
/// idempotent by construction. A response is **always** produced and published.
pub struct ProcessImageService {
    source: Arc<dyn SourceImagePort>,
    storage: Arc<dyn VariantStoragePort>,
    transformer: Arc<dyn ImageTransformerPort>,
    publisher: Arc<dyn ResultPublisherPort>,
}

impl ProcessImageService {
    /// Inject the four driven adapters.
    pub fn new(
        source: Arc<dyn SourceImagePort>,
        storage: Arc<dyn VariantStoragePort>,
        transformer: Arc<dyn ImageTransformerPort>,
        publisher: Arc<dyn ResultPublisherPort>,
    ) -> Self {
        Self {
            source,
            storage,
            transformer,
            publisher,
        }
    }

    /// The core pipeline: download → transform → upload. Returns the written
    /// variants, or the first classified failure encountered.
    async fn run_pipeline(&self, job: &Job) -> Result<Vec<VariantResult>, DomainError> {
        // 1. Download the original (private bucket). Transient retry lives in
        //    the adapter; a permanent error propagates immediately.
        let bytes = self.source.fetch(&job.source).await?;

        // 2. Validate + resize + encode every variant (CPU-bound work runs off
        //    the async reactor inside the adapter).
        let processed = self.transformer.transform(&bytes, &job.variants).await?;

        // 3. Upload every variant to deterministic keys (idempotent overwrite).
        let prefix = job.target.key_prefix.trim_end_matches('/');
        let mut results = Vec::with_capacity(processed.len());
        for variant in &processed {
            let key = format!("{prefix}/{}.{}", variant.name, variant.format.extension());
            let stored = self
                .storage
                .store(&job.target.bucket, &key, variant)
                .await?;
            results.push(VariantResult {
                name: variant.name.clone(),
                bucket: job.target.bucket.clone(),
                key: stored.key,
                width: variant.width,
                height: variant.height,
                size_bytes: stored.size_bytes,
                content_type: variant.content_type().to_string(),
            });
        }
        Ok(results)
    }
}

#[async_trait]
impl ProcessImageUseCase for ProcessImageService {
    async fn process(&self, job: Job) -> JobReport {
        let outcome = match self.run_pipeline(&job).await {
            Ok(variants) => JobOutcome::Ok(variants),
            Err(err) => JobOutcome::Failed {
                code: err.code(),
                message: err.to_string(),
            },
        };

        // Always respond. `published == false` tells the driving adapter the
        // response did not reach the broker, so it should requeue the job
        // (idempotent output keys make redelivery safe).
        let published = self.publisher.publish(&job.job_id, &outcome).await.is_ok();

        JobReport { outcome, published }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::domain::error::ErrorCode;
    use crate::domain::model::{
        Fit, Format, JobId, ProcessedVariant, SourceRef, StoredObject, TargetRef, VariantSpec,
    };
    use std::sync::Mutex;

    // ---- In-memory fakes for the four driven ports (no infrastructure) ----

    #[derive(Default)]
    struct FakeSource {
        bytes: Vec<u8>,
        fail: Option<DomainError>,
    }
    #[async_trait]
    impl SourceImagePort for FakeSource {
        async fn fetch(&self, _source: &SourceRef) -> Result<Vec<u8>, DomainError> {
            match &self.fail {
                Some(DomainError::Download { message, transient }) => {
                    Err(DomainError::download(message.clone(), *transient))
                }
                Some(other) => Err(DomainError::Internal(other.to_string())),
                None => Ok(self.bytes.clone()),
            }
        }
    }

    struct FakeTransformer {
        fail: Option<&'static str>,
    }
    #[async_trait]
    impl ImageTransformerPort for FakeTransformer {
        async fn transform(
            &self,
            _original: &[u8],
            variants: &[VariantSpec],
        ) -> Result<Vec<ProcessedVariant>, DomainError> {
            if let Some(msg) = self.fail {
                return Err(DomainError::UnsupportedFormat(msg.to_string()));
            }
            Ok(variants
                .iter()
                .map(|spec| ProcessedVariant {
                    name: spec.name.clone(),
                    bytes: vec![1, 2, 3, 4],
                    width: spec.width,
                    height: spec.height,
                    format: spec.format,
                })
                .collect())
        }
    }

    #[derive(Default)]
    struct FakeStorage {
        keys: Mutex<Vec<String>>,
    }
    #[async_trait]
    impl VariantStoragePort for FakeStorage {
        async fn store(
            &self,
            _bucket: &str,
            key: &str,
            variant: &ProcessedVariant,
        ) -> Result<StoredObject, DomainError> {
            self.keys.lock().unwrap().push(key.to_string());
            Ok(StoredObject {
                key: key.to_string(),
                size_bytes: variant.size_bytes(),
            })
        }
    }

    #[derive(Default)]
    struct FakePublisher {
        fail: bool,
        published: Mutex<Vec<JobOutcome>>,
    }
    #[async_trait]
    impl ResultPublisherPort for FakePublisher {
        async fn publish(&self, _job_id: &JobId, outcome: &JobOutcome) -> Result<(), DomainError> {
            if self.fail {
                return Err(DomainError::Internal("publish down".into()));
            }
            self.published.lock().unwrap().push(outcome.clone());
            Ok(())
        }
    }

    fn job() -> Job {
        Job {
            job_id: JobId::new("job-1"),
            source: SourceRef {
                bucket: "priv".into(),
                key: "rooms/r/originals/i.jpg".into(),
                content_type: "image/jpeg".into(),
            },
            target: TargetRef {
                bucket: "pub".into(),
                key_prefix: "rooms/r/i/".into(),
            },
            variants: vec![VariantSpec {
                name: "thumbnail".into(),
                width: 320,
                height: 320,
                fit: Fit::Cover,
                format: Format::Webp,
            }],
        }
    }

    fn service(
        source: FakeSource,
        transformer: FakeTransformer,
        storage: Arc<FakeStorage>,
        publisher: Arc<FakePublisher>,
    ) -> ProcessImageService {
        ProcessImageService::new(Arc::new(source), storage, Arc::new(transformer), publisher)
    }

    #[tokio::test]
    async fn happy_path_uploads_deterministic_keys_and_reports_ok() {
        let storage = Arc::new(FakeStorage::default());
        let publisher = Arc::new(FakePublisher::default());
        let svc = service(
            FakeSource {
                bytes: vec![0xFF; 16],
                fail: None,
            },
            FakeTransformer { fail: None },
            storage.clone(),
            publisher.clone(),
        );

        let report = svc.process(job()).await;
        assert!(report.published);
        match report.outcome {
            JobOutcome::Ok(variants) => {
                assert_eq!(variants.len(), 1);
                // Deterministic key: trailing slash on the prefix is normalized.
                assert_eq!(variants[0].key, "rooms/r/i/thumbnail.webp");
                assert_eq!(variants[0].content_type, "image/webp");
            }
            JobOutcome::Failed { .. } => panic!("expected ok"),
        }
        assert_eq!(
            storage.keys.lock().unwrap().as_slice(),
            ["rooms/r/i/thumbnail.webp"]
        );
    }

    #[tokio::test]
    async fn permanent_transform_failure_becomes_failed_outcome() {
        let storage = Arc::new(FakeStorage::default());
        let publisher = Arc::new(FakePublisher::default());
        let svc = service(
            FakeSource {
                bytes: vec![0; 8],
                fail: None,
            },
            FakeTransformer {
                fail: Some("bad magic"),
            },
            storage.clone(),
            publisher.clone(),
        );

        let report = svc.process(job()).await;
        assert!(report.published);
        match report.outcome {
            JobOutcome::Failed { code, .. } => assert_eq!(code, ErrorCode::UnsupportedFormat),
            JobOutcome::Ok(_) => panic!("expected failed"),
        }
        // Nothing was uploaded because transform failed first.
        assert!(storage.keys.lock().unwrap().is_empty());
    }

    #[tokio::test]
    async fn download_failure_becomes_failed_outcome() {
        let storage = Arc::new(FakeStorage::default());
        let publisher = Arc::new(FakePublisher::default());
        let svc = service(
            FakeSource {
                bytes: vec![],
                fail: Some(DomainError::download("not found", false)),
            },
            FakeTransformer { fail: None },
            storage.clone(),
            publisher.clone(),
        );

        let report = svc.process(job()).await;
        match report.outcome {
            JobOutcome::Failed { code, .. } => assert_eq!(code, ErrorCode::DownloadFailed),
            JobOutcome::Ok(_) => panic!("expected failed"),
        }
    }

    #[tokio::test]
    async fn publish_failure_is_reported_as_unpublished() {
        let storage = Arc::new(FakeStorage::default());
        let publisher = Arc::new(FakePublisher {
            fail: true,
            published: Mutex::new(Vec::new()),
        });
        let svc = service(
            FakeSource {
                bytes: vec![0xFF; 16],
                fail: None,
            },
            FakeTransformer { fail: None },
            storage.clone(),
            publisher.clone(),
        );

        let report = svc.process(job()).await;
        // Outcome still produced, but delivery failed -> caller requeues.
        assert!(!report.published);
        assert!(matches!(report.outcome, JobOutcome::Ok(_)));
    }
}
