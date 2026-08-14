package com.cbgm.securechat.feature.transport.controlplane

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpControlPlaneDirectorySynchronizerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun plainTextResponseBodyIsDecodedAsJson() {
        val content =
            """
            {
              "controlPlanes": [
                "http://plane-a.example.test",
                "https://plane-b.example.test"
              ]
            }
            """.trimIndent()

        assertEquals(
            listOf(
                "http://plane-a.example.test",
                "https://plane-b.example.test"
            ),
            parseControlPlaneDirectory(content, json)
        )
    }

    @Test
    fun duplicateAndBlankEntriesAreRemoved() {
        val content =
            """
            {
              "controlPlanes": [
                " https://plane.example.test/ ",
                "https://plane.example.test/",
                "   "
              ]
            }
            """.trimIndent()

        assertEquals(
            listOf("https://plane.example.test/"),
            parseControlPlaneDirectory(content, json)
        )
    }

    @Test
    fun emptyDirectoryIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            parseControlPlaneDirectory("""{"controlPlanes": []}""", json)
        }
    }
}
