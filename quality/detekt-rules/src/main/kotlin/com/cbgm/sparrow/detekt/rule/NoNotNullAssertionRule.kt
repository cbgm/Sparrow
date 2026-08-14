package com.cbgm.sparrow.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression

class NoNotNullAssertionRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents use of Kotlin's non-null assertion operator.",
) {

    override fun visitPostfixExpression(
        expression: KtPostfixExpression,
    ) {
        super.visitPostfixExpression(expression)

        if (expression.operationToken != KtTokens.EXCLEXCL) {
            return
        }

        report(
            Finding(
                entity = Entity.from(expression),
                message =
                    "Do not use '!!'. Handle null explicitly or use requireNotNull/checkNotNull.",
            ),
        )
    }
}
