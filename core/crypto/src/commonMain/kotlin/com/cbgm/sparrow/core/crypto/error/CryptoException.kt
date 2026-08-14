package com.cbgm.sparrow.core.crypto.error

sealed class CryptoException(
    message: String,
    cause: Throwable? = null
) : Exception(
        message,
        cause
    )

class CryptoNotInitializedException :
    CryptoException(
        message = "Cryptographic runtime is not initialized"
    )

class InvalidPublicKeyException(
    message: String = "Public key is invalid"
) : CryptoException(
        message = message
    )

class InvalidPrivateKeyException(
    message: String = "Private key is invalid"
) : CryptoException(
        message = message
    )

class MessageEncryptionException(
    cause: Throwable? = null
) : CryptoException(
        message = "Message encryption failed",
        cause = cause
    )

class MessageDecryptionException(
    cause: Throwable? = null
) : CryptoException(
        message = "Message decryption failed",
        cause = cause
    )

class UnsupportedCryptoVersionException(
    version: Int
) : CryptoException(
        message = "Unsupported crypto payload version: $version"
    )
