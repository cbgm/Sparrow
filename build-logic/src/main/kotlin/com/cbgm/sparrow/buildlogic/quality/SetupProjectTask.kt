package com.cbgm.sparrow.buildlogic.quality

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class SetupProjectTask : DefaultTask() {

    @TaskAction
    fun completeSetup() {
        logger.lifecycle("")
        logger.lifecycle("Sparrow project setup completed.")
        logger.lifecycle("")
        logger.lifecycle("Git hooks are enabled:")
        logger.lifecycle(" - pre-commit: formats Kotlin code")
        logger.lifecycle(" - pre-push: runs all quality checks")
        logger.lifecycle("")
        logger.lifecycle("Documentation:")
        logger.lifecycle(" - Edit Markdown under docs/")
        logger.lifecycle(" - Run ./gradlew architectureReport after module graph changes")
        logger.lifecycle(" - GitHub Actions builds and publishes the MkDocs site")
        logger.lifecycle("")
    }
}
