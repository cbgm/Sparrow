plugins {
    alias(libs.plugins.sparrow.kmp.compose)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.resources"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
    packageOfResClass = "com.cbgm.sparrow.resources"
}
