package com.cbgm.sparrow.buildlogic.architecture.model

data class ArchitectureModel(
    val modules: List<ArchitectureModule>,
) {

    val modulesByPath: Map<String, ArchitectureModule> =
        modules.associateBy(ArchitectureModule::path)

    val groups: Map<String, List<ArchitectureModule>> =
        modules
            .groupBy(ArchitectureModule::group)
            .toSortedMap()

    val dependencies: List<ArchitectureDependency> =
        modules
            .flatMap { module ->
                module.dependencies
                    .filter(modulesByPath::containsKey)
                    .map { dependencyPath ->
                        ArchitectureDependency(
                            source = module.path,
                            target = dependencyPath,
                        )
                    }
            }
            .distinct()
            .sortedWith(
                compareBy(
                    ArchitectureDependency::source,
                    ArchitectureDependency::target,
                ),
            )

    val allSourceSets: Set<String> =
        modules
            .flatMap(ArchitectureModule::sourceSets)
            .toSortedSet()

    val statistics: ArchitectureStatistics =
        ArchitectureStatistics(
            moduleCount = modules.size,
            groupCount = groups.size,
            dependencyCount = dependencies.size,
            moduleWithoutDependenciesCount = modules.count { module ->
                module.dependencies.none(modulesByPath::containsKey)
            },
            moduleWithoutDependentsCount = modules.count { module ->
                dependentsOf(module.path).isEmpty()
            },
            maximumDirectDependencyCount = modules.maxOfOrNull { module ->
                module.dependencies.count(modulesByPath::containsKey)
            } ?: 0,
            maximumDirectDependentCount = modules.maxOfOrNull { module ->
                dependentsOf(module.path).size
            } ?: 0,
            sourceSetCount = allSourceSets.size,
            kotlinSourceFileCount = modules.sumOf(
                ArchitectureModule::kotlinSourceFileCount,
            ),
            productionKotlinFileCount = modules.sumOf(
                ArchitectureModule::productionKotlinFileCount,
            ),
            testKotlinFileCount = modules.sumOf(
                ArchitectureModule::testKotlinFileCount,
            ),
            resourceFileCount = modules.sumOf(
                ArchitectureModule::resourceFileCount,
            ),
        )

    fun dependenciesOf(
        modulePath: String,
    ): List<String> {
        return dependencies
            .asSequence()
            .filter { dependency ->
                dependency.source == modulePath
            }
            .map(ArchitectureDependency::target)
            .sorted()
            .toList()
    }

    fun dependentsOf(
        modulePath: String,
    ): List<String> {
        return dependencies
            .asSequence()
            .filter { dependency ->
                dependency.target == modulePath
            }
            .map(ArchitectureDependency::source)
            .sorted()
            .toList()
    }
}

data class ArchitectureDependency(
    val source: String,
    val target: String,
)

data class ArchitectureStatistics(
    val moduleCount: Int,
    val groupCount: Int,
    val dependencyCount: Int,
    val moduleWithoutDependenciesCount: Int,
    val moduleWithoutDependentsCount: Int,
    val maximumDirectDependencyCount: Int,
    val maximumDirectDependentCount: Int,
    val sourceSetCount: Int,
    val kotlinSourceFileCount: Int,
    val productionKotlinFileCount: Int,
    val testKotlinFileCount: Int,
    val resourceFileCount: Int,
)
