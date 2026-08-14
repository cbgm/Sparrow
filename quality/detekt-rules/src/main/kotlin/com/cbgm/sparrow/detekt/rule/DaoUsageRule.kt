package com.cbgm.sparrow.detekt.rule

import com.cbgm.sparrow.detekt.architecture.isDataPackage
import com.cbgm.sparrow.detekt.architecture.isDatabasePackage
import com.cbgm.sparrow.detekt.architecture.isRepositoryPackage
import com.cbgm.sparrow.detekt.architecture.packageName
import com.cbgm.sparrow.detekt.architecture.typeName
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter

class DaoUsageRule(
    config: Config,
) : Rule(
    config = config,
    description =
        "Restricts DAO dependencies to database and data repository code.",
) {

    override fun visitClass(
        classDeclaration: KtClass,
    ) {
        super.visitClass(classDeclaration)

        val daoParameters =
            classDeclaration.primaryConstructorParameters
                .filter(KtParameter::isDaoParameter)

        if (daoParameters.isEmpty()) {
            return
        }

        val packageName =
            classDeclaration.containingKtFile.packageName()

        if (packageName.mayUseDao()) {
            return
        }

        daoParameters.forEach { parameter ->
            report(
                Finding(
                    entity = Entity.from(parameter),
                    message = buildString {
                        append("DAO dependency ")
                        append(parameter.typeName())
                        append(" is not allowed in package ")
                        append(packageName)
                        append(". DAOs may only be used by database or data repository code.")
                    },
                ),
            )
        }
    }
}

private fun KtParameter.isDaoParameter(): Boolean {
    return typeName()?.endsWith("Dao") == true
}

private fun String.mayUseDao(): Boolean {
    return isDatabasePackage() ||
        (
            isDataPackage() &&
                isRepositoryPackage()
            )
}
