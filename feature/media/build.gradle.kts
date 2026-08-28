plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.media"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.ui)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.coil.compose)
            implementation(libs.coil.video)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.photopicker.compose)
            implementation(libs.koin.android)
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
