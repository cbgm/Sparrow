import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
    alias(libs.plugins.sparrow.kmp.serialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.sparrow.properties)
}

buildkonfig {
    packageName = "com.cbgm.sparrow"

    defaultConfigs {
        buildConfigField(
            STRING,
            "CONTROL_PLANE_DIRECTORY_URL",
            localProperties.get(
                key = "CONTROL_PLANE_DIRECTORY_URL",
                defaultValue = ""
            ),
            const = true
        )
    }

    defaultConfigs("release") {
        buildConfigField(
            STRING,
            "CONTROL_PLANE_DIRECTORY_URL",
            localProperties.get(
                key = "CONTROL_PLANE_RELEASE_DIRECTORY_URL",
                defaultValue = ""
            ),
            const = true
        )
    }
}

kotlin {

    android {
        namespace = "com.cbgm.sparrow.shared"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.datastore)
            implementation(projects.core.crypto)
            implementation(projects.core.protocol)
            implementation(projects.core.ui)
            implementation(projects.navigation)
            implementation(projects.feature.chats)
            implementation(projects.feature.contactimport)
            implementation(projects.feature.contacts)
            implementation(projects.feature.identity)
            implementation(projects.feature.messaging)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.settings)
            implementation(projects.feature.transport)
            implementation(projects.notification)
            implementation(projects.startup)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
            implementation(libs.bundles.serialization)

            implementation(libs.jetbrains.navigation.compose)
        }

        androidMain.dependencies {
            implementation(projects.data.database)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.workmanager)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
