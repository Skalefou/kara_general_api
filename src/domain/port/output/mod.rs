//! Secondary (driven) ports — the services the worker requires from the outside.

pub mod image_transformer;
pub mod result_publisher;
pub mod source_image;
pub mod variant_storage;

pub use image_transformer::ImageTransformerPort;
pub use result_publisher::ResultPublisherPort;
pub use source_image::SourceImagePort;
pub use variant_storage::VariantStoragePort;
