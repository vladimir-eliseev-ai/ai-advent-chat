pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AI Advent Chat"
include(":app")
include(":article-summary-mcp-server")
include(":article-reader-mcp-server")
include(":article-summarizer-mcp-server")
include(":summary-storage-mcp-server")
include(":environment-orchestrator")
 