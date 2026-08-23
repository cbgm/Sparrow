plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
    alias(libs.plugins.about.libs)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.settings"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.datastore)
            implementation(projects.core.embedding)
            implementation(projects.core.ui)
            implementation(projects.feature.identity)
            implementation(projects.feature.search)
            implementation(projects.feature.safety)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)

            implementation(libs.about.libs.compose)
            implementation(libs.about.libs.render)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
    packageOfResClass = "com.cbgm.sparrow.feature.settings.resources"
}

/**
 * Export AboutLibraries definitions:
 *
 * .\gradlew.bat :feature:settings:exportLibraryDefinitions
 * '-PaboutLibraries.exportPath=src/commonMain/composeResources/files/'
 */
