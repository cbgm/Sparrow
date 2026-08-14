package com.cbgm.sparrow.server.security

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.util.Base64
import java.util.Properties

class NodeIdentityStore(
    private val path: Path
) {
    fun loadOrCreate(): NodeIdentity {
        if (Files.exists(path)) {
            return loadExisting()
        }

        val identity = NodeIdentity.generate()
        return runCatching {
            save(identity)
            identity
        }.getOrElse {
            if (Files.exists(path)) loadExisting() else throw it
        }
    }

    fun loadExisting(): NodeIdentity {
        val properties = Properties()
        Files.newInputStream(path).use(properties::load)

        return NodeIdentity.decode(
            publicKey = requireNotNull(properties.getProperty(PUBLIC_KEY)),
            privateKey = requireNotNull(properties.getProperty(PRIVATE_KEY))
        )
    }

    private fun save(identity: NodeIdentity) {
        path.parent?.let(Files::createDirectories)

        val properties =
            Properties().apply {
                setProperty(PUBLIC_KEY, Base64.getEncoder().encodeToString(identity.publicKey.encoded))
                setProperty(PRIVATE_KEY, Base64.getEncoder().encodeToString(identity.privateKey.encoded))
            }

        Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { output ->
            properties.store(output, "Sparrow node identity - keep private")
        }

        runCatching {
            Files.setPosixFilePermissions(
                path,
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            )
        }
    }

    private companion object {
        const val PUBLIC_KEY = "publicKey"
        const val PRIVATE_KEY = "privateKey"
    }
}
