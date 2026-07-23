//! Domain error type and the frozen contract error codes.
//!
//! Every failure that can happen while processing a job is expressed as a
//! [`DomainError`], which maps deterministically to one of the frozen contract
//! codes ([`ErrorCode`]) and classifies itself as transient or permanent. The
//! wire (de)serialization of [`ErrorCode`] lives in
//! [`crate::infrastructure::messaging::contract`] — the domain stays free of
//! wire `serde`.

use thiserror::Error;

/// Frozen enumeration of contract error codes.
///
/// Rendered as `SCREAMING_SNAKE_CASE` on the wire, but that mapping belongs to
/// the messaging adapter, not here. This is a plain domain enum.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ErrorCode {
    DownloadFailed,
    UnsupportedFormat,
    DecodeFailed,
    ResizeFailed,
    UploadFailed,
    Timeout,
    Internal,
}

/// Retry classification of a failure.
///
/// Transient failures (timeouts, 5xx, throttling, network) may be retried with
/// backoff; permanent failures (decode/format/validation) fail immediately.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ErrorClass {
    Transient,
    Permanent,
}

/// Typed domain error. Each variant maps to exactly one contract [`ErrorCode`].
///
/// Storage failures carry a `transient` flag because the same variant may be
/// transient (timeout / 5xx) or permanent (404 / 403) depending on the cause.
#[derive(Debug, Error)]
pub enum DomainError {
    /// Original object could not be downloaded from the source bucket.
    #[error("download failed: {message}")]
    Download { message: String, transient: bool },

    /// The file is not an accepted format, or exceeds the size/dimension caps.
    #[error("unsupported format: {0}")]
    UnsupportedFormat(String),

    /// The bytes are a claimed image but could not be decoded.
    #[error("decode failed: {0}")]
    Decode(String),

    /// Resizing or WebP encoding failed.
    #[error("resize failed: {0}")]
    Resize(String),

    /// A variant could not be uploaded to the target bucket.
    #[error("upload failed: {message}")]
    Upload { message: String, transient: bool },

    /// An operation exceeded its allotted time budget (always transient).
    #[error("timeout: {0}")]
    Timeout(String),

    /// Any other unexpected failure.
    #[error("internal error: {0}")]
    Internal(String),
}

impl DomainError {
    /// Build a download failure with an explicit transient/permanent class.
    pub fn download(message: impl Into<String>, transient: bool) -> Self {
        DomainError::Download {
            message: message.into(),
            transient,
        }
    }

    /// Build an upload failure with an explicit transient/permanent class.
    pub fn upload(message: impl Into<String>, transient: bool) -> Self {
        DomainError::Upload {
            message: message.into(),
            transient,
        }
    }

    /// The frozen contract code that must be reported for this error.
    pub fn code(&self) -> ErrorCode {
        match self {
            DomainError::Download { .. } => ErrorCode::DownloadFailed,
            DomainError::UnsupportedFormat(_) => ErrorCode::UnsupportedFormat,
            DomainError::Decode(_) => ErrorCode::DecodeFailed,
            DomainError::Resize(_) => ErrorCode::ResizeFailed,
            DomainError::Upload { .. } => ErrorCode::UploadFailed,
            DomainError::Timeout(_) => ErrorCode::Timeout,
            DomainError::Internal(_) => ErrorCode::Internal,
        }
    }

    /// Whether this failure may be retried with backoff.
    pub fn is_transient(&self) -> bool {
        match self {
            DomainError::Download { transient, .. } => *transient,
            DomainError::Upload { transient, .. } => *transient,
            DomainError::Timeout(_) => true,
            DomainError::UnsupportedFormat(_)
            | DomainError::Decode(_)
            | DomainError::Resize(_)
            | DomainError::Internal(_) => false,
        }
    }

    /// Retry classification derived from [`Self::is_transient`].
    pub fn class(&self) -> ErrorClass {
        if self.is_transient() {
            ErrorClass::Transient
        } else {
            ErrorClass::Permanent
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_variant_maps_to_expected_code() {
        assert_eq!(
            DomainError::download("x", true).code(),
            ErrorCode::DownloadFailed
        );
        assert_eq!(
            DomainError::UnsupportedFormat("x".into()).code(),
            ErrorCode::UnsupportedFormat
        );
        assert_eq!(
            DomainError::Decode("x".into()).code(),
            ErrorCode::DecodeFailed
        );
        assert_eq!(
            DomainError::Resize("x".into()).code(),
            ErrorCode::ResizeFailed
        );
        assert_eq!(
            DomainError::upload("x", false).code(),
            ErrorCode::UploadFailed
        );
        assert_eq!(DomainError::Timeout("x".into()).code(), ErrorCode::Timeout);
        assert_eq!(
            DomainError::Internal("x".into()).code(),
            ErrorCode::Internal
        );
    }

    #[test]
    fn classification_splits_transient_and_permanent() {
        // Storage errors follow their explicit flag.
        assert!(DomainError::download("x", true).is_transient());
        assert!(!DomainError::download("x", false).is_transient());
        assert!(DomainError::upload("x", true).is_transient());
        assert!(!DomainError::upload("x", false).is_transient());

        // Timeouts are always transient.
        assert_eq!(
            DomainError::Timeout("x".into()).class(),
            ErrorClass::Transient
        );

        // Validation / decode / resize / internal are always permanent.
        assert_eq!(
            DomainError::UnsupportedFormat("x".into()).class(),
            ErrorClass::Permanent
        );
        assert_eq!(
            DomainError::Decode("x".into()).class(),
            ErrorClass::Permanent
        );
        assert_eq!(
            DomainError::Resize("x".into()).class(),
            ErrorClass::Permanent
        );
        assert_eq!(
            DomainError::Internal("x".into()).class(),
            ErrorClass::Permanent
        );
    }

    #[test]
    fn display_message_is_preserved() {
        assert_eq!(
            DomainError::download("not found", false).to_string(),
            "download failed: not found"
        );
        assert_eq!(
            DomainError::UnsupportedFormat("bad magic".into()).to_string(),
            "unsupported format: bad magic"
        );
    }
}
