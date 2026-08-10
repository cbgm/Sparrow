plugins {
    alias(libs.plugins.securechat.kmp.compose)
    alias(libs.plugins.securechat.kmp.serialization)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.core.ui"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.resources)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
            implementation(libs.bundles.serialization)

            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
    }
}
