package com.cbgm.sparrow.feature.attachments.data.datasource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageAttachmentFileDataSourceTest {
    private val root = File(
        ApplicationProvider.getApplicationContext<Context>().cacheDir,
        "attachment-name-test-${UUID.randomUUID()}"
    )
    private val files = MessageAttachmentFileDataSource(rootDirectory = root.absolutePath)

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    @Test
    fun renameMovesExistingMediaAndRetainsConversationMarker() {
        val oldFile = File(save("peer-1", "Old name", "image-1"))
        assertTrue(oldFile.exists())

        files.updateSavedConversationName("peer-1", "New name")

        assertFalse(oldFile.exists())
        val updatedFile = File(root, "Sparrow/New name/media/image-1.jpg")
        assertTrue(updatedFile.exists())
        assertEquals("peer-1", File(root, "Sparrow/New name/.sparrow-conversation-id").readText())

        // On-demand saving after a rename must reuse the same owner directory.
        assertTrue(File(save("peer-1", "New name", "image-2")).exists())
        files.deleteSavedConversation("peer-1")
        assertFalse(updatedFile.exists())
    }

    @Test
    fun renameNeverOverwritesAnotherConversationsDirectory() {
        val first = File(save("peer-1", "One", "image-1"))
        val second = File(save("peer-2", "Taken", "image-2"))

        files.updateSavedConversationName("peer-1", "Taken")

        assertFalse(first.exists())
        assertTrue(second.exists())
        assertEquals("peer-2", File(root, "Sparrow/Taken/.sparrow-conversation-id").readText())
        val moved = File(root, "Sparrow/Taken-peer1/media/image-1.jpg")
        assertTrue(moved.exists())
        files.deleteSavedAttachment("peer-1", "image-1")
        assertFalse(moved.exists())
        assertTrue(second.exists())
    }

    @Test
    fun renameDoesNotTakeOverUnmarkedUserDirectory() {
        val first = File(save("peer-1", "One", "image-1"))
        val userFolder = File(root, "Sparrow/Existing")
        userFolder.mkdirs()
        File(userFolder, "keep.txt").writeText("keep")

        files.updateSavedConversationName("peer-1", "Existing")

        assertTrue(File(userFolder, "keep.txt").exists())
        assertTrue(File(root, "Sparrow/Existing-peer1/media/image-1.jpg").exists())
        assertFalse(first.exists())
    }

    private fun save(conversationId: String, displayName: String, attachmentId: String): String =
        files.saveForConversation(
            conversationId = conversationId,
            displayName = displayName,
            attachmentId = attachmentId,
            type = MessageAttachmentType.IMAGE,
            mimeType = "image/jpeg",
            bytes = byteArrayOf(1, 2, 3)
        )
}
