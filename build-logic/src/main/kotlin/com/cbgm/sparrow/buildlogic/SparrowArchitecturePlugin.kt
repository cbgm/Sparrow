package com.cbgm.sparrow.buildlogic

import com.cbgm.sparrow.buildlogic.architecture.discovery.discoverArchitectureModules
import com.cbgm.sparrow.buildlogic.architecture.serialization.ArchitectureModuleCodec
import com.cbgm.sparrow.buildlogic.architecture.task.GenerateArchitectureReportTask
import com.cbgm.sparrow.buildlogic.architecture.task.ValidateArchitectureTask
import com.cbgm.sparrow.buildlogic.architecture.task.VerifyArchitectureReportTask
import com.cbgm.sparrow.buildlogic.workflow.task.GenerateWorkflowCatalogTask
import com.cbgm.sparrow.buildlogic.workflow.task.VerifyWorkflowCatalogTask
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

            val orchestrationDirectory =
                layout.projectDirectory.dir(
                    ORCHESTRATION_DIRECTORY,
                )

            val workflowSources =
                fileTree(orchestrationDirectory) {
                    include("*/src/commonMain/kotlin/**/*Workflow.kt")
                }

            val workflowCatalogDirectory =
                layout.projectDirectory.dir(
                    WORKFLOW_CATALOG_DIRECTORY,
                )

            tasks.register<GenerateWorkflowCatalogTask>(
                WORKFLOW_CATALOG_TASK_NAME,
            ) {
                group = "documentation"
                description =
                    "Generates the conversation orchestration workflow catalog."

                this.workflowSources.from(workflowSources)
                rootDirectory.set(layout.projectDirectory)
                this.orchestrationDirectory.set(orchestrationDirectory)
                outputDirectory.set(workflowCatalogDirectory)
            }

            val verifyWorkflowCatalogTask =
                tasks.register<VerifyWorkflowCatalogTask>(
                    VERIFY_WORKFLOW_CATALOG_TASK_NAME,
                ) {
                    group = "verification"
                    description =
                        "Verifies that the workflow catalog matches the workflow sources."

                    this.workflowSources.from(workflowSources)
                    rootDirectory.set(layout.projectDirectory)
                    this.orchestrationDirectory.set(orchestrationDirectory)
                    catalogDirectory.set(workflowCatalogDirectory)
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
                dependsOn(
                    verifyWorkflowCatalogTask,
                )
            }
        }
    }

    private companion object {

        const val ORCHESTRATION_DIRECTORY =
            "feature/conversationorchestration"

        const val WORKFLOW_CATALOG_DIRECTORY =
            "docs/architecture/workflows"

        const val WORKFLOW_CATALOG_TASK_NAME =
            "workflowCatalog"

        const val VERIFY_WORKFLOW_CATALOG_TASK_NAME =
            "verifyWorkflowCatalog"

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
