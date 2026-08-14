package com.cbgm.sparrow.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class WeakHashAlgorithmRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents known weak hash algorithms from being selected.",
) {

    private val forbiddenAlgorithms: Set<String> by config(
        defaultValue = setOf(
            "MD2",
            "MD4",
            "MD5",
            "SHA",
            "SHA1",
            "SHA-1",
        ),
    )

    override fun visitCallExpression(
        expression: KtCallExpression,
    ) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != "getInstance") {
            return
        }

        expression.valueArguments.forEach { argument ->
            val stringExpression =
                argument.getArgumentExpression()
                    as? KtStringTemplateExpression
                    ?: return@forEach

            val algorithm = stringExpression
                .entries
                .joinToString(separator = "") { entry ->
                    entry.text
                }
                .trim()
                .uppercase()

            if (algorithm !in forbiddenAlgorithms) {
                return@forEach
            }

            report(
                Finding(
                    entity = Entity.from(argument),
                    message =
                        "Weak hash algorithm '$algorithm' must not be used.",
                ),
            )
        }
    }
}
