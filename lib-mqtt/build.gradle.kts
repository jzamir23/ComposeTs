@file:Suppress("UnstableApiUsage")

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.serialization)
    `maven-publish`
}

android {
    namespace = "com.mqtt.lib"
    compileSdk = 33
    defaultConfig {
        minSdk = 23
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
//    implementation(libs.korLibs.kds)
    implementation(libs.korLibs.kds.android)
//    implementation(libs.korLibs.kmem)
    implementation(libs.korLibs.kmem.android)
//    implementation(libs.korLibs.klock)
    implementation(libs.korLibs.klock.android)
    compileOnly(libs.zstd.jni)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.reflect)
    implementation(libs.hivemq.mqtt.client)
    implementation(libs.kotlinx.datetime)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = "mqtt"// com.mqtt.wz:mqtt:1.0.1
                group = "com.mqtt.wz"
                version = "1.0.7"
                pom.withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")
                    configurations.implementation.get().allDependencies.forEach {
                        if (it.name != "unspecified" && it.version != "unspecified") {
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.name)
                            dependencyNode.appendNode("version", it.version)
                        }
                    }
                }
                afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
            }
        }
        repositories {
            maven {
                setUrl("https://packages.aliyun.com/maven/repository/2103831-release-ehKaRc/")
                credentials {
                    username = "60a23aa16da2f73831b26893"
                    password = "vA])Xd3-HwVe"
                }
            }
        }
    }
}

tasks.create("generateSourcesJar", Jar::class) {
    group = "fatJar"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        android.sourceSets.maybeCreate("main").java.srcDirs
    )
    archiveClassifier.set("sources")
}