package com.cbgm.sparrow.feature.expenses.presentation.create.kmapper

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toAvatarTarget
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpenseParticipantAvatarMapperTest {
    @Test
    fun localUserUsesExistingAvatarTarget() {
        assertEquals(AvatarTarget.LocalUser, ExpenseParticipantUi("a", "Me", isLocal = true).toAvatarTarget())
    }

    @Test
    fun knownContactUsesExistingContactAvatar() {
        assertEquals(
            AvatarTarget.User("contact-local-id"),
            ExpenseParticipantUi("public-signing-key", "Alex", avatarContactId = "contact-local-id").toAvatarTarget()
        )
    }

    @Test
    fun unlinkedPersonUsesSparrowAvatarInitialsFallback() {
        assertNull(ExpenseParticipantUi("public-signing-key", "Alex").toAvatarTarget())
    }
}
