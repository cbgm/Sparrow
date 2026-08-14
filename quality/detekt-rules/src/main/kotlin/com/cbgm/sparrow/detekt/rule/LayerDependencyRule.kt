package com.cbgm.sparrow.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtImportDirective

class LayerDependencyRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Prevents invalid dependencies between presentation, domain, and data layers.",
) {

    override fun visitImportDirective(
        importDirective: KtImportDirective,
    ) {
        super.visitImportDirective(importDirective)

        val sourcePackage =
            importDirective.containingKtFile.packageFqName.asString()

        val importedPackage =
            importDirective.importPath?.pathStr
                ?: return

        val violation = when {
            sourcePackage.isPresentationPackage() &&
                importedPackage.isDataPackage() -> {
                "Presentation code must not import data-layer code."
            }

            sourcePackage.isDomainPackage() &&
                importedPackage.isDataPackage() -> {
                "Domain code must not import data-layer code."
            }

            sourcePackage.isDomainPackage() &&
                importedPackage.isPresentationPackage() -> {
                "Domain code must not import presentation-layer code."
            }

            sourcePackage.isDataPackage() &&
                importedPackage.isPresentationPackage() -> {
                "Data code must not import presentation-layer code."
            }

            else -> null
        }

        if (violation == null) {
            return
        }

        report(
            Finding(
                entity = Entity.from(importDirective),
                message = buildString {
                    append(violation)
                    append(" Invalid import: ")
                    append(importedPackage)
                },
            ),
        )
    }
}

private fun String.isPresentationPackage(): Boolean {
    return contains(".presentation.") ||
        endsWith(".presentation")
}

private fun String.isDomainPackage(): Boolean {
    return contains(".domain.") ||
        endsWith(".domain")
}

private fun String.isDataPackage(): Boolean {
    return contains(".data.") ||
        endsWith(".data")
}
