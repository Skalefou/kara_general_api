//! Pure domain value objects and aggregates.
//!
//! Immutable data with a little pure logic (fit geometry, outcome invariants).
//! No infrastructure crate and no wire `serde` appear here.

pub mod job;
pub mod outcome;
pub mod refs;
pub mod variant;

pub use job::{Job, JobId};
pub use outcome::{JobOutcome, JobReport};
pub use refs::{SourceRef, TargetRef};
pub use variant::{
    Fit, FitPlan, Format, ProcessedVariant, StoredObject, VariantResult, VariantSpec, plan_fit,
};
