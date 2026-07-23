//! Queue-facing adapters and the wire contract.
//!
//! This is the only place that knows about RabbitMQ (`lapin`) and the only place
//! the wire `serde` contract lives. [`contract`] holds the DTOs (an exact mirror
//! of the contract owned by `kara_general_api`) and the wire↔domain mapping.
//! [`consumer`] is the driving adapter (it calls the input port); [`publisher`]
//! is a driven adapter (it implements the result-publishing output port).

pub mod consumer;
pub mod contract;
pub mod publisher;
