package com.cbgm.sparrow.feature.safety.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.ui.navigation.AppNavigator
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningLevel
import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningReason
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MessageSafetyDetailsRoute(
    levelId: String,
    reasonIds: String,
    focusReasonId: String?,
    contactId: String?,
    navigator: AppNavigator = koinInject(),
    blockContact: BlockContactUseCase = koinInject(),
    blocklistRepository: ContactBlocklistRepository = koinInject()
) {
    val level =
        MessageSafetyWarningLevel.fromId(levelId)
            ?: MessageSafetyWarningLevel.SUSPICIOUS
    val reasons =
        reasonIds
            .split(',')
            .mapNotNull { reasonId -> MessageSafetyWarningReason.fromId(reasonId) }
            .distinct()
    val focusReason = focusReasonId?.let(MessageSafetyWarningReason::fromId)
    val blockedContactIds by
        remember(blocklistRepository) {
            blocklistRepository.observeBlockedContactIds()
        }.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    var isBlockingUser by remember { mutableStateOf(false) }
    var blockError by remember { mutableStateOf<String?>(null) }
    val blockableContactId = contactId?.takeIf(String::isNotBlank)

    MessageSafetyDetailsScreen(
        level = level,
        reasons = reasons,
        focusReason = focusReason,
        canBlockUser = blockableContactId != null,
        isUserBlocked = blockableContactId != null && blockableContactId in blockedContactIds,
        isBlockingUser = isBlockingUser,
        blockError = blockError,
        onBackClick = navigator::popBackStack,
        onBlockUserClick = {
            val id = blockableContactId
            if (id != null && !isBlockingUser && id !in blockedContactIds) {
                scope.launch {
                    isBlockingUser = true
                    blockError = null
                    blockContact(id)
                        .onFailure { error ->
                            blockError = error.message
                        }
                    isBlockingUser = false
                }
            }
        }
    )
}
