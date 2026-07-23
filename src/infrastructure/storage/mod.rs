//! Object-storage adapters (Google Cloud Storage through its S3-compatible API).
//!
//! The worker is a plain S3 client: path-style addressing, custom endpoint and
//! GCS HMAC keys mapped onto AWS credentials. It only ever reads from the
//! private source bucket and writes to the public target bucket.

pub mod s3;
