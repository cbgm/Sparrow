package com.cbgm.sparrow.core.crypto.error

class SignatureVerificationException(
    cause: Throwable? = null
) : CryptoException(
        message = "Signature verification failed",
        cause = cause
    )
