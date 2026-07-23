//! Object references: where to read the original and where to write variants.

/// Location of the original image in the private source bucket.
#[derive(Debug, Clone)]
pub struct SourceRef {
    pub bucket: String,
    pub key: String,
    pub content_type: String,
}

/// Destination prefix in the public target bucket. Each variant is written to
/// `{key_prefix}/{name}.{extension}` — deterministic, so a replay overwrites the
/// same objects without creating duplicates.
#[derive(Debug, Clone)]
pub struct TargetRef {
    pub bucket: String,
    pub key_prefix: String,
}
