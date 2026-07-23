//! The terminal outcome of a job and the report the use case hands back.

use crate::domain::error::ErrorCode;
use crate::domain::model::variant::VariantResult;

/// Terminal outcome of a job — always one of the two, never both.
///
/// Invariant (mirrored on the wire): `Ok` carries the written variants, `Failed`
/// carries a contract code and a human-readable message.
#[derive(Debug, Clone)]
pub enum JobOutcome {
    Ok(Vec<VariantResult>),
    Failed { code: ErrorCode, message: String },
}

/// What the use case returns to its driving adapter.
///
/// The use case **always** produces an [`JobOutcome`] and always attempts to
/// publish it. `published` reports whether that response reached the broker, so
/// the driving adapter can ack (published) or requeue (not published).
#[derive(Debug, Clone)]
pub struct JobReport {
    pub outcome: JobOutcome,
    pub published: bool,
}

#[cfg(test)]
mod tests {
    use super::*;

    fn variant() -> VariantResult {
        VariantResult {
            name: "thumbnail".into(),
            bucket: "pub".into(),
            key: "rooms/r/i/thumbnail.webp".into(),
            width: 320,
            height: 320,
            size_bytes: 1234,
            content_type: "image/webp".into(),
        }
    }

    #[test]
    fn ok_outcome_carries_variants() {
        let outcome = JobOutcome::Ok(vec![variant()]);
        match outcome {
            JobOutcome::Ok(variants) => assert_eq!(variants.len(), 1),
            JobOutcome::Failed { .. } => panic!("expected ok"),
        }
    }

    #[test]
    fn failed_outcome_carries_code_and_message() {
        let outcome = JobOutcome::Failed {
            code: ErrorCode::DownloadFailed,
            message: "not found".into(),
        };
        match outcome {
            JobOutcome::Failed { code, message } => {
                assert_eq!(code, ErrorCode::DownloadFailed);
                assert_eq!(message, "not found");
            }
            JobOutcome::Ok(_) => panic!("expected failed"),
        }
    }
}
