package com.cbgm.sparrow.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SparrowKmpRoomPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("sparrow.kmp.library")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        dependencies.add(
            "kspAndroid",
            libs.findLibrary("androidx-room-compiler").get()
        )

        val isMacOs = System
            .getProperty("os.name")
            .startsWith(
                prefix = "Mac",
                ignoreCase = true
            )

        if (isMacOs) {
            dependencies.add(
                "kspIosArm64",
                libs.findLibrary("androidx-room-compiler").get()
            )

            dependencies.add(
                "kspIosSimulatorArm64",
                libs.findLibrary("androidx-room-compiler").get()
            )
        }
    }
}
