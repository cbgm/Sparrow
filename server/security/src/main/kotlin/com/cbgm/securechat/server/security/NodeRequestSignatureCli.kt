package com.cbgm.securechat.server.security

import java.nio.file.Path

object NodeRequestSignatureCli {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size >= REQUIRED_ARGUMENT_COUNT) {
            "Usage: <identity-path> <method> <path> [body]"
        }
        val identity = NodeIdentityStore(Path.of(arguments[0])).loadOrCreate()
        val authentication =
            NodeRequestSigner(identity).sign(
                method = arguments[1],
                path = arguments[2],
                body = arguments.getOrElse(BODY_ARGUMENT_INDEX) { "" }
            )

        CommandLineOutput.write(
            listOf(
                "nodeId=${authentication.nodeId}",
                "timestamp=${authentication.timestampEpochMilliseconds}",
                "nonce=${authentication.nonce}",
                "signature=${authentication.signature}"
            )
        )
    }

    private const val REQUIRED_ARGUMENT_COUNT = 3
    private const val BODY_ARGUMENT_INDEX = 3
}
