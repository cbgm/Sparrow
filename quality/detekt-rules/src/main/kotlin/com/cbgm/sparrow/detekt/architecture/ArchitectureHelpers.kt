package com.cbgm.sparrow.detekt.architecture

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference

internal fun KtClass.isViewModel(): Boolean {
    return name?.endsWith("ViewModel") == true ||
        superTypeListEntries.any { entry ->
            entry.text
                .substringBefore('<')
                .substringAfterLast('.')
                .trim() == "ViewModel"
        }
}

internal fun KtClass.isUseCase(): Boolean {
    val className = name.orEmpty()

    return className.endsWith("UseCase") ||
        className.matches(
            Regex(
                pattern = "^(Get|Observe|Create|Update|Delete|Send|Import|" +
                    "Load|Save|Verify|Generate|Retry|Clear|Start|Stop).+",
            ),
        ) &&
        containingKtFile.packageName().isDomainPackage()
}

internal fun KtClass.isRepositoryImplementation(): Boolean {
    val className = name.orEmpty()
    val packageName = containingKtFile.packageName()

    return packageName.isDataPackage() &&
        (
            className.endsWith("Repository") ||
                className.endsWith("RepositoryImpl") ||
                className.startsWith("Default") &&
                className.contains("Repository")
            )
}

internal fun KtParameter.typeName(): String? {
    return typeReference?.simpleTypeName()
}

internal fun KtTypeReference.simpleTypeName(): String {
    return text
        .substringAfterLast('.')
        .substringBefore('<')
        .removeSuffix("?")
        .trim()
}

internal fun KtFile.packageName(): String {
    return packageFqName.asString()
}

internal fun String.isPresentationPackage(): Boolean {
    return containsPackageSegment("presentation")
}

internal fun String.isDomainPackage(): Boolean {
    return containsPackageSegment("domain")
}

internal fun String.isDataPackage(): Boolean {
    return containsPackageSegment("data")
}

internal fun String.isRepositoryPackage(): Boolean {
    return containsPackageSegment("repository")
}

internal fun String.isDatabasePackage(): Boolean {
    return containsPackageSegment("database") ||
        containsPackageSegment("dao")
}

private fun String.containsPackageSegment(
    segment: String,
): Boolean {
    return this == segment ||
        startsWith("$segment.") ||
        endsWith(".$segment") ||
        contains(".$segment.")
}
