package com.cbgm.sparrow.feature.onboarding.presentation.tour

import com.cbgm.sparrow.core.ui.component.DeliveryDotsState

/** The icons of the sample chat list's top bar. */
internal enum class TopBarIcon { SHIELD, SEARCH, MAILBOX }

/** What was tried on the sample screens of the current step; done moves the tour on by itself. */
internal data class TourTryState(
    val tappedIcons: List<TopBarIcon> = emptyList(),
    /** The dots of the message sent from the sample field; null while it is still a draft. */
    val sentDots: DeliveryDotsState? = null,
    val isMenuOpen: Boolean = false,
    val reaction: String? = null,
    val isDone: Boolean = false
)

internal sealed interface TourTry {
    data class IconTapped(
        val icon: TopBarIcon
    ) : TourTry

    data object DraftSent : TourTry

    data class DotsAdvanced(
        val dots: DeliveryDotsState
    ) : TourTry

    data object MessageHeld : TourTry

    data class Reacted(
        val emoji: String
    ) : TourTry

    data object MenuDismissed : TourTry

    data object DayPicked : TourTry
}

internal fun TourTryState.after(tried: TourTry): TourTryState =
    when (tried) {
        is TourTry.IconTapped -> {
            val tapped = (tappedIcons - tried.icon) + tried.icon
            copy(tappedIcons = tapped, isDone = tapped.size == TopBarIcon.entries.size)
        }

        TourTry.DraftSent -> copy(sentDots = DeliveryDotsState.QUEUED)
        is TourTry.DotsAdvanced -> copy(sentDots = tried.dots, isDone = tried.dots == DeliveryDotsState.READ)
        TourTry.MessageHeld -> copy(isMenuOpen = true)
        is TourTry.Reacted -> copy(isMenuOpen = false, reaction = tried.emoji, isDone = true)
        TourTry.MenuDismissed -> copy(isMenuOpen = false)
        TourTry.DayPicked -> copy(isDone = true)
    }
