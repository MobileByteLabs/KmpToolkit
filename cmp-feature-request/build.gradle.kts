import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlinSerialization)
}

// ============================================================================
// LIBRARY CONFIGURATION
// ============================================================================
group = "io.github.mobilebytesensei"
version = "2.0.0"

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate()

    // JVM
    jvm()

    // Android
    androidLibrary {
        namespace = "com.mobilebytesensei.featurerequest"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    // iOS
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // macOS
    macosX64()
    macosArm64()

    // JavaScript
    js {
        browser()
        nodejs()
    }

    // WebAssembly
    wasmJs {
        browser()
        nodejs()
    }

    // Compiler Options
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Source Sets
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.animation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.supabase.postgrest)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kermit)
            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }

        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING CONFIGURATION
// ============================================================================
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "kmptoolkit-feature-request", version.toString())

    pom {
        name = "KMP Feature Request"
        description = "Cross-platform feature request, bug report, and support ticket system for Compose Multiplatform"
        inceptionYear = "2025"
        url = "https://github.com/MobileByteLabs/KmpToolkit/"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "MobileByteLabs"
                name = "MobileByteLabs"
                url = "https://github.com/MobileByteLabs"
            }
        }

        scm {
            url = "https://github.com/MobileByteLabs/KmpToolkit/"
            connection = "scm:git:git://github.com/MobileByteLabs/KmpToolkit.git"
            developerConnection = "scm:git:ssh://git@github.com/MobileByteLabs/KmpToolkit.git"
        }
    }
}
