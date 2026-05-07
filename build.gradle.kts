plugins {
    id("com.cheroliv.bakery") version "0.1.3"
}

val officePath = System.getenv("OFFICE_PATH") ?: "/home/cheroliv/workspace/office"

bakery {
    configPath.set(file("$officePath/sites/cheroliv.com/site.yml").path)
}

// TODO: includeBuild() pour tous les plugins N0/N1/N2 en dev mode
// TODO: tasks orchestration: aggregateGraphs, verifyDagAcyclic
// TODO: interface CLI + API Edster