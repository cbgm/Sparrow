plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
    alias(libs.plugins.sparrow.kmp.serialization)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.expenses"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.base)
            implementation(projects.core.ui)
            implementation(projects.core.util)
            implementation(projects.data.datastore)
            implementation(projects.feature.avatar)
            implementation(projects.feature.media)
            implementation(projects.feature.ocr)
            implementation(projects.feature.transport)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
            implementation(libs.bundles.ktor.client)
            implementation(libs.bundles.serialization)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
