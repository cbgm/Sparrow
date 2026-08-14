package com.cbgm.sparrow.feature.chats.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.feature.chats.presentation.create.CreateGroupRoute
import com.cbgm.sparrow.feature.chats.presentation.create.model.ContactsFlowUiEvent
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupEffect
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactsRoute
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsEffect
import org.koin.compose.viewmodel.koinViewModel

private enum class ContactsContent {
    Contacts,
    CreateGroup
}

@Composable
fun ContactsFlow(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsFlowViewModel = koinViewModel()
) {
    var content by rememberSaveable {
        mutableStateOf(ContactsContent.Contacts)
    }

    AnimatedContent(
        targetState = content,
        modifier = modifier,
        transitionSpec = {
            if (targetState == ContactsContent.CreateGroup) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            }
        },
        label = "contacts-content"
    ) { target ->
        when (target) {
            ContactsContent.Contacts -> {
                ContactsRoute(
                    onEffect = { event ->
                        handleContactsEffect(
                            event = event,
                            onDismiss = onDismiss,
                            onCreateGroup = { content = ContactsContent.CreateGroup },
                            onUiEvent = viewModel::onUiEvent
                        )
                    }
                )
            }

            ContactsContent.CreateGroup -> {
                CreateGroupRoute(
                    onEffect = { event ->
                        handleCreateGroupEffect(
                            event = event,
                            onDismiss = onDismiss,
                            onBack = { content = ContactsContent.Contacts },
                            onUiEvent = viewModel::onUiEvent
                        )
                    }
                )
            }
        }
    }
}

private fun handleContactsEffect(
    event: ContactsEffect,
    onDismiss: () -> Unit,
    onCreateGroup: () -> Unit,
    onUiEvent: (ContactsFlowUiEvent) -> Unit
) {
    when (event) {
        ContactsEffect.BackRequested -> onDismiss()
        ContactsEffect.ImportContactRequested -> {
            onDismiss()
            onUiEvent(ContactsFlowUiEvent.ImportContactClicked)
        }
        ContactsEffect.CreateGroupRequested -> onCreateGroup()
        is ContactsEffect.ContactSelected -> {
            onDismiss()
            onUiEvent(
                ContactsFlowUiEvent.ContactSelected(
                    contactId = event.contactId,
                    contactName = event.contactName
                )
            )
        }
        is ContactsEffect.ShowError -> Unit
    }
}

private fun handleCreateGroupEffect(
    event: CreateGroupEffect,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onUiEvent: (ContactsFlowUiEvent) -> Unit
) {
    when (event) {
        CreateGroupEffect.BackRequested -> onBack()
        is CreateGroupEffect.GroupCreated -> {
            onDismiss()
            onUiEvent(ContactsFlowUiEvent.GroupCreated(event.conversationId))
        }
    }
}
