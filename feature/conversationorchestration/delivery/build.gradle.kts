plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.conversationorchestration.delivery"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.conversationorchestration.engine)
            implementation(projects.core.base)
            implementation(projects.core.util)
            implementation(projects.core.crypto)
            implementation(projects.protocol)
            implementation(projects.data.database)
            implementation(projects.feature.contacts)
            implementation(projects.feature.identity)
            implementation(projects.feature.membership)
            implementation(projects.feature.messaging)
            implementation(projects.feature.transport)

            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
