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
