import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.github.mobilebytelabs.kmptoolkit.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.dokka.gradle)
    compileOnly(libs.kover.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("kover") {
            id = "io.github.mobilebytelabs.kmptoolkit.kover"
            implementationClass = "KoverConventionPlugin"
            description =
                "Applies kover for multi-module coverage aggregation. On root it configures the report filters + verify rule; on a leaf module it self-registers into root's aggregation."
        }
        register("dokka") {
            id = "io.github.mobilebytelabs.kmptoolkit.dokka"
            implementationClass = "DokkaConventionPlugin"
            description =
                "Applies Dokka 2.0 (V2EnabledWithHelpers) with kmp-toolkit defaults — used by every cmp-* module so vanniktech's JavadocJar.Dokka(\"dokkaGeneratePublicationHtml\") has a real task to wrap."
        }
    }
}
