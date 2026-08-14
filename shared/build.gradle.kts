import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

val isMacOs =
    System
        .getProperty("os.name")
        .startsWith(
            prefix = "Mac",
            ignoreCase = true
        )

plugins {
    alias(libs.plugins.securechat.kmp.compose.feature)
    alias(libs.plugins.securechat.kmp.serialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.securechat.properties)
}

buildkonfig {
    packageName = "com.cbgm.securechat"

    defaultConfigs {
        buildConfigField(
            STRING,
            "CONTROL_PLANE_DIRECTORY_URL",
            localProperties.get(
                key = "controlPlaneDirectoryUrl",
                defaultValue = ""
            ),
            const = true
        )
    }
}

kotlin {
    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { target ->
            target.binaries.framework {
                baseName = "SecureChat"
                isStatic = true
            }
        }
    }

    android {
        namespace = "com.cbgm.securechat.shared"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
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
            implementation(compose.materialIconsExtended)
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
