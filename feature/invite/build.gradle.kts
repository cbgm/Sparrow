plugins {
    alias(libs.plugins.sparrow.kmp.library)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.invite"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.protocol)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }
    }
}
