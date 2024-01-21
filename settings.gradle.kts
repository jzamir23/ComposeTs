@file:Suppress("UnstableApiUsage")

val snapshotVersion: String? = System.getenv("COMPOSE_SNAPSHOT_ID")
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        snapshotVersion?.let {
            println("https://androidx.dev/snapshots/builds/$it/artifacts/repository/")
            maven { url = uri("https://androidx.dev/snapshots/builds/$it/artifacts/repository/") }
        }
        google()
        mavenCentral()
        maven {
            credentials {
                username = "616e53e660ebaf99ec290f15"
                password = "4m=p-LsjACQt"
            }
            setUrl("https://packages.aliyun.com/maven/repository/2103831-release-ehKaRc/")
        }
    }
}
rootProject.name = "ComposeTs"
include(":app")
include(":lib-mqtt")