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
            implementation(projects.feature.contacts)
            implementation(libs.bundles.coroutines)
        }
    }
}
