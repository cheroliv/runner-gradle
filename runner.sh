#!/usr/bin/env bash
# runner.sh — CLI wrapper for N3 Runner
# Usage: ./runner <command>

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

usage() {
    cat <<'EOF'
Runner CLI — Available Commands
================================

./runner <command>

Commands:
  info                 Show runner status and workspace paths
  verify               Verify boroughs + OpenAPI contract
  verify-boroughs      Validate metadata contracts for 11 boroughs
  validate-api         Validate OpenAPI 3.0 contract (openapi.yaml)
  chatbot [model]      Start REPL with Ollama (default: deepseek-v4-pro:cloud)
  health               Return workspace health JSON
  tasks                List all Gradle tasks

Direct Gradle:
  ./gradlew <task>     Run any Gradle task directly
EOF
}

case "${1:-}" in
    info)
        ./gradlew info -q
        ;;
    verify)
        ./gradlew verifyBoroughs validateApiSchema -q
        ;;
    verify-boroughs)
        ./gradlew verifyBoroughs -q
        ;;
    validate-api)
        ./gradlew validateApiSchema -q
        ;;
    contract)
        ./gradlew validateApiSchema -q
        ;;
    api)
        ./gradlew validateApiSchema -q
        ;;
    chatbot)
        ./chatbot.sh "${2:-deepseek-v4-pro:cloud}"
        ;;
    health)
        ./health.sh
        ;;
    tasks)
        ./gradlew tasks --group deploy --group verify --group info --group bakery
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
