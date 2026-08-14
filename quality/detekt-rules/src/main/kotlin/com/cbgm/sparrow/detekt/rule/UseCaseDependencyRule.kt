package com.cbgm.sparrow.detekt.rule

import com.cbgm.sparrow.detekt.architecture.isUseCase
import com.cbgm.sparrow.detekt.architecture.typeName
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter

class UseCaseDependencyRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents use cases from directly depending on infrastructure types.",
) {

    private val forbiddenTypeSuffixes: Set<String> by config(
        defaultValue = setOf(
            "Dao",
            "Database",
            "DataSource",
            "HttpClient",
            "WebSocketClient",
            "TransportClient",
            "Api",
        ),
    )

    override fun visitClass(
        classDeclaration: KtClass,
    ) {
        super.visitClass(classDeclaration)

        if (!classDeclaration.isUseCase()) {
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
                    append("Use case must not depend directly on ")
                    append(typeName)
                    append(". Depend on a domain repository or abstraction. ")
                    append("Matched suffix: ")
                    append(forbiddenSuffix)
                },
            ),
        )
    }
}
