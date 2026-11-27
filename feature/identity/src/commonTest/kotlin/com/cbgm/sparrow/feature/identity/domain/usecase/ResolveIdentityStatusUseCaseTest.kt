package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityResolution
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.protocol.identity.LocalIdentityChangeHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResolveIdentityStatusUseCaseTest {
    private val repository = FakeIdentityRepository()
    private val handler = FakeChangeHandler()
    private val useCase =
        ResolveIdentityStatusUseCase(
            identityRepository = repository,
            localIdentityChangeHandler = handler,
            recoverIncompleteIdentity = RecoverIncompleteIdentityUseCase(repository, handler),
            retryDelayMilliseconds = 0L
        )

    @Test
    fun aBriefReadErrorIsTriedAgainAndDeletesNothing() =
        runTest {
            repository.statuses += listOf(Result.failure(IllegalStateException("busy")), Result.success(IdentityStatus.READY))

            assertEquals(IdentityResolution.READY, useCase().getOrThrow())
            assertFalse(handler.wiped)
        }

    @Test
    fun aLastingReadErrorFailsStartupAndDeletesNothing() =
        runTest {
            repeat(3) { repository.statuses += Result.failure(IllegalStateException("busy")) }

            assertTrue(useCase().isFailure)
            assertFalse(handler.wiped)
            assertFalse(repository.reset)
        }

    @Test
    fun unusableKeysWithChatsAreLeftToTheUser() =
        runTest {
            repository.statuses += Result.success(IdentityStatus.INCOMPLETE)
            handler.conversations = true

            assertEquals(IdentityResolution.KEYS_UNUSABLE, useCase().getOrThrow())
            assertFalse(handler.wiped)
        }

    @Test
    fun anInterruptedSetupWithoutChatsIsResetSilently() =
        runTest {
            repeat(2) { repository.statuses += Result.success(IdentityStatus.INCOMPLETE) }
            handler.conversations = false

            assertEquals(IdentityResolution.NOT_CREATED, useCase().getOrThrow())
            assertTrue(handler.wiped)
            assertTrue(repository.reset)
        }

    private class FakeChangeHandler : LocalIdentityChangeHandler {
        var conversations = false
        var wiped = false

        override suspend fun onLocalIdentityChanged(): Result<Unit> = Result.success(Unit).also { wiped = true }

        override suspend fun hasConversations(): Result<Boolean> = Result.success(conversations)
    }

    private class FakeIdentityRepository : IdentityRepository {
        val statuses = ArrayDeque<Result<IdentityStatus>>()
        var reset = false

        override suspend fun getStatus(): Result<IdentityStatus> = statuses.removeFirst()

        override suspend fun resetIdentity(): Result<Unit> = Result.success(Unit).also { reset = true }

        override fun observeIdentity(): Flow<PublicIdentity?> = error("Not used")

        override fun observeKeysUsable(): Flow<Boolean> = error("Not used")

        override suspend fun hasIdentity(): Result<Boolean> = error("Not used")

        override suspend fun createIdentity(): Result<PublicIdentity> = error("Not used")

        override suspend fun validateIdentityKeys(
            publicIdentity: PublicIdentity,
            encryptionPrivateKey: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<Unit> = error("Not used")

        override suspend fun restoreIdentity(
            publicIdentity: PublicIdentity,
            encryptionPrivateKey: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<PublicIdentity> = error("Not used")

        override suspend fun repairIdentityKeys(
            publicIdentity: PublicIdentity,
            encryptionPrivateKey: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<PublicIdentity> = error("Not used")

        override suspend fun discardKeyStoreKey(): Result<Unit> = error("Not used")

        override suspend fun getIdentity(): Result<PublicIdentity?> = error("Not used")

        override suspend fun getEncryptionPrivateKey(): Result<ByteArray> = error("Not used")

        override suspend fun getSigningPrivateKey(): Result<ByteArray> = error("Not used")
    }
}
