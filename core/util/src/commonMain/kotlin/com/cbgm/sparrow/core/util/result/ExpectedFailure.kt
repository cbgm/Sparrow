package com.cbgm.sparrow.core.util.result

/**
 * Marks an exception that is an expected outcome rather than a fault, such as an expired invitation or a blocked
 * contact. It is still returned as a failure, but never logged as an error or shown in the error snackbar.
 */
interface ExpectedFailure
