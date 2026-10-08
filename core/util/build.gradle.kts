plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.core.util"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.coroutines)
            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
