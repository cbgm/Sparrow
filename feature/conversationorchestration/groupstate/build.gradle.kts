plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.conversationorchestration.groupstate"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.conversationorchestration.message)
            implementation(projects.core.base)
            implementation(projects.core.util)
            implementation(projects.core.crypto)
            implementation(projects.protocol)
            implementation(projects.data.database)
            implementation(projects.data.datastore)
            implementation(projects.feature.attachments)
            implementation(projects.feature.contacts)
            implementation(projects.feature.identity)
            implementation(projects.feature.invite)
            implementation(projects.feature.membership)

            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
