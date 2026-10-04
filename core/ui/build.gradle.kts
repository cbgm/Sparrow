plugins {
    alias(libs.plugins.sparrow.kmp.compose)
    alias(libs.plugins.sparrow.kmp.serialization)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.core.ui"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.resources)
            implementation(libs.bundles.compose)
            implementation(libs.coil.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
            implementation(libs.bundles.serialization)

            implementation(libs.androidx.lifecycle.runtimeCompose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
