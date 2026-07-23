//! Driven adapter: publish results on the `image-results` queue (Worker ->
//! Backend), implementing [`ResultPublisherPort`].
//!
//! Maps a domain [`JobOutcome`](crate::domain::model::JobOutcome) to the wire
//! [`ResultMessage`] (ok or failed) via the contract mapping, carrying the
//! `jobId` through unchanged, and publishes it with publisher confirms.

use std::sync::Arc;

use async_trait::async_trait;
use lapin::options::BasicPublishOptions;
use lapin::{BasicProperties, Channel};

use crate::domain::error::DomainError;
use crate::domain::model::{JobId, JobOutcome};
use crate::domain::port::output::ResultPublisherPort;
use crate::infrastructure::messaging::contract::to_wire;

/// Publishes results on the results queue. Cheap to clone.
#[derive(Clone)]
pub struct RabbitResultPublisher {
    channel: Arc<Channel>,
    queue: String,
}

impl RabbitResultPublisher {
    /// Wrap a channel dedicated to publishing results.
    pub fn new(channel: Channel, queue: String) -> Self {
        Self {
            channel: Arc::new(channel),
            queue,
        }
    }
}

#[async_trait]
impl ResultPublisherPort for RabbitResultPublisher {
    async fn publish(&self, job_id: &JobId, outcome: &JobOutcome) -> Result<(), DomainError> {
        let message = to_wire(job_id, outcome);
        let payload = serde_json::to_vec(&message)
            .map_err(|e| DomainError::Internal(format!("result serialization: {e}")))?;

        let properties = BasicProperties::default()
            .with_content_type("application/json".into())
            .with_delivery_mode(2); // persistent

        self.channel
            .basic_publish(
                "".into(),
                self.queue.as_str().into(),
                BasicPublishOptions::default(),
                &payload,
                properties,
            )
            .await
            .map_err(|e| DomainError::Internal(format!("publish to {}: {e}", self.queue)))?
            .await
            .map_err(|e| {
                DomainError::Internal(format!("publish confirm on {}: {e}", self.queue))
            })?;

        Ok(())
    }
}
