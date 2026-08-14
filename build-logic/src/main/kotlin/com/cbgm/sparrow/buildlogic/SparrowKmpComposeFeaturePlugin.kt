package com.cbgm.sparrow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class SparrowKmpComposeFeaturePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("sparrow.kmp.compose")
        pluginManager.apply("sparrow.kmp.testing")
    }
}
