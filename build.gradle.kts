plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.about.libs) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.sparrow.architecture)
    alias(libs.plugins.sparrow.quality)
}
