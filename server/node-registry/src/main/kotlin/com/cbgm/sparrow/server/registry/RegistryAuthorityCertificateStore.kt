package com.cbgm.sparrow.server.registry

import com.cbgm.sparrow.server.protocol.RegistryAuthorityCertificate
import com.cbgm.sparrow.server.protocol.serverJson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class RegistryAuthorityCertificateStore(
    private val path: Path
) {
    fun load(): RegistryAuthorityCertificate =
        serverJson.decodeFromString(Files.readString(path))

    fun save(certificate: RegistryAuthorityCertificate) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(
            path,
            serverJson.encodeToString(certificate),
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        )
    }
}
