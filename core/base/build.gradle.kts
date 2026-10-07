plugins {
    alias(libs.plugins.sparrow.kmp.serialization)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.core.base"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.serialization)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
