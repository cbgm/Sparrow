package com.cbgm.sparrow.buildlogic

import com.cbgm.sparrow.buildlogic.architecture.discovery.discoverArchitectureModules
import com.cbgm.sparrow.buildlogic.architecture.serialization.ArchitectureModuleCodec
import com.cbgm.sparrow.buildlogic.architecture.task.GenerateArchitectureReportTask
import com.cbgm.sparrow.buildlogic.architecture.task.ValidateArchitectureTask
import com.cbgm.sparrow.buildlogic.architecture.task.VerifyArchitectureReportTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class SparrowArchitecturePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            require(this == rootProject) {
                buildString {
                    append("Plugin 'sparrow.architecture' ")
                    append("must only be applied to the root project.")
                }
            }

            pluginManager.apply("base")

            val generatedDocumentationDirectory =
                layout.projectDirectory.dir(
                    GENERATED_DOCUMENTATION_DIRECTORY,
                )

            val validationTask =
                tasks.register<ValidateArchitectureTask>(
                    VALIDATE_TASK_NAME,
                ) {
                    group = "verification"
                    description =
                        "Validates the automatically discovered project graph."
                }

            val reportTask =
                tasks.register<GenerateArchitectureReportTask>(
                    REPORT_TASK_NAME,
                ) {
                    group = "documentation"
                    description =
                        "Generates tracked architecture documentation."

                    outputDirectory.set(
                        generatedDocumentationDirectory,
                    )
                }

            val verificationTask =
                tasks.register<VerifyArchitectureReportTask>(
                    VERIFY_REPORT_TASK_NAME,
                ) {
                    group = "verification"
                    description =
                        "Verifies that tracked architecture documentation is current."

                    generatedDirectory.set(
                        generatedDocumentationDirectory,
                    )
                }

            gradle.projectsEvaluated {
                val encodedModules =
                    discoverArchitectureModules()
                        .map(ArchitectureModuleCodec::encode)

                validationTask.configure {
                    moduleDefinitions.set(
                        encodedModules,
                    )
                }

                reportTask.configure {
                    moduleDefinitions.set(
                        encodedModules,
                    )
                }

                verificationTask.configure {
                    moduleDefinitions.set(
                        encodedModules,
                    )
                }
            }

            tasks.named("check").configure {
                dependsOn(
                    validationTask,
                )
                dependsOn(
                    verificationTask,
                )
            }
        }
    }

    private companion object {

        const val VALIDATE_TASK_NAME =
            "validateArchitecture"

        const val REPORT_TASK_NAME =
            "architectureReport"

        const val VERIFY_REPORT_TASK_NAME =
            "verifyArchitectureReport"

        const val GENERATED_DOCUMENTATION_DIRECTORY =
            "docs/generated"
    }
}
