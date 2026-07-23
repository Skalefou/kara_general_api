//! `kara_image_worker` — RabbitMQ image-processing worker.
//!
//! Fully isolated from the Backend: it never touches the Backend DB and never
//! issues synchronous HTTP calls to it. The only communication channel is the
//! message broker (`image-jobs` in, `image-results` out).
//!
//! The crate is organized in three hexagonal layers, with dependencies pointing
//! strictly inward (infrastructure → application → domain):
//!
//! - [`domain`] — the pure hexagon: value objects, the typed error, and the
//!   ports (interfaces). No infrastructure crate, no wire `serde`.
//! - [`application`] — orchestration: the use case that wires the driven ports
//!   into the pipeline. Depends only on the domain.
//! - [`infrastructure`] — adapters and every external dependency (`lapin`,
//!   `aws_sdk_s3`, `image`, `fast_image_resize`, `webp`, `metrics`) and the wire
//!   contract.

pub mod application;
pub mod domain;
pub mod infrastructure;
