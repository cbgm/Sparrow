package com.cbgm.sparrow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class SparrowKmpSerializationPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("sparrow.kmp.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    }
}
