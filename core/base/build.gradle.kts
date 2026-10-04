plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.core.base"
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
