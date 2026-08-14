package com.cbgm.sparrow.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class SparrowKmpLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("sparrow.lint")

        extensions.configure<KotlinMultiplatformExtension> {
            val isMacOs = System
                .getProperty("os.name")
                .startsWith(
                    prefix = "Mac",
                    ignoreCase = true
                )

            if (isMacOs) {
                iosArm64()
                iosSimulatorArm64()
            }

            targets
                .withType<KotlinMultiplatformAndroidLibraryTarget>()
                .configureEach {
                    compileSdk = libs.intVersion("android-compileSdk")

                    minSdk = libs.intVersion("android-minSdk")

                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
        }
    }
}