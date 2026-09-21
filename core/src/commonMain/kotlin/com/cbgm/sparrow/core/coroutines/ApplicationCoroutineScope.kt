package com.cbgm.sparrow.core.coroutines

import com.cbgm.sparrow.core.logging.SparrowLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class ApplicationCoroutineScope(
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, failure ->
            SparrowLog.error("ApplicationCoroutineScope", "Unhandled application coroutine failure", failure)
        }
) : CoroutineScope
