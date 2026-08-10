plugins {
    alias(libs.plugins.securechat.kmp.library)
    alias(libs.plugins.securechat.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.notification"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.crypto)
            implementation(projects.feature.chats)
            implementation(projects.feature.messaging)
            implementation(projects.feature.transport)

            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }

        androidMain.dependencies {
            implementation(projects.resources)

            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.work.runtime)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.analytics)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.workmanager)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
