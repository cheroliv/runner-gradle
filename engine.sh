#!/usr/bin/env bash
# engine.sh — CLI wrapper for N3 Engine
# Usage: ./engine <command>

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

usage() {
    ./gradlew usage -q
}

case "${1:-}" in
    info)
        ./gradlew info -q
        ;;
    verify)
        ./gradlew verifyDagAcyclic -q
        ;;
    graph)
        ./gradlew aggregateGraphs -q
        ;;
    provision)
        ./gradlew provisionWorkspace -q
        ;;
    tasks)
        ./gradlew tasks --group engine
        ;;
    usage|--help|-h|"")
        usage
        ;;
    *)
        echo "Unknown command: $1"
        usage
        exit 1
        ;;
esac
