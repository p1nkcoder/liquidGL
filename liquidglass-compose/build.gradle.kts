import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = providers.environmentVariable("GROUP").getOrElse("io.github.p1nkcoder.liquidgl")
version = providers.environmentVariable("VERSION").getOrElse("0.1.0-SNAPSHOT")

android {
    namespace = "io.github.p1nkcoder.liquidgl.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.foundation)

    testImplementation(libs.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "liquidglass-compose"
                pom {
                    name.set("liquidGL Compose")
                    description.set("Native liquid-glass refraction for Kotlin and Jetpack Compose")
                    url.set("https://github.com/p1nkcoder/liquidGL")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                }
            }
        }
    }
}
