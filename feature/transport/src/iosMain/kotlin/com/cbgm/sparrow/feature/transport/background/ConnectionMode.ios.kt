package com.cbgm.sparrow.feature.transport.background

/** iOS does not let an app keep a connection open in the background, and Sparrow uses no Apple push. */
actual fun keepsConnectionWhileClosed(): Boolean = false
