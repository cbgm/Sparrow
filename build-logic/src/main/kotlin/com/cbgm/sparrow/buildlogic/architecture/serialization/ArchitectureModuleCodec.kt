package com.cbgm.sparrow.buildlogic.architecture.serialization

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object ArchitectureModuleCodec {

    fun encode(
        module: ArchitectureModule,
    ): String {
        return listOf(
            module.name,
            module.path,
            module.group,
            module.directory,
            module.buildFile,
            module.sourceSets.encodeList(),
            module.kotlinSourceFileCount.toString(),
            module.testKotlinFileCount.toString(),
            module.resourceFileCount.toString(),
            module.dependencies.encodeList(),
        ).joinToString(
            separator = FIELD_SEPARATOR.toString(),
        )
    }

    fun decode(
        value: String,
    ): ArchitectureModule {
        val fields = value.split(
            FIELD_SEPARATOR,
            limit = FIELD_COUNT,
        )

        require(fields.size == FIELD_COUNT) {
            "Invalid encoded architecture module: $value"
        }

        return ArchitectureModule(
            name = fields[NAME_INDEX],
            path = fields[PATH_INDEX],
            group = fields[GROUP_INDEX],
            directory = fields[DIRECTORY_INDEX],
            buildFile = fields[BUILD_FILE_INDEX],
            sourceSets = fields[SOURCE_SETS_INDEX].decodeList(),
            kotlinSourceFileCount = fields[KOTLIN_FILE_COUNT_INDEX].toInt(),
            testKotlinFileCount = fields[TEST_FILE_COUNT_INDEX].toInt(),
            resourceFileCount = fields[RESOURCE_FILE_COUNT_INDEX].toInt(),
            dependencies = fields[DEPENDENCIES_INDEX].decodeList(),
        )
    }

    private fun Collection<String>.encodeList(): String {
        return sorted().joinToString(
            separator = LIST_SEPARATOR.toString(),
        )
    }

    private fun String.decodeList(): Set<String> {
        return split(LIST_SEPARATOR)
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private const val FIELD_SEPARATOR: Char = '\u001F'
    private const val LIST_SEPARATOR: Char = '\u001E'

    private const val FIELD_COUNT = 10

    private const val NAME_INDEX = 0
    private const val PATH_INDEX = 1
    private const val GROUP_INDEX = 2
    private const val DIRECTORY_INDEX = 3
    private const val BUILD_FILE_INDEX = 4
    private const val SOURCE_SETS_INDEX = 5
    private const val KOTLIN_FILE_COUNT_INDEX = 6
    private const val TEST_FILE_COUNT_INDEX = 7
    private const val RESOURCE_FILE_COUNT_INDEX = 8
    private const val DEPENDENCIES_INDEX = 9
}
