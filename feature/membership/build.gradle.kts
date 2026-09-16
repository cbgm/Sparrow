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
            implementation(projects.data.database)
            implementation(projects.feature.contacts)
            implementation(libs.bundles.coroutines)
        }
    }
}
