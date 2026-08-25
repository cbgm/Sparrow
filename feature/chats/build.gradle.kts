plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.chats"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.data.datastore)
            implementation(projects.core.crypto)
            implementation(projects.core.protocol)
            implementation(projects.core.ui)
            implementation(projects.data.database)
            implementation(projects.feature.contactimport)
            implementation(projects.feature.contacts)
            implementation(projects.feature.attachments)
            implementation(projects.feature.identity)
            implementation(projects.feature.media)
            implementation(projects.feature.safety)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
            implementation(libs.okio)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
