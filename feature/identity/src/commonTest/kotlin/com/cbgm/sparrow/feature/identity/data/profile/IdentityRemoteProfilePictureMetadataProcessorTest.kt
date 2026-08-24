package com.cbgm.sparrow.feature.identity.data.profile

import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.ProfilePicturePayload
import com.cbgm.sparrow.feature.identity.data.datasource.profile.IdentityRemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IdentityRemoteProfilePictureMetadataProcessorTest {
    @Test
    fun newerPictureWithBytesReplacesStoredPicture() = runTest {
        val repository = FakeRemoteProfilePictureRepository()
        val processor = IdentityRemoteProfilePictureMetadataProcessor(repository)

        processor
            .apply(
                contactId = CONTACT_ID,
                metadata =
                    ProfilePictureMetadata(
                        changedAtEpochMilliseconds = 20L,
                        hasPicture = true,
                        payload = ProfilePicturePayload(byteArrayOf(2, 3))
                    )
            ).getOrThrow()

        val stored = repository.get(CONTACT_ID).getOrThrow()
        assertEquals(20L, stored.changedAtEpochMilliseconds)
        assertContentEquals(byteArrayOf(2, 3), stored.bytes)
    }

    @Test
    fun newerMetadataWithoutBytesDoesNotConsumeVersion() = runTest {
        val repository =
            FakeRemoteProfilePictureRepository(
                RemoteProfilePicture(
                    contactId = CONTACT_ID,
                    changedAtEpochMilliseconds = 10L,
                    bytes = byteArrayOf(1)
                )
            )
        val processor = IdentityRemoteProfilePictureMetadataProcessor(repository)

        processor
            .apply(
                contactId = CONTACT_ID,
                metadata =
                    ProfilePictureMetadata(
                        changedAtEpochMilliseconds = 20L,
                        hasPicture = true,
                        payload = null
                    )
            ).getOrThrow()

        assertEquals(10L, repository.get(CONTACT_ID).getOrThrow().changedAtEpochMilliseconds)

        processor
            .apply(
                contactId = CONTACT_ID,
                metadata =
                    ProfilePictureMetadata(
                        changedAtEpochMilliseconds = 20L,
                        hasPicture = true,
                        payload = ProfilePicturePayload(byteArrayOf(9))
                    )
            ).getOrThrow()

        val stored = repository.get(CONTACT_ID).getOrThrow()
        assertEquals(20L, stored.changedAtEpochMilliseconds)
        assertContentEquals(byteArrayOf(9), stored.bytes)
    }

    @Test
    fun newerRemovalDeletesStoredPicture() = runTest {
        val repository =
            FakeRemoteProfilePictureRepository(
                RemoteProfilePicture(
                    contactId = CONTACT_ID,
                    changedAtEpochMilliseconds = 10L,
                    bytes = byteArrayOf(1)
                )
            )
        val processor = IdentityRemoteProfilePictureMetadataProcessor(repository)

        processor
            .apply(
                contactId = CONTACT_ID,
                metadata =
                    ProfilePictureMetadata(
                        changedAtEpochMilliseconds = 30L,
                        hasPicture = false
                    )
            ).getOrThrow()

        val stored = repository.get(CONTACT_ID).getOrThrow()
        assertEquals(30L, stored.changedAtEpochMilliseconds)
        assertNull(stored.bytes)
    }

    private class FakeRemoteProfilePictureRepository(
        initial: RemoteProfilePicture = RemoteProfilePicture(contactId = CONTACT_ID)
    ) : RemoteProfilePictureRepository {
        private val state = MutableStateFlow(initial)

        override fun observe(contactId: String): Flow<RemoteProfilePicture> = state

        override suspend fun get(contactId: String): Result<RemoteProfilePicture> = Result.success(state.value)

        override suspend fun save(
            contactId: String,
            bytes: ByteArray,
            changedAtEpochMilliseconds: Long
        ): Result<Unit> {
            state.value =
                RemoteProfilePicture(
                    contactId = contactId,
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    bytes = bytes.copyOf()
                )
            return Result.success(Unit)
        }

        override suspend fun remove(
            contactId: String,
            changedAtEpochMilliseconds: Long
        ): Result<Unit> {
            state.value =
                RemoteProfilePicture(
                    contactId = contactId,
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    bytes = null
                )
            return Result.success(Unit)
        }
    }

    private companion object {
        const val CONTACT_ID = "contact-1"
    }
}
