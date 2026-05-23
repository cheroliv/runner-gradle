#!/usr/bin/env bash
# health.sh — Runner N3 /health mock endpoint
# Retourne le JSON HealthResponse conforme au contrat OpenAPI 3.0

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRAPH_FILE="${HOME:-/home/cheroliv}/workspace/office/graph.json"

GRAPH_NODES="null"
GRAPH_EDGES="null"
DAG_HEALTHY=true
PLUGINS_COUNT=0

if [[ -f "$GRAPH_FILE" ]]; then
    GRAPH_NODES=$(jq '.nodes | length' "$GRAPH_FILE" 2>/dev/null || echo "null")
    GRAPH_EDGES=$(jq '.edges | length' "$GRAPH_FILE" 2>/dev/null || echo "null")
fi

if [[ -f "$SCRIPT_DIR/build.gradle.kts" ]]; then
    PLUGINS_COUNT=$(grep -Ec '"[a-z].*" to [0-9]+' "$SCRIPT_DIR/build.gradle.kts" 2>/dev/null || echo 0)
fi

if ! "$SCRIPT_DIR/gradlew" verifyDagAcyclic -q 2>/dev/null; then
    DAG_HEALTHY=false
fi

jq -n \
    --arg status "UP" \
    --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --argjson dag "$DAG_HEALTHY" \
    --argjson nodes "$GRAPH_NODES" \
    --argjson edges "$GRAPH_EDGES" \
    --argjson plugins "$PLUGINS_COUNT" \
    --arg version "1.0.0" \
    '{
        status: $status,
        timestamp: $ts,
        dagHealthy: $dag,
        graphNodes: $nodes,
        graphEdges: $edges,
        pluginsAvailable: $plugins,
        version: $version
    }'
