//! ③ Infrastructure — adapters and every external dependency.
//!
//! This is the only layer that knows `lapin`, `aws_sdk_s3`, `image`,
//! `fast_image_resize`, `webp` and `metrics`, and the only place the wire
//! `serde` contract lives. Driving adapters (the consumer) call the input port;
//! driven adapters (storage, image transformer, publisher) implement the output
//! ports. Dependencies point inward: infrastructure → application → domain.

pub mod config;
pub mod image;
pub mod messaging;
pub mod metrics;
pub mod retry;
pub mod storage;
