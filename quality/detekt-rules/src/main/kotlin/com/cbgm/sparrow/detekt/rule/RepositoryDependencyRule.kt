package com.cbgm.sparrow.detekt.rule

import com.cbgm.sparrow.detekt.architecture.isRepositoryImplementation
import com.cbgm.sparrow.detekt.architecture.typeName
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter

class RepositoryDependencyRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents repository implementations from depending on presentation or use-case types.",
) {

    private val forbiddenTypeSuffixes: Set<String> by config(
        defaultValue = setOf(
            "ViewModel",
            "Screen",
            "Route",
            "UiState",
            "UseCase",
        ),
    )

    override fun visitClass(
        classDeclaration: KtClass,
    ) {
        super.visitClass(classDeclaration)

        if (!classDeclaration.isRepositoryImplementation()) {
            return
        }

        classDeclaration.primaryConstructorParameters
            .forEach(::checkParameter)
    }

    private fun checkParameter(
        parameter: KtParameter,
    ) {
        val typeName = parameter.typeName()
            ?: return

        val forbiddenSuffix =
            forbiddenTypeSuffixes.firstOrNull { suffix ->
                typeName.endsWith(suffix)
            } ?: return

        report(
            Finding(
                entity = Entity.from(parameter),
                message = buildString {
                    append("Repository implementation must not depend on ")
                    append(typeName)
                    append(". Repositories must remain below domain and presentation. ")
                    append("Matched suffix: ")
                    append(forbiddenSuffix)
                },
            ),
        )
    }
}
