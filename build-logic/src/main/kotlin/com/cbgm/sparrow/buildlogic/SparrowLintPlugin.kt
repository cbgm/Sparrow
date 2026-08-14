package com.cbgm.sparrow.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

class SparrowLintPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        configureDetekt()
        configureKtlint()
        configureLintDependencies()
    }

    private fun Project.configureDetekt() {
        extensions.configure<DetektExtension> {
            toolVersion.set(
                libs.requiredVersion("detekt"),
            )

            buildUponDefaultConfig.set(true)
            parallel.set(true)
            ignoreFailures.set(false)

            config.setFrom(
                rootProject.files(
                    "config/detekt/detekt.yml",
                ),
            )

            basePath.set(
                rootProject.projectDir,
            )
        }
    }

    private fun Project.configureKtlint() {
        extensions.configure<KtlintExtension> {
            version.set(
                libs.requiredVersion("ktlint-engine"),
            )

            verbose.set(true)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            enableExperimentalRules.set(false)

            additionalEditorconfig.set(
                mapOf(
                    "ktlint_standard_filename" to "disabled",
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_property-naming" to "disabled",
                    "ktlint_standard_class-naming" to "disabled",
                    "ktlint_standard_max-line-length" to "disabled",
                ),
            )

            reporters {
                reporter(ReporterType.PLAIN)
                reporter(ReporterType.CHECKSTYLE)
            }

            filter {
                exclude { entry ->
                    val path = entry.file.invariantSeparatorsPath

                    path.contains("/build/") ||
                        path.contains("/generated/") ||
                        path.contains("/composeResources/")
                }
            }
        }
    }

    private fun Project.configureLintDependencies() {
        dependencies.add(
            "detektPlugins",
            libs.requiredLibrary(
                "compose-rules-detekt",
            ),
        )

        dependencies.add(
            "detektPlugins",
            libs.requiredLibrary(
                "compose-rules-detekt",
            ),
        )

        dependencies.add(
            "detektPlugins",
            dependencies.project(
                mapOf(
                    "path" to ":quality:detekt-rules",
                ),
            ),
        )

        tasks.withType<Detekt>().configureEach {
            dependsOn(":quality:detekt-rules:assemble")
        }
    }
}
