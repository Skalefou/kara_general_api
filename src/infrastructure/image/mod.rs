//! Image-processing adapter (decode / resize / encode).
//!
//! Wraps the `image`, `fast_image_resize` and `webp` crates behind the
//! [`ImageTransformerPort`](crate::domain::port::output::ImageTransformerPort).
//! All CPU-bound work runs on the blocking pool, never on the async reactor.

pub mod transformer;
