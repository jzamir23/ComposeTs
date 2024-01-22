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
            setUrl("https://packages.aliyun.com/maven/repository/2451671-release-NAQFTP/")
            credentials {
                username = "65adb8c280528f89ca14f9f7"
                password = "9xe48Swe4gGU"
            }
        }

        maven {
            setUrl("https://packages.aliyun.com/maven/repository/2451671-snapshot-hSSpID/")
            credentials {
                username = "65adb8c280528f89ca14f9f7"
                password = "9xe48Swe4gGU"
            }
        }
    }
}
rootProject.name = "ComposeTs"
include(":app")
include(":lib-mqtt")