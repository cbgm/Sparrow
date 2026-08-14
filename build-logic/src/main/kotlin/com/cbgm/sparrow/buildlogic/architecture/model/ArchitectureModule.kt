package com.cbgm.sparrow.buildlogic.architecture.model

data class ArchitectureModule(
    val name: String,
    val path: String,
    val group: String,
    val directory: String,
    val buildFile: String,
    val sourceSets: Set<String>,
    val kotlinSourceFileCount: Int,
    val testKotlinFileCount: Int,
    val resourceFileCount: Int,
    val dependencies: Set<String>,
) {

    init {
        require(name.isNotBlank()) {
            "Architecture module name must not be blank."
        }

        require(path.startsWith(":")) {
            "Architecture module path must start with ':': $path"
        }

        require(group.isNotBlank()) {
            "Architecture module group must not be blank: $path"
        }

        require(directory.isNotBlank()) {
            "Architecture module directory must not be blank: $path"
        }

        require(buildFile.isNotBlank()) {
            "Architecture module build file must not be blank: $path"
        }

        require(kotlinSourceFileCount >= 0) {
            "Kotlin source file count must not be negative: $path"
        }

        require(testKotlinFileCount >= 0) {
            "Test Kotlin file count must not be negative: $path"
        }

        require(resourceFileCount >= 0) {
            "Resource file count must not be negative: $path"
        }

        require(path !in dependencies) {
            "Architecture module must not depend on itself: $path"
        }
    }

    val productionKotlinFileCount: Int
        get() = kotlinSourceFileCount - testKotlinFileCount
}
