//! ① Hexagon — the pure domain.
//!
//! This module is deliberately free of every infrastructure crate: no `lapin`,
//! `aws_sdk_s3`, `image`, `fast_image_resize`, `webp` or `metrics`, and no wire
//! `serde`. If it compiles without those crates on the classpath, the dependency
//! rule holds. The domain declares the ports (interfaces); the adapters that
//! implement them live in [`crate::infrastructure`].

pub mod error;
pub mod model;
pub mod port;
