import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sparrow.lint)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val appVersionCode =
    providers
        .gradleProperty("appVersionCode")
        .map(String::toInt)
        .orElse(
            providers
                .environmentVariable("GITHUB_RUN_NUMBER")
                .map { 10_000 + it.toInt() }
        )
        .orElse(
            providers.exec {
                commandLine("git", "rev-list", "--count", "HEAD")
            }.standardOutput.asText
                .map { 10_000 + it.trim().toInt() }
        )
        .getOrElse(10_001)

val appVersionName =
    providers
        .gradleProperty("appVersionName")
        .orElse(
            providers.exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
            }.standardOutput.asText
                .map { it.trim().removePrefix("v") }
        )
        .getOrElse("1.0.0")

android {
    namespace = "com.cbgm.sparrow"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.cbgm.sparrow"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("../feature/voice/src/androidMain/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = localProperties.getProperty("KEY_STORE_FILE") ?: ""

            storeFile = when {
                keystorePath.isEmpty() -> null
                // If it is a full local Windows/Mac path, use it directly
                File(keystorePath).isAbsolute -> File(keystorePath)
                // If it's a simple filename (like on GitHub Actions), look in the root folder
                else -> rootProject.file(keystorePath)
            }
            storePassword = localProperties.getProperty("KEY_STORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    debugImplementation(libs.compose.uiTooling)
}
