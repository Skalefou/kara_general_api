//! Retry helper: exponential backoff + jitter, driven by domain classification.
//!
//! The worker owns its retry policy (the Backend never re-drives it). An
//! operation returns a [`DomainError`]; if that error
//! [`is_transient`](crate::domain::error::DomainError::is_transient) and the
//! attempt budget is not exhausted, the operation is retried with exponential
//! backoff. Permanent failures stop immediately. This helper is shared by the
//! driven adapters that own an external I/O boundary.

use std::time::Duration;

use backoff::ExponentialBackoff;
use backoff::backoff::Backoff;

use crate::domain::error::DomainError;

/// Retry policy parameters derived from configuration.
#[derive(Debug, Clone, Copy)]
pub struct RetryConfig {
    pub max_attempts: u32,
    pub base_ms: u64,
}

/// Run `op` with exponential backoff + jitter, honouring the attempt budget.
///
/// The operation returns `Ok` on success or `Err(DomainError)` on failure. A
/// transient error is retried (subject to `max_attempts`); a permanent error is
/// returned immediately. The last transient error is returned once the budget
/// is exhausted.
pub async fn retry_async<T, F, Fut>(cfg: &RetryConfig, mut op: F) -> Result<T, DomainError>
where
    F: FnMut() -> Fut,
    Fut: std::future::Future<Output = Result<T, DomainError>>,
{
    let mut policy = ExponentialBackoff {
        initial_interval: Duration::from_millis(cfg.base_ms),
        max_interval: Duration::from_secs(30),
        // Attempt count, not wall-clock, bounds the loop.
        max_elapsed_time: None,
        ..ExponentialBackoff::default()
    };
    policy.reset();

    let mut attempt: u32 = 0;
    loop {
        attempt += 1;
        match op().await {
            Ok(value) => return Ok(value),
            Err(err) => {
                if !err.is_transient() || attempt >= cfg.max_attempts {
                    return Err(err);
                }
                let delay = policy
                    .next_backoff()
                    .unwrap_or_else(|| Duration::from_millis(cfg.base_ms));
                tokio::time::sleep(delay).await;
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::cell::Cell;

    fn cfg() -> RetryConfig {
        RetryConfig {
            max_attempts: 5,
            base_ms: 1,
        }
    }

    #[tokio::test]
    async fn succeeds_after_transient_failures() {
        let calls = Cell::new(0u32);
        let out: Result<u32, DomainError> = retry_async(&cfg(), || {
            let n = calls.get() + 1;
            calls.set(n);
            async move {
                if n < 3 {
                    Err(DomainError::download("temporary", true))
                } else {
                    Ok(n)
                }
            }
        })
        .await;
        assert_eq!(out.unwrap(), 3);
        assert_eq!(calls.get(), 3);
    }

    #[tokio::test]
    async fn permanent_failure_stops_immediately() {
        let calls = Cell::new(0u32);
        let out: Result<u32, DomainError> = retry_async(&cfg(), || {
            calls.set(calls.get() + 1);
            async move { Err(DomainError::download("nope", false)) }
        })
        .await;
        assert!(matches!(out, Err(DomainError::Download { .. })));
        assert_eq!(calls.get(), 1);
    }

    #[tokio::test]
    async fn gives_up_after_budget() {
        let calls = Cell::new(0u32);
        let out: Result<u32, DomainError> = retry_async(&cfg(), || {
            calls.set(calls.get() + 1);
            async move { Err(DomainError::download("temporary", true)) }
        })
        .await;
        assert!(matches!(out, Err(DomainError::Download { .. })));
        assert_eq!(calls.get(), 5);
    }
}
