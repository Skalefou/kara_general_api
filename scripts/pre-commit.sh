#!/usr/bin/env bash
# CI locale exécutée avant chaque commit : build + tests unitaires.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

echo "==> [pre-commit] Build + tests unitaires (./gradlew build)"
./gradlew build --console=plain

echo "==> [pre-commit] OK"