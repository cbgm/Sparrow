package com.cbgm.sparrow.core.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    object Base {
        val zero = 0.dp
        val progressIndicatorStrokeWidth = 2.dp
        val dividerThickness = 0.5.dp
        val dashLength = 5.dp
        val dashGap = 4.dp
        val borderStrokeWidth = 1.dp
        val progressButtonIndicatorSize = 18.dp
    }

    object Poll {
        val previewMediaSelectionSize = 80.dp
        val barHeight = 3.dp
        val checkIconSize = 16.dp
    }

    object Payment {
        val loadingIndicatorSize = 30.dp
    }

    object Expense {
        val numberBadgeSize = 25.dp
        val currencyFieldWidth = 64.dp
        val categoryBadgeSize = 40.dp
        val pinIconTileSize = 36.dp
        val customAmountFieldWidth = 112.dp
        val balanceBarHeight = 4.dp
        val detailIconSize = 64.dp
        val currencyListDialogHeight = 320.dp
    }

    object ActionMenu {
        val shadowElevation = 8.dp
        val actionItemHeight = 36.dp
        val menuWidth = 200.dp
    }

    object SearchField {
        val searchHeight = 44.dp
        val searchIconSize = 18.dp
        val searchClearButtonSize = 32.dp
    }

    object MessageList {
        val loadingStroke = 2.dp
        val loadingSize = 20.dp
    }

    object Avatar {
        val defaultSize = 48.dp
        val sourceActionSize = 22.dp
        val profileSize = 112.dp
        val ringGap = 3.dp
        val ringStroke = 1.2.dp
        val ringDash = 3.dp
    }

    object Badge {
        val iconSize = 12.dp
    }

    object Button {
        val actionPadding = 16.dp
        val linkPadding = 10.dp
        val actionLineWidth = 36.dp
        val actionLinePressedWidth = 52.dp
        val actionLineThickness = 1.dp
        val minTouchHeight = 48.dp
        val height = 44.dp
        val cornerRadius = 14.dp
        val borderWidth = 1.dp
        val iconButtonSize = 44.dp
        val loadingIndicatorSize = 18.dp
        val loadingIndicatorStrokeWidth = 2.dp
    }

    object SectionLabel {
        val letterSpacing = 1.5.sp
    }

    object IdentityOverviewScreen {
        val keyRingsSize = 168.dp
        val keyRingStroke = 1.5.dp
        val keyRingGap = 9.dp
        val pictureSize = 120.dp
    }

    object MediaGrid {
        val gap = 4.dp
    }

    object Tabs {
        val height = 48.dp
        val lineWidth = 16.dp
        val lineHeight = 1.5.dp
        val letterSpacing = 0.8.sp
    }

    object FloatingButton {
        val size = 48.dp
    }

    object Chip {
        val iconSize = 16.dp
    }

    object TopBar {
        val iconSize = 24.dp
    }

    object Banner {
        val iconSize = 18.dp
    }

    object Dialog {
        val padding = 22.dp
        val actionHeight = 52.dp
    }

    object Card {
        val iconSize = 14.dp
        val tonalElevation = 2.dp
        val shadowElevation = 1.dp
    }

    object MessageSafetyWarning {
        val iconSize = 14.dp
    }

    object MessageContext {
        val blurRadius = 6.dp
    }

    object MessageBubble {
        val iconSize = 14.dp
        val progressSize = 12.dp
        val progressStrokeWidth = 1.5.dp
        val retrySize = 28.dp
        val deliveryDotsWidth = 30.dp
        val deliveryDotsHeight = 8.dp
        val deliveryDotRadius = 2.dp
        val deliveryLineWidth = 1.dp
        val retryIconSize = 16.dp
        val staticBubbleSize = 200.dp
    }

    object MessageReaction {
        val cloudIconSlotSize = 20.dp
        val cloudIconStep = 8.dp
        val cloudSideOffsetY = 2.dp
        val cloudContentPadding = 2.dp
        val burstLayerSize = 72.dp
        val burstSpreadRadius = 128.dp

        const val burstItemDurationMillis = 560
        const val burstStaggerWindowMillis = 140
        const val burstStaggerMinMillis = 8
        const val burstStaggerMaxMillis = 24
        const val burstTravelEndFraction = 0.72f
        const val burstFadeStartFraction = 0.58f
        const val burstPeakScaleFraction = 0.52f
        const val burstInitialScale = 0.5f
        const val burstPeakScale = 2.25f
        const val burstEndScale = 0.85f
        const val burstRotationDegrees = 54f
        const val burstRadiusBaseFactor = 0.72f
        const val burstRadiusStepFactor = 0.11f
        const val burstRadiusVariantCount = 4
    }

    object MessageDeletion {
        val maxBlurRadius = 10.dp
    }

    object MessageInput {
        val sendButtonWidth = 42.dp
        val messageFieldHeight = 30.dp
        val messageFieldHeightMax = 300.dp
        val buttonHeight = 29.dp
        val overlap = 10.dp
        val iconSize = 16.dp
        val actionSize = 40.dp
        val actionIconSize = 22.dp
    }

    object MessageAttachment {
        val previewPlayIconSize = 16.dp
        val previewSize = 60.dp
        val filePreviewIconSize = 24.dp
        val loadingIndicatorSize = 24.dp
    }

    object MediaSelection {
        val previewPlayIconSize = 16.dp
        val previewRemoveButtonSize = 20.dp
        val previewRemoveIconSize = 12.dp
        val removeBadgeOverhang = 7.dp
        val previewSize = 56.dp
        val filePreviewWidth = 160.dp
        val filePreviewIconSize = 24.dp
    }

    object Media {
        val playIconSize = 40.dp
    }

    object SwipeRevealActions {
        val actionWidth = 80.dp
    }

    object MainScreen {
        val bottomBarHeight = 60.dp
    }

    object AppLockScreen {
        val iconSize = 72.dp
    }

    object OverviewScreen {
        val unreadBadgeMinSize = 20.dp
        val emptyGraphicWidth = 96.dp
        val emptyGraphicHeight = 16.dp
        val emptyGraphicDotRadius = 5.dp
        val emptyGraphicContactRadius = 6.dp
        val emptyGraphicStroke = 1.dp
    }

    object GroupDetailsScreen {
        val memberStatusIconSize = 18.dp
        val cameraBadgeSize = 28.dp
        val cameraIconSize = 16.dp
        val avatarProgressStrokeWidth = 3.dp
        val verificationProgressSize = 20.dp
        val loadingSize = 160.dp
        val avatarProgressSize = 32.dp
        val sectionIconSize = 22.dp
    }

    object GroupConversationScreen {
        val topBarAvatarSize = 36.dp
        val avatarSize = 28.dp
        val pinBarWidth = 2.dp
    }

    object DirectConversationScreen {
        val topBarAvatarSize = 36.dp
        val authorizationIconSize = 16.dp
    }

    object OnboardingScreen {
        val horizontalPadding = 32.dp
        val topPadding = 48.dp
        val bottomPadding = 24.dp
        val bottomBarHeight = 48.dp
        val lineWidth = 1.dp
        val markSize = 210.dp
        val onionGraphicHeight = 160.dp
        val dotsGraphicHeight = 160.dp
        val receivingGraphicHeight = 160.dp
        val contactsGraphicHeight = 160.dp
        val wordSlide = 8.dp
        val rowSlide = 60.dp
        val choiceMarkSize = 6.dp
    }

    object InvitationsScreen {
        val avatarSize = 40.dp
        val progressSize = 20.dp
        val actionIconSize = 24.dp
        val tabIndicatorHeight = 1.dp
    }

    object BlockedContactsScreen {
        val avatarSize = 40.dp
        val dialogListMaxHeight = 300.dp
    }

    object ContactDetailsScreen {
        val loadingSize = 160.dp
        val sectionIconSize = 18.dp
        val rowIconSize = 20.dp
        val statusDotSize = 7.dp
    }

    object ContactsScreen {
        val selectionProgressSize = 20.dp
        val contactStatusSize = 24.dp
        val contactStatusStroke = 2.dp
        val contactStatusIconSize = 16.dp
        val emptyStateIconContainerSize = 80.dp
        val emptyStateIconSize = 36.dp
        val addContactButtonSize = 50.dp
        val menuIconContainerSize = 40.dp
        val dragHandleWidth = 36.dp
        val dragHandleHeight = 4.dp
        val menuItemIconSize = 22.dp
    }

    object ShareIdentityScreen {
        val avatarSize = 88.dp
        val actionIconContainerSize = 40.dp
        val progressSize = 20.dp
        val qrCodeSize = 240.dp
    }

    object IdentityScreen {
        val iconBadgeSize = 80.dp
        val iconBadgeBorderWidth = 1.dp
        val iconBadgeIconSize = 36.dp
    }

    object SettingsScreen {
        val primaryIconSize = 22.dp
        val secondaryIconSize = 20.dp
    }

    object DeveloperMenuScreen {
        val progressSize = 18.dp
    }

    object ControlPlaneSettingsScreen {
        val statusIndicatorSize = 10.dp
    }

    object ImportIdentityScreen {
        val progressSize = 20.dp
        val resultIconSize = 18.dp
    }

    object TypingIndicator {
        val dotSize = 5.dp
        val dotGap = 3.dp
    }

    object HoldAction {
        val size = 76.dp
        val iconSize = 24.dp
        val outerStroke = 1.5.dp
        val innerStroke = 1.dp
        val progressStroke = 2.dp
        val ringGap = 6.dp
    }

    object ScanFrame {
        val height = 140.dp
        val scanningHeight = 280.dp
        val iconSize = 26.dp
        val cornerInset = 14.dp
        val cornerLength = 16.dp
        val cornerStroke = 1.5.dp
    }

    object AttachmentBar {
        val buttonSize = 44.dp
        val iconSize = 22.dp
    }
}
