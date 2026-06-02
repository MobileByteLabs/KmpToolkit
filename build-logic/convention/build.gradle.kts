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
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("dokka") {
            id = "io.github.mobilebytelabs.kmptoolkit.dokka"
            implementationClass = "DokkaConventionPlugin"
            description =
                "Applies Dokka 2.0 (V2EnabledWithHelpers) with kmp-toolkit defaults — used by every cmp-* module so vanniktech's JavadocJar.Dokka(\"dokkaGeneratePublicationHtml\") has a real task to wrap."
        }
    }
}
