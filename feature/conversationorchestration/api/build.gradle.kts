plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.conversationorchestration.api"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.base)

            implementation(libs.bundles.coroutines)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
