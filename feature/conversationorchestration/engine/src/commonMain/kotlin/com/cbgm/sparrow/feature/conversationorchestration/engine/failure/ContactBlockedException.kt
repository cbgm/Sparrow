package com.cbgm.sparrow.feature.conversationorchestration.engine.failure

import com.cbgm.sparrow.core.util.result.ExpectedFailure

/** The user blocked this contact; nothing is sent to it until they unblock it. */
class ContactBlockedException :
    IllegalStateException("Blocked contacts cannot be invited"),
    ExpectedFailure
