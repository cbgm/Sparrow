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
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.androidx.camera.video)
        }

        iosMain.dependencies {
            implementation(libs.coil.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
