package com.cbgm.sparrow.feature.identity.domain.model

/** The backup belongs to a different identity than the one stored on this device. */
class IdentityBackupMismatchException : IllegalStateException("The backup belongs to a different identity")
