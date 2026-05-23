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
  info                 Show runner status and DAG registry
  provision            Generate provision.sh for workspace bootstrap
  api | contract       Validate OpenAPI 3.0 contract (openapi.yaml)
  chatbot [model]      Start REPL with Ollama (default: deepseek-v4-pro:cloud)
  health               Return workspace health JSON
  collect [query]      Run collectCompositeContext N2→N3 (codexRetrieve + codebaseRAG)
  tasks                List all Gradle tasks (runner + subprojects)

Direct Gradle:
  ./gradlew <task>     Run any Gradle task directly
EOF
}

case "${1:-}" in
    info)
        ./gradlew info -q
        ;;
    provision)
        ./gradlew deployWorkspace -q
        ;;
    api)
        ./gradlew generateApiSchema -q
        ;;
    contract)
        ./gradlew generateApiSchema -q
        ;;
    chatbot)
        ./chatbot.sh "${2:-deepseek-v4-pro:cloud}"
        ;;
    health)
        ./health.sh
        ;;
    collect)
        ./gradlew collectCompositeContext -Pquery="${2:-}" -q
        ;;
    tasks)
        ./gradlew tasks --group runner
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
