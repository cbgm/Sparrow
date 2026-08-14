package com.cbgm.sparrow.detekt

import com.cbgm.sparrow.detekt.rule.DaoUsageRule
import com.cbgm.sparrow.detekt.rule.LayerDependencyRule
import com.cbgm.sparrow.detekt.rule.NoNotNullAssertionRule
import com.cbgm.sparrow.detekt.rule.NoPlatformImportInCommonMainRule
import com.cbgm.sparrow.detekt.rule.NoTestImportInProductionRule
import com.cbgm.sparrow.detekt.rule.RepositoryDependencyRule
import com.cbgm.sparrow.detekt.rule.UseCaseDependencyRule
import com.cbgm.sparrow.detekt.rule.ViewModelDirectDataDependencyRule
import com.cbgm.sparrow.detekt.rule.WeakHashAlgorithmRule
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class SparrowRuleSetProvider : RuleSetProvider {

    override val ruleSetId =
        RuleSetId("Sparrow")

    override fun instance(): RuleSet {
        return RuleSet(
            id = ruleSetId,
            rules = mapOf(
                RuleName("NoPlatformImportInCommonMainRule") to
                    ::NoPlatformImportInCommonMainRule,

                RuleName("NoTestImportInProductionRule") to
                    ::NoTestImportInProductionRule,

                RuleName("NoNotNullAssertionRule") to
                    ::NoNotNullAssertionRule,

                RuleName("LayerDependencyRule") to
                    ::LayerDependencyRule,

                RuleName("ViewModelDirectDataDependencyRule") to
                    ::ViewModelDirectDataDependencyRule,

                RuleName("UseCaseDependencyRule") to
                    ::UseCaseDependencyRule,

                RuleName("RepositoryDependencyRule") to
                    ::RepositoryDependencyRule,

                RuleName("DaoUsageRule") to
                    ::DaoUsageRule,

                RuleName("WeakHashAlgorithmRule") to
                    ::WeakHashAlgorithmRule,
            ),
        )
    }
}
