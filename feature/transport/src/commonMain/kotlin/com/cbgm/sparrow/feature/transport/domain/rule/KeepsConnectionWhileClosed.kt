package com.cbgm.sparrow.feature.transport.domain.rule

/** Whether this platform lets an app keep its own connection while it is closed: Android yes, iOS no. */
expect fun keepsConnectionWhileClosed(): Boolean
