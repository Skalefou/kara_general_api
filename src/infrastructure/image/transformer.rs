//! Driven adapter: validate + resize + WebP-encode, implementing
//! [`ImageTransformerPort`].
//!
//! Validation (magic bytes, allowed format, size/dimension caps) is permanent on
//! failure. The fit **geometry** is computed by the pure domain
//! ([`plan_fit`]); this adapter only performs the pixel work: decode once,
//! resample each variant from the shared decoded image, and encode to WebP. All
//! of it runs on the blocking pool — CPU-bound work never touches the reactor.

use async_trait::async_trait;
use fast_image_resize::{self as fir, PixelType, ResizeOptions, Resizer, images::Image};
use image::DynamicImage;

use crate::domain::error::DomainError;
use crate::domain::model::{Format, ProcessedVariant, VariantSpec, plan_fit};
use crate::domain::port::output::ImageTransformerPort;

/// Default WebP quality (0-100). A good size/quality trade-off for web assets.
const WEBP_QUALITY: f32 = 80.0;

/// Image container detected from the leading bytes.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum DetectedFormat {
    Jpeg,
    Png,
    Webp,
}

/// Validates originals against the configured caps and turns them into variants.
#[derive(Clone)]
pub struct WebpTransformer {
    max_image_bytes: u64,
    max_dimension: u32,
}

impl WebpTransformer {
    /// Build the transformer with the validation caps from configuration.
    pub fn new(max_image_bytes: u64, max_dimension: u32) -> Self {
        Self {
            max_image_bytes,
            max_dimension,
        }
    }
}

#[async_trait]
impl ImageTransformerPort for WebpTransformer {
    async fn transform(
        &self,
        original: &[u8],
        variants: &[VariantSpec],
    ) -> Result<Vec<ProcessedVariant>, DomainError> {
        let bytes = original.to_vec();
        let variants = variants.to_vec();
        let max_bytes = self.max_image_bytes;
        let max_dim = self.max_dimension;

        // CPU-bound work must never run on the async reactor.
        tokio::task::spawn_blocking(move || {
            let image = validate(&bytes, max_bytes, max_dim)?;
            let mut out = Vec::with_capacity(variants.len());
            for spec in &variants {
                out.push(resize_encode(&image, spec)?);
            }
            Ok::<_, DomainError>(out)
        })
        .await
        .map_err(|e| DomainError::Internal(format!("blocking task join: {e}")))?
    }
}

/// Detect the container from magic bytes, restricted to the allowed set.
fn detect_format(bytes: &[u8]) -> Result<DetectedFormat, DomainError> {
    if bytes.len() >= 3 && bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[2] == 0xFF {
        return Ok(DetectedFormat::Jpeg);
    }
    if bytes.len() >= 8 && bytes[0..8] == [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] {
        return Ok(DetectedFormat::Png);
    }
    if bytes.len() >= 12 && &bytes[0..4] == b"RIFF" && &bytes[8..12] == b"WEBP" {
        return Ok(DetectedFormat::Webp);
    }
    Err(DomainError::UnsupportedFormat(
        "unrecognized magic bytes (expected jpeg, png or webp)".to_string(),
    ))
}

/// Validate downloaded bytes and decode them into an image.
///
/// Checks, in order: allowed format (magic bytes), byte-size cap, decodability,
/// and dimension cap. Every failure here is permanent.
fn validate(bytes: &[u8], max_bytes: u64, max_dimension: u32) -> Result<DynamicImage, DomainError> {
    let format = detect_format(bytes)?;

    if bytes.len() as u64 > max_bytes {
        return Err(DomainError::UnsupportedFormat(format!(
            "image is {} bytes, exceeds the {max_bytes} byte cap",
            bytes.len()
        )));
    }

    let image = image::load_from_memory(bytes)
        .map_err(|e| DomainError::Decode(format!("{format:?} decode failed: {e}")))?;

    let (width, height) = (image.width(), image.height());
    if width == 0 || height == 0 {
        return Err(DomainError::Decode(
            "image has a zero dimension".to_string(),
        ));
    }
    if width > max_dimension || height > max_dimension {
        return Err(DomainError::UnsupportedFormat(format!(
            "image is {width}x{height}, exceeds the {max_dimension}px dimension cap"
        )));
    }

    Ok(image)
}

/// Resize `image` according to `spec` and encode the result into a
/// [`ProcessedVariant`].
fn resize_encode(
    image: &DynamicImage,
    spec: &VariantSpec,
) -> Result<ProcessedVariant, DomainError> {
    let src_w = image.width();
    let src_h = image.height();
    if src_w == 0 || src_h == 0 {
        return Err(DomainError::Resize(
            "source image has a zero dimension".to_string(),
        ));
    }

    // Geometry comes from the pure domain.
    let plan = plan_fit(src_w, src_h, spec.width, spec.height, spec.fit);

    let rgba = image.to_rgba8();
    let src = Image::from_vec_u8(src_w, src_h, rgba.into_raw(), PixelType::U8x4)
        .map_err(|e| DomainError::Resize(format!("source view: {e}")))?;

    let mut dst = Image::new(plan.out_w, plan.out_h, PixelType::U8x4);

    let mut options =
        ResizeOptions::new().resize_alg(fir::ResizeAlg::Convolution(fir::FilterType::Lanczos3));
    if let Some((left, top, width, height)) = plan.crop {
        options = options.crop(left, top, width, height);
    }

    let mut resizer = Resizer::new();
    resizer
        .resize(&src, &mut dst, &options)
        .map_err(|e| DomainError::Resize(format!("resize {}: {e}", spec.name)))?;

    let bytes = match spec.format {
        Format::Webp => webp::Encoder::from_rgba(dst.buffer(), plan.out_w, plan.out_h)
            .encode(WEBP_QUALITY)
            .to_vec(),
    };

    Ok(ProcessedVariant {
        name: spec.name.clone(),
        bytes,
        width: plan.out_w,
        height: plan.out_h,
        format: spec.format,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::domain::error::ErrorCode;
    use crate::domain::model::Fit;
    use image::{ImageFormat, RgbaImage};
    use std::io::Cursor;

    fn encode(img: &RgbaImage, fmt: ImageFormat) -> Vec<u8> {
        let mut buf = Cursor::new(Vec::new());
        DynamicImage::ImageRgba8(img.clone())
            .write_to(&mut buf, fmt)
            .unwrap();
        buf.into_inner()
    }

    fn spec(name: &str, w: u32, h: u32, fit: Fit) -> VariantSpec {
        VariantSpec {
            name: name.to_string(),
            width: w,
            height: h,
            fit,
            format: Format::Webp,
        }
    }

    fn sample(w: u32, h: u32) -> DynamicImage {
        DynamicImage::ImageRgba8(RgbaImage::from_pixel(w, h, image::Rgba([120, 80, 40, 255])))
    }

    // ---- validation ----

    #[test]
    fn detects_png_jpeg_webp() {
        let img = RgbaImage::from_pixel(4, 4, image::Rgba([10, 20, 30, 255]));
        assert_eq!(
            detect_format(&encode(&img, ImageFormat::Png)).unwrap(),
            DetectedFormat::Png
        );
        assert_eq!(
            detect_format(&encode(&img, ImageFormat::Jpeg)).unwrap(),
            DetectedFormat::Jpeg
        );
        assert_eq!(
            detect_format(&encode(&img, ImageFormat::WebP)).unwrap(),
            DetectedFormat::Webp
        );
    }

    #[test]
    fn rejects_unknown_format() {
        let err = detect_format(b"not-an-image-at-all").unwrap_err();
        assert_eq!(err.code(), ErrorCode::UnsupportedFormat);
    }

    #[test]
    fn rejects_oversized_bytes() {
        let img = RgbaImage::from_pixel(8, 8, image::Rgba([1, 2, 3, 255]));
        let bytes = encode(&img, ImageFormat::Png);
        let err = validate(&bytes, 4, 8000).unwrap_err();
        assert_eq!(err.code(), ErrorCode::UnsupportedFormat);
    }

    #[test]
    fn rejects_oversized_dimensions() {
        let img = RgbaImage::from_pixel(64, 8, image::Rgba([1, 2, 3, 255]));
        let bytes = encode(&img, ImageFormat::Png);
        let err = validate(&bytes, 10_000_000, 32).unwrap_err();
        assert_eq!(err.code(), ErrorCode::UnsupportedFormat);
    }

    #[test]
    fn accepts_valid_image() {
        let img = RgbaImage::from_pixel(20, 10, image::Rgba([1, 2, 3, 255]));
        let bytes = encode(&img, ImageFormat::Png);
        let decoded = validate(&bytes, 10_000_000, 8000).unwrap();
        assert_eq!((decoded.width(), decoded.height()), (20, 10));
    }

    // ---- resize + encode ----

    #[test]
    fn cover_outputs_exact_box() {
        let img = sample(1000, 500);
        let out = resize_encode(&img, &spec("thumbnail", 320, 320, Fit::Cover)).unwrap();
        assert_eq!((out.width, out.height), (320, 320));
        assert!(!out.bytes.is_empty());
        assert_eq!(out.content_type(), "image/webp");
        assert_eq!(out.name, "thumbnail");
    }

    #[test]
    fn contain_preserves_aspect_within_box() {
        let img = sample(1000, 500); // 2:1
        let out = resize_encode(&img, &spec("detail", 1024, 768, Fit::Contain)).unwrap();
        assert_eq!((out.width, out.height), (1024, 512));
    }

    #[test]
    fn inside_never_upscales() {
        let img = sample(200, 100);
        let out = resize_encode(&img, &spec("full", 2048, 2048, Fit::Inside)).unwrap();
        assert_eq!((out.width, out.height), (200, 100));
    }

    #[test]
    fn inside_downscales_when_larger() {
        let img = sample(4000, 2000);
        let out = resize_encode(&img, &spec("full", 2048, 2048, Fit::Inside)).unwrap();
        assert_eq!((out.width, out.height), (2048, 1024));
    }

    #[tokio::test]
    async fn transform_produces_one_variant_per_spec() {
        let img = sample(800, 600);
        let bytes = encode(img.as_rgba8().unwrap(), ImageFormat::Png);
        let transformer = WebpTransformer::new(10_000_000, 8000);
        let specs = vec![
            spec("thumbnail", 320, 320, Fit::Cover),
            spec("detail", 1024, 768, Fit::Contain),
        ];
        let out = transformer.transform(&bytes, &specs).await.unwrap();
        assert_eq!(out.len(), 2);
        assert_eq!((out[0].width, out[0].height), (320, 320));
    }

    #[tokio::test]
    async fn transform_rejects_non_image_as_permanent() {
        let transformer = WebpTransformer::new(10_000_000, 8000);
        let err = transformer
            .transform(b"not-an-image", &[spec("thumbnail", 320, 320, Fit::Cover)])
            .await
            .unwrap_err();
        assert_eq!(err.code(), ErrorCode::UnsupportedFormat);
        assert!(!err.is_transient());
    }
}
