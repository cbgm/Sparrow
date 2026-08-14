package com.cbgm.sparrow.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtImportDirective

class NoPlatformImportInCommonMainRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents platform-specific imports from being used in commonMain.",
) {

    private val forbiddenPrefixes: List<String> by config(
        defaultValue = listOf(
            "android.",
            "androidx.activity.",
            "androidx.camera.",
            "androidx.core.content.",
            "androidx.fragment.",
            "java.",
            "javax.",
            "kotlin.jvm.",
        ),
    )

    override fun visitImportDirective(
        importDirective: KtImportDirective,
    ) {
        super.visitImportDirective(importDirective)

        if (!importDirective.isInsideCommonMain()) {
            return
        }

        val importedPath =
            importDirective.importPath?.pathStr
                ?: return

        val forbiddenPrefix =
            forbiddenPrefixes.firstOrNull { prefix ->
                importedPath.startsWith(prefix)
            } ?: return

        report(
            Finding(
                entity = Entity.from(importDirective),
                message = buildString {
                    append("Platform-specific import '")
                    append(importedPath)
                    append("' is not allowed in commonMain. ")
                    append("Matched forbidden prefix: ")
                    append(forbiddenPrefix)
                },
            ),
        )
    }
}

private fun KtImportDirective.isInsideCommonMain(): Boolean {
    val normalizedPath = containingKtFile
        .virtualFilePath
        .replace('\\', '/')

    return normalizedPath.contains("/src/commonMain/")
}
