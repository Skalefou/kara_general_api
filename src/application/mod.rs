//! ② Application — orchestration.
//!
//! Depends only on the domain (ports + models). It wires the driven ports
//! together into the pipeline (download → transform → upload → outcome →
//! publish) and enforces the retry policy at the orchestration level (permanent
//! failures fail immediately; transient retry is owned by the driven adapters).
//! No infrastructure crate is imported here.

pub mod process_image_service;

pub use process_image_service::ProcessImageService;
