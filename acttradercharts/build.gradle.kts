plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.acttrader.acttradercharts"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        aarMetadata {
            minCompileSdk = 24
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
            assets.srcDirs("src/main/assets")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.properties["GROUP_ID"] as String
                artifactId = project.properties["ARTIFACT_ID"] as String
                version = project.properties["VERSION_NAME"] as String

                pom {
                    name.set("ActtraderCharts Android")
                    description.set("Android WebView wrapper for acttrader-charts")
                    url.set("https://github.com/${System.getenv("GITHUB_REPOSITORY") ?: "acttrader/acttrader-charts-android"}")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                }
            }
        }

        repositories {
            val ghActor = System.getenv("GITHUB_ACTOR")
            val ghToken = System.getenv("GITHUB_TOKEN")
            if (!ghToken.isNullOrBlank()) {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "acttrader/acttrader-charts-android"}")
                    credentials {
                        username = ghActor ?: ""
                        password = ghToken
                    }
                }
            }
        }
    }
}
