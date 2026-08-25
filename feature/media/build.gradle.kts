plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.media"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(libs.bundles.compose)
        }

        androidMain.dependencies {
            implementation(libs.coil.compose)
            implementation(libs.coil.video)
        }

        iosMain.dependencies {
            implementation(libs.coil.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
