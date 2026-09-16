plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.membership"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.protocol)
            implementation(projects.data.database)
            implementation(projects.feature.contacts)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }
    }
}
