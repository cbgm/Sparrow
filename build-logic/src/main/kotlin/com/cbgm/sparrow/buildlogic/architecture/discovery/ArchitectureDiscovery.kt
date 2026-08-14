package com.cbgm.sparrow.buildlogic.architecture.discovery

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import java.io.File

internal fun Project.discoverArchitectureModules():
    List<ArchitectureModule> {
    return rootProject.subprojects
        .map(Project::toArchitectureModule)
        .sortedBy(ArchitectureModule::path)
}

private fun Project.toArchitectureModule(): ArchitectureModule {
    val sourceDirectory = projectDir.resolve(SOURCE_DIRECTORY_NAME)
    val sourceSets = sourceDirectory
        .listFiles()
        .orEmpty()
        .asSequence()
        .filter(File::isDirectory)
        .map(File::getName)
        .toSortedSet()

    val kotlinFiles = sourceDirectory.filesWithExtension(KOTLIN_EXTENSION)
    val testKotlinFileCount = kotlinFiles.count { file ->
        file.isInsideTestSourceSet(sourceDirectory)
    }

    return ArchitectureModule(
        name = name,
        path = path,
        group = detectArchitectureGroup(),
        directory = projectDir.relativeTo(rootProject.projectDir).invariantSeparatorsPath,
        buildFile = buildFile.relativeTo(rootProject.projectDir).invariantSeparatorsPath,
        sourceSets = sourceSets,
        kotlinSourceFileCount = kotlinFiles.size,
        testKotlinFileCount = testKotlinFileCount,
        resourceFileCount = sourceDirectory.filesWithResourceExtension().size,
        dependencies = collectProductionProjectDependencies(),
    )
}

private fun Project.detectArchitectureGroup(): String {
    return path
        .removePrefix(":")
        .substringBefore(":")
        .ifBlank {
            ROOT_GROUP
        }
}

private fun Project.collectProductionProjectDependencies():
    Set<String> {
    return configurations
        .asSequence()
        .filter(ConfigurationFilter::isProductionConfiguration)
        .flatMap { configuration ->
            configuration.dependencies
                .withType(ProjectDependency::class.java)
                .asSequence()
        }
        .map(ProjectDependency::getPath)
        .filterNot { dependencyPath ->
            dependencyPath == path
        }
        .toSortedSet()
}

private fun File.filesWithExtension(
    extension: String,
): List<File> {
    if (!exists()) {
        return emptyList()
    }

    return walkTopDown()
        .filter(File::isFile)
        .filter { file ->
            file.extension.equals(
                other = extension,
                ignoreCase = true,
            )
        }
        .toList()
}

private fun File.filesWithResourceExtension(): List<File> {
    if (!exists()) {
        return emptyList()
    }

    return walkTopDown()
        .filter(File::isFile)
        .filter { file ->
            file.extension.lowercase() in RESOURCE_EXTENSIONS
        }
        .toList()
}

private fun File.isInsideTestSourceSet(
    sourceDirectory: File,
): Boolean {
    val relativePath = relativeTo(sourceDirectory).invariantSeparatorsPath
    val sourceSet = relativePath.substringBefore('/')

    return sourceSet.contains(
        other = TEST_SOURCE_SET_MARKER,
        ignoreCase = true,
    )
}

private object ConfigurationFilter {

    private val excludedNameParts = setOf(
        "test",
        "androidtest",
        "devicetest",
        "hosttest",
        "benchmark",
        "lint",
        "detekt",
        "ktlint",
        "ksp",
    )

    fun isProductionConfiguration(
        configuration: Configuration,
    ): Boolean {
        val normalizedName =
            configuration.name.lowercase()

        return excludedNameParts.none { excludedPart ->
            normalizedName.contains(excludedPart)
        }
    }
}

private const val ROOT_GROUP = "root"
private const val SOURCE_DIRECTORY_NAME = "src"
private const val KOTLIN_EXTENSION = "kt"
private const val TEST_SOURCE_SET_MARKER = "test"

private val RESOURCE_EXTENSIONS = setOf(
    "xml",
    "json",
    "properties",
    "md",
    "txt",
    "png",
    "jpg",
    "jpeg",
    "webp",
    "svg",
    "ttf",
    "otf",
)
