package com.cbgm.sparrow.feature.identity.domain.error

import com.cbgm.sparrow.core.util.result.ExpectedFailure

/** An invitation that was not answered within its lifetime; it is closed and never completed. */
class InvitationExpiredException :
    IllegalStateException("Invitation has expired"),
    ExpectedFailure
