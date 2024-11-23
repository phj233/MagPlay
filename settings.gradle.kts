pluginManagement {
    repositories {
        maven( url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public"))
        maven( url = uri("https://maven.aliyun.com/repository/public"))
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
        maven( url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public"))
        maven( url = uri("https://maven.aliyun.com/repository/public"))
        maven( url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        maven( url = uri("https://jitpack.io"))
        google()
        mavenCentral()
    }
}

rootProject.name = "MagPlay"
include(":app")
