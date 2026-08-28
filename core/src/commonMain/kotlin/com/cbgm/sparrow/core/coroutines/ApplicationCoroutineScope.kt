package com.cbgm.sparrow.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class ApplicationCoroutineScope(
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) : CoroutineScope
