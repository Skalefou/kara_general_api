//! Ports — the interfaces the hexagon exposes and requires.
//!
//! - [`input`]: primary/driving ports (use cases the worker *offers*).
//! - [`output`]: secondary/driven ports (services the worker *requires*).
//!
//! Interfaces live in the domain; their concrete adapters live in
//! [`crate::infrastructure`]. Dependencies point inward only.

pub mod input;
pub mod output;
