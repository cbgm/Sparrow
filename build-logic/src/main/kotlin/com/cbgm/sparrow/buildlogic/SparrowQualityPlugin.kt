package com.cbgm.sparrow.buildlogic

import com.cbgm.sparrow.buildlogic.quality.InstallGitHooksTask
import com.cbgm.sparrow.buildlogic.quality.SetupProjectTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class SparrowQualityPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        require(this == rootProject) {
            "Plugin 'sparrow.quality' must only be applied to the root project."
        }

        pluginManager.apply("base")

        val installGitHooks = registerInstallGitHooksTask()
        registerSetupTask(installGitHooks)

        val qualityFix = tasks.register(
            QUALITY_FIX_TASK_NAME,
        ) {
            group = QUALITY_TASK_GROUP
            description = "Automatically formats all Kotlin source files with KtLint."
        }

        val qualityCheck = tasks.register(
            QUALITY_CHECK_TASK_NAME,
        ) {
            group = QUALITY_TASK_GROUP
            description = "Runs KtLint, Detekt, and architecture validation without modifying files."
        }

        val quality = tasks.register(
            QUALITY_TASK_NAME,
        ) {
            group = QUALITY_TASK_GROUP
            description = "Formats source files and then runs all quality checks."

            dependsOn(
                qualityFix,
                qualityCheck,
            )
        }

        gradle.projectsEvaluated {
            configureQualityTasks(
                qualityFix = qualityFix,
                qualityCheck = qualityCheck,
                quality = quality,
            )
        }

        tasks.named("check").configure {
            dependsOn(qualityCheck)
        }
    }

    private fun Project.registerInstallGitHooksTask():
        TaskProvider<InstallGitHooksTask> {
        return tasks.register<InstallGitHooksTask>(
            INSTALL_GIT_HOOKS_TASK_NAME,
        ) {
            group = SETUP_TASK_GROUP
            description = "Configures Git to use the repository's tracked hooks."

            repositoryDirectory.set(
                rootProject.layout.projectDirectory,
            )

            hooksDirectory.set(
                rootProject.layout.projectDirectory.dir(
                    GIT_HOOKS_DIRECTORY,
                ),
            )

            hooksPath.set(
                GIT_HOOKS_DIRECTORY,
            )
        }
    }

    private fun Project.registerSetupTask(
        installGitHooks: TaskProvider<InstallGitHooksTask>,
    ) {
        tasks.register<SetupProjectTask>(
            SETUP_TASK_NAME,
        ) {
            group = SETUP_TASK_GROUP
            description = "Performs the initial Sparrow project setup."

            dependsOn(
                installGitHooks,
            )
        }
    }

    private fun Project.configureQualityTasks(
        qualityFix: TaskProvider<Task>,
        qualityCheck: TaskProvider<Task>,
        quality: TaskProvider<Task>,
    ) {
        val projects =
            listOf(rootProject) + rootProject.subprojects

        val formatTasks = projects.flatMap { project ->
            project.tasks
                .matching { task ->
                    task.name == KTLINT_FORMAT_TASK_NAME
                }
                .toList()
        }

        val ktlintCheckTasks = projects.flatMap { project ->
            project.tasks
                .matching { task ->
                    task.name == KTLINT_CHECK_TASK_NAME
                }
                .toList()
        }

        val detektTasks = projects.flatMap { project ->
            project.tasks
                .matching { task ->
                    task.name == DETEKT_TASK_NAME
                }
                .toList()
        }

        val architectureValidationTasks =
            rootProject.tasks
                .matching { task ->
                    task.name == VALIDATE_ARCHITECTURE_TASK_NAME
                }
                .toList()

        val architectureReportVerificationTasks =
            rootProject.tasks
                .matching { task ->
                    task.name == VERIFY_ARCHITECTURE_REPORT_TASK_NAME
                }
                .toList()

        qualityFix.configure {
            dependsOn(formatTasks)
        }

        qualityCheck.configure {
            dependsOn(ktlintCheckTasks)
            dependsOn(detektTasks)
            dependsOn(architectureValidationTasks)
            dependsOn(architectureReportVerificationTasks)
        }

        val verificationTasks =
            ktlintCheckTasks +
                detektTasks +
                architectureValidationTasks +
                architectureReportVerificationTasks

        verificationTasks.forEach { verificationTask ->
            verificationTask.mustRunAfter(formatTasks)
        }

        quality.configure {
            dependsOn(
                qualityFix,
                qualityCheck,
            )
        }
    }

    private companion object {

        const val QUALITY_TASK_GROUP = "verification"
        const val SETUP_TASK_GROUP = "setup"

        const val SETUP_TASK_NAME = "setup"
        const val INSTALL_GIT_HOOKS_TASK_NAME = "installGitHooks"

        const val QUALITY_TASK_NAME = "quality"
        const val QUALITY_FIX_TASK_NAME = "qualityFix"
        const val QUALITY_CHECK_TASK_NAME = "qualityCheck"

        const val KTLINT_FORMAT_TASK_NAME = "ktlintFormat"
        const val KTLINT_CHECK_TASK_NAME = "ktlintCheck"
        const val DETEKT_TASK_NAME = "detekt"

        const val VALIDATE_ARCHITECTURE_TASK_NAME = "validateArchitecture"

        const val VERIFY_ARCHITECTURE_REPORT_TASK_NAME =
            "verifyArchitectureReport"

        const val GIT_HOOKS_DIRECTORY = ".githooks"
    }
}
