//! Wire contract — an **exact mirror** of the message contract owned by
//! `kara_general_api`, plus the wire↔domain mapping.
//!
//! These `serde` DTOs are the single source of truth on the worker side; if the
//! API evolves the contract, this file is updated in mirror, never the other way
//! around. The domain never sees these types: [`from_wire`] converts an inbound
//! job DTO into a domain [`Job`], and [`to_wire`] converts a domain
//! [`JobOutcome`] into an outbound result DTO.
//!
//! All fields are `camelCase` on the wire, both queues carry `schemaVersion`,
//! and messages never contain image bytes — only object keys.

use std::time::{SystemTime, UNIX_EPOCH};

use serde::{Deserialize, Serialize};

use crate::domain::error::ErrorCode;
use crate::domain::model::{
    Fit, Format, Job, JobId, JobOutcome, SourceRef, TargetRef, VariantResult, VariantSpec,
};

/// Contract schema version. Bumped only in lockstep with the API.
pub const SCHEMA_VERSION: u32 = 1;

// ---------------------------------------------------------------------------
// Inbound: queue `image-jobs` (Backend -> Worker)
// ---------------------------------------------------------------------------

/// A single image-processing job read from `image-jobs`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JobMessage {
    pub schema_version: u32,
    /// Correlation id, echoed verbatim in the result.
    pub job_id: String,
    pub room_id: String,
    pub image_id: String,
    pub source: SourceDto,
    pub target: TargetDto,
    pub variants: Vec<VariantSpecDto>,
    pub reply_to: String,
    pub enqueued_at: String,
}

/// Location of the original image in the private source bucket.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SourceDto {
    pub bucket: String,
    pub key: String,
    pub content_type: String,
}

/// Destination prefix in the public target bucket.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TargetDto {
    pub bucket: String,
    /// Each variant is written to `{key_prefix}/{name}.{format}`.
    pub key_prefix: String,
}

/// A requested output variant.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VariantSpecDto {
    pub name: String,
    pub width: u32,
    pub height: u32,
    pub fit: FitDto,
    pub format: VariantFormatDto,
}

/// How the source image is fitted into the requested box.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum FitDto {
    Cover,
    Contain,
    Inside,
}

/// Output encoding of a variant. Only WebP is currently supported.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum VariantFormatDto {
    Webp,
}

// ---------------------------------------------------------------------------
// Outbound: queue `image-results` (Worker -> Backend)
// ---------------------------------------------------------------------------

/// Result published on `image-results` — always emitted, ok or failed.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ResultMessage {
    pub schema_version: u32,
    pub job_id: String,
    pub status: Status,
    /// Present **iff** `status == ok`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub variants: Option<Vec<VariantResultDto>>,
    /// Present **iff** `status == failed`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorInfo>,
    pub processed_at: String,
}

/// Terminal status of a job.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Status {
    Ok,
    Failed,
}

/// Description of one successfully written variant.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VariantResultDto {
    pub name: String,
    pub bucket: String,
    pub key: String,
    pub width: u32,
    pub height: u32,
    pub size_bytes: u64,
    pub content_type: String,
}

/// Structured failure payload.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorInfo {
    pub code: ErrorCodeDto,
    pub message: String,
}

/// Frozen enumeration of contract error codes (`SCREAMING_SNAKE_CASE`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ErrorCodeDto {
    DownloadFailed,
    UnsupportedFormat,
    DecodeFailed,
    ResizeFailed,
    UploadFailed,
    Timeout,
    Internal,
}

// ---------------------------------------------------------------------------
// Wire <-> domain mapping
// ---------------------------------------------------------------------------

impl From<FitDto> for Fit {
    fn from(dto: FitDto) -> Self {
        match dto {
            FitDto::Cover => Fit::Cover,
            FitDto::Contain => Fit::Contain,
            FitDto::Inside => Fit::Inside,
        }
    }
}

impl From<VariantFormatDto> for Format {
    fn from(dto: VariantFormatDto) -> Self {
        match dto {
            VariantFormatDto::Webp => Format::Webp,
        }
    }
}

impl From<ErrorCode> for ErrorCodeDto {
    fn from(code: ErrorCode) -> Self {
        match code {
            ErrorCode::DownloadFailed => ErrorCodeDto::DownloadFailed,
            ErrorCode::UnsupportedFormat => ErrorCodeDto::UnsupportedFormat,
            ErrorCode::DecodeFailed => ErrorCodeDto::DecodeFailed,
            ErrorCode::ResizeFailed => ErrorCodeDto::ResizeFailed,
            ErrorCode::UploadFailed => ErrorCodeDto::UploadFailed,
            ErrorCode::Timeout => ErrorCodeDto::Timeout,
            ErrorCode::Internal => ErrorCodeDto::Internal,
        }
    }
}

/// Convert an inbound job DTO into a domain [`Job`].
pub fn from_wire(msg: JobMessage) -> Job {
    Job {
        job_id: JobId::new(msg.job_id),
        source: SourceRef {
            bucket: msg.source.bucket,
            key: msg.source.key,
            content_type: msg.source.content_type,
        },
        target: TargetRef {
            bucket: msg.target.bucket,
            key_prefix: msg.target.key_prefix,
        },
        variants: msg
            .variants
            .into_iter()
            .map(|v| VariantSpec {
                name: v.name,
                width: v.width,
                height: v.height,
                fit: v.fit.into(),
                format: v.format.into(),
            })
            .collect(),
    }
}

/// Convert a domain outcome into the outbound result DTO. Always sets the
/// invariant fields: `variants` iff ok, `error` iff failed.
pub fn to_wire(job_id: &JobId, outcome: &JobOutcome) -> ResultMessage {
    match outcome {
        JobOutcome::Ok(variants) => ResultMessage {
            schema_version: SCHEMA_VERSION,
            job_id: job_id.as_str().to_string(),
            status: Status::Ok,
            variants: Some(variants.iter().map(variant_result_to_wire).collect()),
            error: None,
            processed_at: now_rfc3339(),
        },
        JobOutcome::Failed { code, message } => ResultMessage {
            schema_version: SCHEMA_VERSION,
            job_id: job_id.as_str().to_string(),
            status: Status::Failed,
            variants: None,
            error: Some(ErrorInfo {
                code: (*code).into(),
                message: message.clone(),
            }),
            processed_at: now_rfc3339(),
        },
    }
}

fn variant_result_to_wire(variant: &VariantResult) -> VariantResultDto {
    VariantResultDto {
        name: variant.name.clone(),
        bucket: variant.bucket.clone(),
        key: variant.key.clone(),
        width: variant.width,
        height: variant.height,
        size_bytes: variant.size_bytes,
        content_type: variant.content_type.clone(),
    }
}

// ---------------------------------------------------------------------------
// Timestamp helper (RFC 3339 / UTC) without pulling a date crate.
// ---------------------------------------------------------------------------

/// Current UTC time formatted as an RFC 3339 timestamp (`...Z`).
///
/// Uses a dependency-free civil-calendar conversion. Never panics.
pub fn now_rfc3339() -> String {
    let secs = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0) as i64;

    let days = secs.div_euclid(86_400);
    let rem = secs.rem_euclid(86_400);
    let (hour, min, sec) = (rem / 3600, (rem % 3600) / 60, rem % 60);
    let (year, month, day) = civil_from_days(days);

    format!("{year:04}-{month:02}-{day:02}T{hour:02}:{min:02}:{sec:02}Z")
}

/// Convert a count of days since the Unix epoch to `(year, month, day)`.
/// Howard Hinnant's `civil_from_days` algorithm.
fn civil_from_days(z: i64) -> (i64, u32, u32) {
    let z = z + 719_468;
    let era = if z >= 0 { z } else { z - 146_096 } / 146_097;
    let doe = z - era * 146_097; // [0, 146096]
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365; // [0, 399]
    let y = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100); // [0, 365]
    let mp = (5 * doy + 2) / 153; // [0, 11]
    let d = (doy - (153 * mp + 2) / 5 + 1) as u32; // [1, 31]
    let m = if mp < 10 { mp + 3 } else { mp - 9 } as u32; // [1, 12]
    (if m <= 2 { y + 1 } else { y }, m, d)
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE_JOB: &str = r#"{
        "schemaVersion": 1,
        "jobId": "550e8400-e29b-41d4-a716-446655440000",
        "roomId": "b3f1c2a4-1111-2222-3333-444455556666",
        "imageId": "c4a2d0e8-aaaa-bbbb-cccc-ddddeeeeffff",
        "source": { "bucket": "priv", "key": "rooms/r/originals/i.jpg", "contentType": "image/jpeg" },
        "target": { "bucket": "pub", "keyPrefix": "rooms/r/i" },
        "variants": [
            { "name": "thumbnail", "width": 320, "height": 320, "fit": "cover", "format": "webp" },
            { "name": "detail", "width": 1024, "height": 768, "fit": "contain", "format": "webp" },
            { "name": "full", "width": 2048, "height": 2048, "fit": "inside", "format": "webp" }
        ],
        "replyTo": "image-results",
        "enqueuedAt": "2026-07-23T12:00:00Z"
    }"#;

    #[test]
    fn job_deserializes_from_contract_json() {
        let job: JobMessage = serde_json::from_str(SAMPLE_JOB).expect("parse job");
        assert_eq!(job.schema_version, 1);
        assert_eq!(job.job_id, "550e8400-e29b-41d4-a716-446655440000");
        assert_eq!(job.source.content_type, "image/jpeg");
        assert_eq!(job.target.key_prefix, "rooms/r/i");
        assert_eq!(job.variants.len(), 3);
        assert_eq!(job.variants[0].fit, FitDto::Cover);
        assert_eq!(job.variants[1].fit, FitDto::Contain);
        assert_eq!(job.variants[2].fit, FitDto::Inside);
        assert_eq!(job.variants[0].format, VariantFormatDto::Webp);
    }

    #[test]
    fn job_roundtrips() {
        let job: JobMessage = serde_json::from_str(SAMPLE_JOB).unwrap();
        let text = serde_json::to_string(&job).unwrap();
        let back: JobMessage = serde_json::from_str(&text).unwrap();
        assert_eq!(back.variants.len(), job.variants.len());
        assert_eq!(back.job_id, job.job_id);
    }

    #[test]
    fn from_wire_maps_to_domain_job() {
        let msg: JobMessage = serde_json::from_str(SAMPLE_JOB).unwrap();
        let job = from_wire(msg);
        assert_eq!(job.job_id.as_str(), "550e8400-e29b-41d4-a716-446655440000");
        assert_eq!(job.source.bucket, "priv");
        assert_eq!(job.target.key_prefix, "rooms/r/i");
        assert_eq!(job.variants.len(), 3);
        assert_eq!(job.variants[0].fit, Fit::Cover);
        assert_eq!(job.variants[1].fit, Fit::Contain);
        assert_eq!(job.variants[2].fit, Fit::Inside);
        assert_eq!(job.variants[0].format, Format::Webp);
    }

    #[test]
    fn to_wire_ok_has_variants_and_no_error() {
        let outcome = JobOutcome::Ok(vec![VariantResult {
            name: "thumbnail".into(),
            bucket: "pub".into(),
            key: "rooms/r/i/thumbnail.webp".into(),
            width: 320,
            height: 320,
            size_bytes: 1234,
            content_type: "image/webp".into(),
        }]);
        let msg = to_wire(&JobId::new("job-1"), &outcome);
        let value: serde_json::Value = serde_json::to_value(&msg).unwrap();
        assert_eq!(value["status"], "ok");
        assert_eq!(value["jobId"], "job-1");
        assert!(value.get("variants").is_some());
        assert!(value.get("error").is_none());
        assert_eq!(value["schemaVersion"], 1);
    }

    #[test]
    fn to_wire_failed_has_error_and_no_variants() {
        let outcome = JobOutcome::Failed {
            code: ErrorCode::DownloadFailed,
            message: "not found".into(),
        };
        let msg = to_wire(&JobId::new("job-1"), &outcome);
        let value: serde_json::Value = serde_json::to_value(&msg).unwrap();
        assert_eq!(value["status"], "failed");
        assert!(value.get("variants").is_none());
        assert_eq!(value["error"]["code"], "DOWNLOAD_FAILED");
        assert_eq!(value["error"]["message"], "not found");
    }

    #[test]
    fn error_codes_render_screaming_snake_case() {
        let cases = [
            (ErrorCode::DownloadFailed, "\"DOWNLOAD_FAILED\""),
            (ErrorCode::UnsupportedFormat, "\"UNSUPPORTED_FORMAT\""),
            (ErrorCode::DecodeFailed, "\"DECODE_FAILED\""),
            (ErrorCode::ResizeFailed, "\"RESIZE_FAILED\""),
            (ErrorCode::UploadFailed, "\"UPLOAD_FAILED\""),
            (ErrorCode::Timeout, "\"TIMEOUT\""),
            (ErrorCode::Internal, "\"INTERNAL\""),
        ];
        for (code, expected) in cases {
            let dto: ErrorCodeDto = code.into();
            assert_eq!(serde_json::to_string(&dto).unwrap(), expected);
        }
    }

    #[test]
    fn timestamp_is_well_formed() {
        // 2026-07-23T12:00:00Z corresponds to a known epoch second.
        let (y, m, d) = civil_from_days(1_774_224_000_i64.div_euclid(86_400));
        assert_eq!((y, m, d), (2026, 3, 23));
        let ts = now_rfc3339();
        assert_eq!(ts.len(), 20);
        assert!(ts.ends_with('Z'));
    }
}
