//! Variant value objects and the pure fit geometry.
//!
//! The geometry of a resize (output dimensions and the optional source crop
//! box) is pure math and lives here, in the domain. The actual pixel work
//! (decode / resample / encode) belongs to the image adapter — this module
//! never touches the `image`, `fast_image_resize` or `webp` crates.

/// How the source image is fitted into the requested box.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Fit {
    /// Crop-fill: output is exactly `width`x`height`.
    Cover,
    /// Scale to fit within the box, preserving aspect (may upscale).
    Contain,
    /// Scale to fit within the box, preserving aspect, never upscaling.
    Inside,
}

/// Output encoding of a variant. Only WebP is currently supported.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Format {
    Webp,
}

impl Format {
    /// File extension used to build the deterministic output key.
    pub fn extension(&self) -> &'static str {
        match self {
            Format::Webp => "webp",
        }
    }

    /// MIME type reported back in the result.
    pub fn content_type(&self) -> &'static str {
        match self {
            Format::Webp => "image/webp",
        }
    }
}

/// A requested output variant.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VariantSpec {
    pub name: String,
    pub width: u32,
    pub height: u32,
    pub fit: Fit,
    pub format: Format,
}

/// The concrete geometry for one variant: output size and an optional source
/// crop box `(left, top, width, height)` in pixels.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct FitPlan {
    pub out_w: u32,
    pub out_h: u32,
    pub crop: Option<(f64, f64, f64, f64)>,
}

/// Compute output dimensions (and an optional source crop) for a `fit` mode.
///
/// Pure function — no image data, only geometry.
pub fn plan_fit(src_w: u32, src_h: u32, box_w: u32, box_h: u32, fit: Fit) -> FitPlan {
    let (sw, sh) = (src_w as f64, src_h as f64);
    let (bw, bh) = (box_w.max(1) as f64, box_h.max(1) as f64);

    match fit {
        Fit::Cover => {
            // Output is exactly the box; crop the source to the box aspect.
            let box_aspect = bw / bh;
            let src_aspect = sw / sh;
            let (cw, ch) = if src_aspect > box_aspect {
                (sh * box_aspect, sh)
            } else {
                (sw, sw / box_aspect)
            };
            let left = (sw - cw) / 2.0;
            let top = (sh - ch) / 2.0;
            FitPlan {
                out_w: box_w.max(1),
                out_h: box_h.max(1),
                crop: Some((left, top, cw, ch)),
            }
        }
        Fit::Contain | Fit::Inside => {
            let mut scale = (bw / sw).min(bh / sh);
            if matches!(fit, Fit::Inside) {
                scale = scale.min(1.0);
            }
            let out_w = (sw * scale).round().max(1.0) as u32;
            let out_h = (sh * scale).round().max(1.0) as u32;
            FitPlan {
                out_w,
                out_h,
                crop: None,
            }
        }
    }
}

/// A resized + encoded variant, ready to be stored. Owns its encoded bytes.
#[derive(Debug, Clone)]
pub struct ProcessedVariant {
    pub name: String,
    pub bytes: Vec<u8>,
    pub width: u32,
    pub height: u32,
    pub format: Format,
}

impl ProcessedVariant {
    /// MIME type of the encoded bytes.
    pub fn content_type(&self) -> &'static str {
        self.format.content_type()
    }

    /// Size of the encoded bytes, as reported in the result.
    pub fn size_bytes(&self) -> u64 {
        self.bytes.len() as u64
    }
}

/// Description of one successfully written variant (mirrors the wire result).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VariantResult {
    pub name: String,
    pub bucket: String,
    pub key: String,
    pub width: u32,
    pub height: u32,
    pub size_bytes: u64,
    pub content_type: String,
}

/// What the storage port reports after persisting a variant.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct StoredObject {
    pub key: String,
    pub size_bytes: u64,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cover_outputs_exact_box_and_crops_source() {
        // 1000x500 source into a 320x320 box: crop to a centered square.
        let plan = plan_fit(1000, 500, 320, 320, Fit::Cover);
        assert_eq!((plan.out_w, plan.out_h), (320, 320));
        let (left, top, cw, ch) = plan.crop.expect("cover crops the source");
        assert_eq!((cw, ch), (500.0, 500.0));
        assert_eq!((left, top), (250.0, 0.0));
    }

    #[test]
    fn contain_preserves_aspect_within_box() {
        // 1000x500 (2:1) into 1024x768: width-bound, height scaled to keep 2:1.
        let plan = plan_fit(1000, 500, 1024, 768, Fit::Contain);
        assert_eq!((plan.out_w, plan.out_h), (1024, 512));
        assert!(plan.crop.is_none());
    }

    #[test]
    fn contain_may_upscale() {
        let plan = plan_fit(200, 100, 1000, 1000, Fit::Contain);
        assert_eq!((plan.out_w, plan.out_h), (1000, 500));
    }

    #[test]
    fn inside_never_upscales() {
        let plan = plan_fit(200, 100, 2048, 2048, Fit::Inside);
        assert_eq!((plan.out_w, plan.out_h), (200, 100));
    }

    #[test]
    fn inside_downscales_when_larger() {
        let plan = plan_fit(4000, 2000, 2048, 2048, Fit::Inside);
        assert_eq!((plan.out_w, plan.out_h), (2048, 1024));
    }

    #[test]
    fn format_exposes_extension_and_content_type() {
        assert_eq!(Format::Webp.extension(), "webp");
        assert_eq!(Format::Webp.content_type(), "image/webp");
    }
}
