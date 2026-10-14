plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.ocr"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.util)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.mlkit.text.recognition)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
