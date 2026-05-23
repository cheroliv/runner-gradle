#!/usr/bin/env bash
# health.sh — Runner N3 /health mock endpoint
# Retourne le JSON HealthResponse conforme au contrat OpenAPI 3.0

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

PLUGINS_COUNT=0

if [[ -f "$SCRIPT_DIR/build.gradle.kts" ]]; then
    PLUGINS_COUNT=$(grep -cE 'id\(' "$SCRIPT_DIR/build.gradle.kts" 2>/dev/null || echo 0)
fi

jq -n \
    --arg status "UP" \
    --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --argjson plugins "$PLUGINS_COUNT" \
    --arg version "2.0.0" \
    '{
        status: $status,
        timestamp: $ts,
        dagHealthy: true,
        pluginsAvailable: $plugins,
        version: $version
    }'
