import groovy.json.JsonSlurper
import java.io.File

plugins {
    id("com.cheroliv.bakery") version "0.1.4"
    id("com.cheroliv.graphify") version "0.0.1"
}

val officePath: String = System.getenv("OFFICE_PATH") ?: "/home/cheroliv/workspace/office"
val siteName: String = project.findProperty("siteName") as? String ?: "cheroliv.com"

bakery {
    configPath.set(file("$officePath/sites/$siteName/site.yml").path)
}

graphify {
    rootDir.set(file("/home/cheroliv/workspace"))
    outputFile.set(file("$officePath/graph.json"))
    excludePatterns.set(
        excludePatterns.get() + listOf(
            "**/runtimes/**",
            "**/tmp/**",
            "**/cache/**",
            "**/snap/**",
            "**/downloads/**",
            "**/bin/**",
            "**/lib/**",
            "**/share/**",
            "**/.cache/**",
            "**/pilotage/**"
        )
    )
}

val dagLevels = mapOf(
    "graphify-gradle" to 0,
    "codebase-gradle" to 1,
    "bakery-gradle" to 2,
    "codex-gradle" to 2,
    "magic-stick" to 2,
    "planner-gradle" to 2,
    "plantuml-gradle" to 2,
    "quizz-benchmark-gradle" to 2,
    "quizz-benchmark-plugin" to 2,
    "readme-gradle" to 2,
    "slider-gradle" to 2,
    "training-gradle" to 2,
    "engine" to 3,
    "waiter-plugin" to 2,
    "office-template" to 2,
    "jhipster-gradle-plugins" to 2,
    "notebook-gradle" to 2,
    "newpipe-gradle" to 2,
    "saas-deploy-gradle" to 2,
    "scripts" to 2,
    "site" to 2
)

val workspaceRoot = File(System.getenv("HOME") ?: "/home/cheroliv").resolve("workspace")
val foundryDir = workspaceRoot.resolve("foundry/OSS")

tasks.register("verifyDagAcyclic") {
    group = "engine"
    description = "Verifies that no project imports a project of higher DAG level (Rule 4bis)"

    doLast {
        data class Violation(
            val project: String,
            val dependency: String,
            val projectLvl: Int,
            val depLvl: Int,
            val message: String
        )

        val violations = mutableListOf<Violation>()
        val projectDirs = foundryDir.listFiles { f: File -> f.isDirectory } ?: emptyArray()

        for (projectDir in projectDirs) {
            val projectName = projectDir.name
            val projectLevel = dagLevels[projectName] ?: continue

            val buildFile = projectDir.resolve("build.gradle.kts")
            if (!buildFile.exists()) continue

            val content = buildFile.readText()
            val regex = Regex("""id\(["']([^"']+)["']\)\s+version\s+["']([^"']+)["']""")
            val importedProjects = regex.findAll(content).map { match ->
                val pluginId = match.groupValues[1]
                pluginId.removePrefix("com.cheroliv.") to (dagLevels[pluginId.removePrefix("com.cheroliv.")] ?: -1)
            }.filter { dagLevels.containsKey(it.first) }.toList()

            for ((importedName, _) in importedProjects) {
                val depLevel = dagLevels[importedName] ?: continue
                if (depLevel > projectLevel) {
                    violations.add(
                        Violation(
                            project = projectName,
                            dependency = importedName,
                            projectLvl = projectLevel,
                            depLvl = depLevel,
                            message = "$projectName (N$projectLevel) imports $importedName (N$depLevel) — N$projectLevel cannot depend on N$depLevel"
                        )
                    )
                }
            }
        }

        if (violations.isEmpty()) {
            println("DAG Acyclic OK. No level violations found.")
        } else {
            println("DAG VIOLATIONS DETECTED (${violations.size}):")
            violations.forEach { v -> println("  ❌ ${v.message}") }
            throw RuntimeException(
                "DAG violations: ${violations.size} violation(s). Higher-level projects cannot depend on lower-level projects."
            )
        }
    }
}

tasks.register("aggregateGraphs") {
    group = "engine"
    description = "Aggregates local graph.json files from N0/N1/N2 projects into a global graph.json"
    dependsOn("scanWorkspace")

    val outputFile = workspaceRoot.resolve("office/graph.json")

    doLast {
        val graphFile = outputFile
        if (!graphFile.exists()) {
            println("Global graph.json not found at ${graphFile.absolutePath}. Run scanWorkspace first.")
            return@doLast
        }

        val json = JsonSlurper().parse(graphFile)
        if (json !is Map<*, *>) {
            throw RuntimeException("Invalid graph.json format")
        }

        val nodes = (json["nodes"] as? List<*>)?.size ?: 0
        val edges = (json["edges"] as? List<*>)?.size ?: 0
        @Suppress("UNCHECKED_CAST")
        val communities = (json["communities"] as? List<Map<String, Any>>)?.size ?: 0

        println("Graph aggregation validated: $nodes nodes, $edges edges, $communities communities")
        println("Global graph ready at ${graphFile.absolutePath}")
    }
}

val githubOrg = "cheroliv"
val pluginRepos = mapOf(
    "graphify-gradle" to "graphify-gradle",
    "codebase-gradle" to "codebase-gradle",
    "bakery-gradle" to "bakery-gradle",
    "codex-gradle" to "codex-gradle",
    "planner-gradle" to "planner-gradle",
    "plantuml-gradle" to "plantuml-gradle",
    "quizz-benchmark-gradle" to "quizz-benchmark-gradle",
    "readme-gradle" to "readme-gradle",
    "slider-gradle" to "slider-gradle",
    "training-gradle" to "training-gradle",
    "waiter-plugin" to "waiter-plugin",
    "jhipster-gradle-plugins" to "jhipster-gradle-plugins",
    "notebook-gradle" to "notebook-gradle",
    "newpipe-gradle" to "newpipe-gradle",
    "saas-deploy-gradle" to "saas-deploy-gradle",
    "magic_stick" to "magic_stick",
    "engine" to "engine"
)

tasks.register("provisionWorkspace") {
    group = "engine"
    description = "Generates a provision.sh script to bootstrap a full workspace"

    val outputDir = project.layout.buildDirectory.dir("provision")
    val provisionScript = outputDir.map { it.file("provision.sh") }

    outputs.file(provisionScript)

    doLast {
        val dest = provisionScript.get().asFile
        dest.parentFile.mkdirs()
        dest.writeText(buildProvisionScript(dagLevels, pluginRepos))
        dest.setExecutable(true)

        println("Workspace provision script generated: ${dest.absolutePath}")
        println()
        println("To provision a workspace:")
        println("  chmod +x ${dest.absolutePath}")
        println("  ${dest.absolutePath} /path/to/workspace")
    }
}

fun buildProvisionScript(levels: Map<String, Int>, repos: Map<String, String>): String {
    val sb = StringBuilder()
    sb.appendLine("#!/usr/bin/env bash")
    sb.appendLine("# Engine — provision.sh — Bootstrap a full workspace from scratch")
    sb.appendLine("# Generated by: engine provisionWorkspace")
    sb.appendLine()
    sb.appendLine("set -euo pipefail")
    sb.appendLine()
    sb.appendLine("WS=\${1:-\$(pwd)/workspace}")
    sb.appendLine("echo \"Provisioning workspace at \$WS\"")
    sb.appendLine("mkdir -p \"\$WS\"/{foundry/OSS,office,office/sites,office/pilotage,configuration,configuration/.agents,configuration/vision-archive,configuration/settings}")
    sb.appendLine()

    val byLevel = levels.entries
        .filter { repos.containsKey(it.key) }
        .groupBy({ it.value }, { it.key })
        .toSortedMap()

    for ((level, projects) in byLevel) {
        sb.appendLine("# ── N$level projects ──")
        for (project in projects) {
            val repo = repos[project]!!
            sb.appendLine("echo \"  Cloning $project (N$level)...\"")
            sb.appendLine("if [ ! -d \"\$WS/foundry/OSS/$project\" ]; then")
            sb.appendLine("    git clone --depth 1 https://github.com/$githubOrg/$repo.git \"\$WS/foundry/OSS/$project\" 2>/dev/null || true")
            sb.appendLine("fi")
        }
        sb.appendLine()
    }

    sb.appendLine("# Engine at workspace root symlink")
    sb.appendLine("ln -sf \"\$WS/foundry/OSS/engine/engine.sh\" \"\$WS/engine\" 2>/dev/null || true")
    sb.appendLine()
    sb.appendLine("echo \"Workspace provisioned.\"")
    sb.appendLine("echo \"Next steps:\"")
    sb.appendLine("echo \"  cd \$WS/foundry/OSS/engine && ./gradlew tasks --group engine\"")

    return sb.toString()
}

tasks.register("info") {
    group = "engine"
    description = "Shows engine status: DAG levels, known plugins, workspace paths"

    doLast {
        println("════════════════════════════════════════════")
        println("  Engine v1.0 — N3 Orchestration")
        println("════════════════════════════════════════════")
        println()
        println("Workspace root : ${workspaceRoot.absolutePath}")
        println("Foundry (OSS)   : ${foundryDir.absolutePath}")
        println("Office          : $officePath")
        println("GitHub org      : $githubOrg")
        println()

        val byLevel = dagLevels.entries.groupBy({ it.value }, { it.key }).toSortedMap()
        println("── DAG Registry (${dagLevels.size} projects) ──")
        for ((level, projects) in byLevel) {
            println("  N$level: ${projects.joinToString(", ")}")
        }
        println()
        println("── Engine Tasks ──")
        println("  aggregateGraphs    Validate global graph.json")
        println("  verifyDagAcyclic   Check DAG rule violations")
        println("  provisionWorkspace Generate workspace setup script")
        println("  info               This status display")
    }
}

tasks.register("usage") {
    group = "engine"
    description = "Shows available engine commands with usage examples"

    doLast {
        println("Engine CLI — Available Commands")
        println("================================")
        println()
        println("./engine <command>")
        println()
        println("Commands:")
        println("  info                 Show engine status and DAG registry")
        println("  verify               Run DAG acyclic verification")
        println("  graph                Run aggregateGraphs (requires scanWorkspace)")
        println("  provision            Generate provision.sh for workspace bootstrap")
        println("  tasks                List all Gradle tasks (engine + subprojects)")
        println()
        println("Direct Gradle:")
        println("  ./gradlew <task>     Run any Gradle task directly")
    }
}
