package com.cbgm.sparrow.core.protocol.identity

/** The device has not created or restored its local identity yet. */
class LocalIdentityUnavailableException : IllegalStateException("Local Sparrow identity does not exist")
