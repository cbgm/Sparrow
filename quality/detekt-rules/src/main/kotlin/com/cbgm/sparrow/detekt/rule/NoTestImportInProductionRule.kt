package com.cbgm.sparrow.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtImportDirective

class NoTestImportInProductionRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents testing libraries from being imported by production source sets.",
) {

    private val forbiddenPrefixes: List<String> by config(
        defaultValue = listOf(
            "org.junit.",
            "kotlin.test.",
            "io.mockk.",
            "org.mockito.",
            "com.google.common.truth.",
            "app.cash.turbine.",
        ),
    )

    override fun visitImportDirective(
        importDirective: KtImportDirective,
    ) {
        super.visitImportDirective(importDirective)

        if (!importDirective.isInsideProductionSourceSet()) {
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
                    append("Testing import '")
                    append(importedPath)
                    append("' is not allowed in production code. ")
                    append("Matched forbidden prefix: ")
                    append(forbiddenPrefix)
                },
            ),
        )
    }
}

private fun KtImportDirective.isInsideProductionSourceSet(): Boolean {
    val path = containingKtFile
        .virtualFilePath
        .replace('\\', '/')

    val isSourceFile =
        path.contains("/src/")

    val isTestSource = listOf(
        "/commonTest/",
        "/androidHostTest/",
        "/androidDeviceTest/",
        "/jvmTest/",
        "/iosTest/",
        "/test/",
        "/androidTest/",
    ).any(path::contains)

    return isSourceFile && !isTestSource
}
