plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.conversationorchestration.engine"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.conversationorchestration.api)
            implementation(projects.core.util)

            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
