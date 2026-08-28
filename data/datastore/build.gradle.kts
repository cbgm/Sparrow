plugins {
    alias(libs.plugins.sparrow.kmp.library)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.data.datastore"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
            implementation(libs.okio)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
