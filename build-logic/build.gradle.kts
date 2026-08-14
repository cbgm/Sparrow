plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.cbgm.sparrow.buildlogic"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(
        "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}"
    )

    implementation(
        "org.jetbrains.kotlin.plugin.compose:" +
            "org.jetbrains.kotlin.plugin.compose.gradle.plugin:" +
            libs.versions.kotlin.get()
    )

    implementation(
        "org.jetbrains.kotlin.plugin.serialization:" +
            "org.jetbrains.kotlin.plugin.serialization.gradle.plugin:" +
            libs.versions.kotlin.get()
    )

    implementation(
        "org.jetbrains.compose:compose-gradle-plugin:" +
            libs.versions.composeMultiplatform.get()
    )

    implementation(
        "com.android.tools.build:gradle:${libs.versions.agp.get()}"
    )

    implementation(
        "com.google.devtools.ksp:symbol-processing-gradle-plugin:" +
            libs.versions.ksp.get()
    )

    implementation(
        "androidx.room:room-gradle-plugin:${libs.versions.room.get()}"
    )

    implementation(
        "dev.detekt:detekt-gradle-plugin:" +
            libs.versions.detekt.get()
    )

    implementation(
        "org.jlleitschuh.gradle.ktlint:" +
            "org.jlleitschuh.gradle.ktlint.gradle.plugin:" +
            libs.versions.ktlint.gradle.get()
    )

}

gradlePlugin {
    plugins {
        register("sparrowKmpLibrary") {
            id = "sparrow.kmp.library"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowKmpLibraryPlugin"
        }

        register("sparrowKmpCompose") {
            id = "sparrow.kmp.compose"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowKmpComposePlugin"
        }

        register("sparrowKmpTesting") {
            id = "sparrow.kmp.testing"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowKmpTestingPlugin"
        }

        register("sparrowKmpComposeFeature") {
            id = "sparrow.kmp.compose.feature"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowKmpComposeFeaturePlugin"
        }

        register("sparrowKmpSerialization") {
            id = "sparrow.kmp.serialization"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowKmpSerializationPlugin"
        }

        register("sparrowKmpRoom") {
            id = "sparrow.kmp.room"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowKmpRoomPlugin"
        }

        register("sparrowLint") {
            id = "sparrow.lint"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowLintPlugin"
        }

        register("sparrowArchitecture") {
            id = "sparrow.architecture"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowArchitecturePlugin"
        }

        register("sparrowQuality") {
            id = "sparrow.quality"
            implementationClass = "com.cbgm.sparrow.buildlogic.SparrowQualityPlugin"
        }

        register("localProperties") {
            id = "sparrow.local-properties"
            implementationClass = "com.cbgm.sparrow.buildlogic.LocalPropertiesPlugin"
        }
    }
}
