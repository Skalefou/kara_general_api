# syntax=docker/dockerfile:1

# ---- Build stage -----------------------------------------------------------
FROM rust:1-bookworm AS builder

# libwebp is built from source by the `webp`/`libwebp-sys` crate via `cc`,
# so a C toolchain is enough; no extra system libs required.
WORKDIR /app

# Cache dependencies: copy manifests first, then the sources.
COPY Cargo.toml Cargo.lock ./
COPY src ./src

RUN cargo build --release --locked --bin kara-image-worker \
    && strip target/release/kara-image-worker

# ---- Runtime stage ---------------------------------------------------------
FROM debian:bookworm-slim AS runtime

# CA certificates for TLS to RabbitMQ / Google Cloud Storage.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Run as a non-root user.
RUN useradd --system --uid 10001 --user-group worker
USER worker

WORKDIR /app
COPY --from=builder /app/target/release/kara-image-worker /usr/local/bin/kara-image-worker

# Metrics port (see METRICS_PORT).
EXPOSE 9100

ENTRYPOINT ["/usr/local/bin/kara-image-worker"]
