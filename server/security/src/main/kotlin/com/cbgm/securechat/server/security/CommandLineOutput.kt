package com.cbgm.securechat.server.security

internal object CommandLineOutput {
    fun write(lines: List<String>) {
        val output =
            lines.joinToString(
                separator = System.lineSeparator(),
                postfix = System.lineSeparator()
            )
        System.out.write(output.encodeToByteArray())
        System.out.flush()
    }
}
