package com.cbgm.sparrow.detekt.rule

import com.cbgm.sparrow.detekt.architecture.isViewModel
import com.cbgm.sparrow.detekt.architecture.typeName
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter

class ViewModelDirectDataDependencyRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents ViewModels from directly depending on data-layer types.",
) {

    private val forbiddenTypeSuffixes: Set<String> by config(
        defaultValue = setOf(
            "Repository",
            "RepositoryImpl",
            "Dao",
            "Database",
            "DataSource",
            "HttpClient",
            "WebSocketClient",
            "TransportClient",
            "Api",
            "Service",
        ),
    )

    override fun visitClass(
        classDeclaration: KtClass,
    ) {
        super.visitClass(classDeclaration)

        if (!classDeclaration.isViewModel()) {
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
                    append("ViewModel must not depend directly on ")
                    append(typeName)
                    append(". Use a use case or presentation-facing abstraction. ")
                    append("Matched suffix: ")
                    append(forbiddenSuffix)
                },
            ),
        )
    }
}
