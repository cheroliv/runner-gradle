#!/usr/bin/env bash
# engine.sh — CLI wrapper for N3 Engine
# Usage: ./engine <command>

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

usage() {
    cat <<'EOF'
Engine CLI — Available Commands
================================

./engine <command>

Commands:
  info                 Show engine status and DAG registry
  verify               Run DAG acyclic verification
  graph                Run aggregateGraphs (requires scanWorkspace)
  provision            Generate provision.sh for workspace bootstrap
  api | contract       Validate OpenAPI 3.0 contract (openapi.yaml)
  chatbot [model]      Start REPL with Ollama (default: deepseek-v4-pro:cloud)
  health               Return workspace health JSON
  augment              Run augmentOpencode pipeline -> /tmp/opencode-context.txt
  tasks                List all Gradle tasks (engine + subprojects)

Direct Gradle:
  ./gradlew <task>     Run any Gradle task directly
EOF
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
    api)
        ./gradlew apiSchema -q
        ;;
    contract)
        ./gradlew apiSchema -q
        ;;
    chatbot)
        ./chatbot.sh "${2:-deepseek-v4-pro:cloud}"
        ;;
    health)
        ./health.sh
        ;;
    augment)
        ./gradlew augmentOpencode -q
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
