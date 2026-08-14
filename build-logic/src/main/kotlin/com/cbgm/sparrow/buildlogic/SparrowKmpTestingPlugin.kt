package com.cbgm.sparrow.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class SparrowKmpTestingPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("sparrow.kmp.library")

        pluginManager.withPlugin(
            "com.android.kotlin.multiplatform.library"
        ) {
            extensions.configure<KotlinMultiplatformExtension> {
                targets
                    .withType<KotlinMultiplatformAndroidLibraryTarget>()
                    .configureEach {
                        withHostTest {
                            isIncludeAndroidResources = true
                        }

                        withDeviceTest {
                            instrumentationRunner =
                                "androidx.test.runner.AndroidJUnitRunner"

                            execution = "HOST"
                        }
                    }
            }
        }
    }
}