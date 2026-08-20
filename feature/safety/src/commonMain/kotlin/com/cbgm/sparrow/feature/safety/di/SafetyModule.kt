package com.cbgm.sparrow.feature.safety.di

import com.cbgm.sparrow.feature.safety.domain.rule.MessageSafetyRuleEngine
import com.cbgm.sparrow.feature.safety.domain.usecase.AnalyzeMessageSafetyUseCase
import org.koin.dsl.module

val safetyModule =
    module {
        single { MessageSafetyRuleEngine() }
        factory { AnalyzeMessageSafetyUseCase(ruleEngine = get()) }
    }
