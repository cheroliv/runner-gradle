#!/usr/bin/env bash
# chatbot.sh — Runner N3 orchestrator, REPL conversationnelle
# Usage: ./chatbot.sh [model]

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODEL="${1:-deepseek-v4-pro:cloud}"
GRAPH_FILE="${HOME:-/home/cheroliv}/workspace/office/graph.json"

SYSTEM_PROMPT="Tu es l'orchestrateur N3 du workspace Runner. Ton rôle est de superviser l'écosystème des plugins Gradle (N0 à N2), de garantir le respect du DAG (Règle 4bis : aucun plugin n'importe un plugin de niveau supérieur), et d'orchestrer les pipelines transverses (aggregateGraphs, verifyDagAcyclic, deployWorkspace, augmentOpencode).

Règles absolues :
- Runner N3 importe N0/N1/N2, jamais l'inverse
- `graph.json` global est la source de vérité de l'architecture
- Zéro secret dans le code, les credentials viennent de `configuration/` (cercle 1)
- Pas de publishToMavenLocal pour runner-gradle
- Pas de commit sans permission explicite

Tu expliques les dépendances entre plugins, les niveaux DAG, les violations éventuelles, et tu proposes des corrections architecturales. Tu réponds en français."

CHAT_HISTORY="[INST] ${SYSTEM_PROMPT}

Contexte : le graphe global est accessible dans ${GRAPH_FILE}.
Liste les plugins connus, leurs niveaux DAG, et réponds à mes questions sur l'architecture du workspace. [/INST]

Bonjour, je suis l'orchestrateur N3. Prêt à t'aider avec l'architecture du workspace Runner."

display_banner() {
    echo "═════════════════════════════════════════════════"
    echo "  Runner N3 Orchestrator — ${MODEL}"
    echo "  (tape /exit pour quitter, /reset pour réinitialiser)"
    echo "═════════════════════════════════════════════════"
    echo
}

chat() {
    local input="$1"
    local payload
    payload=$(jq -n \
        --arg model "$MODEL" \
        --arg prompt "${CHAT_HISTORY}

[INST] ${input} [/INST]" \
        '{
            model: $model,
            messages: [{ role: "user", content: $prompt }],
            stream: false
        }')
    curl -s http://localhost:11434/api/chat -d "$payload" | jq -r '.message.content // "Erreur: pas de reponse du modele"'
}

display_banner

while true; do
    echo -n "runner> "
    read -r line
    case "${line:-}" in
        /exit|/quit|exit)
            echo "Session terminee."
            break
            ;;
        /reset)
            CHAT_HISTORY="[INST] ${SYSTEM_PROMPT} [/INST]
Bonjour, je suis l'orchestrateur N3. Prêt à t'aider avec l'architecture du workspace Runner."
            echo "Contexte reinitialise."
            echo
            ;;
        "")
            continue
            ;;
        *)
            echo
            chat "$line"
            echo
            ;;
    esac
done
