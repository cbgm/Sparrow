package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val micro: Dp = 4.dp,
    val base: Dp = 8.dp,
    val small: Dp = 16.dp,
    val medium: Dp = 24.dp,
    val large: Dp = 32.dp,
    val screenPadding: Dp = 24.dp,
    val zero: Dp = 0.dp,
    val listDividerStart: Dp = 80.dp,
    val profilePictureCameraScreen: ProfilePictureCameraScreenSpacing = ProfilePictureCameraScreenSpacing(),
    val profilePictureCropScreen: ProfilePictureCropScreenSpacing = ProfilePictureCropScreenSpacing(),
    val button: ButtonSpacing = ButtonSpacing(),
    val card: CardSpacing = CardSpacing(),
    val startupScreen: StartupScreenSpacing = StartupScreenSpacing(),
    val importIdentityScreen: ImportIdentityScreenSpacing = ImportIdentityScreenSpacing(),
    val overviewScreen: OverviewScreenSpacing = OverviewScreenSpacing(),
    val messageList: MessageListSpacing = MessageListSpacing(),
    val directScreen: DirectScreenSpacing = DirectScreenSpacing(),
    val groupScreen: GroupScreenSpacing = GroupScreenSpacing(),
    val groupDetailsScreen: GroupDetailsScreenSpacing = GroupDetailsScreenSpacing(),
    val messageBubble: MessageBubbleSpacing = MessageBubbleSpacing(),
    val identityScreen: IdentityScreenSpacing = IdentityScreenSpacing(),
    val contactsScreen: ContactsScreenSpacing = ContactsScreenSpacing(),
    val textField: FieldSpacing = FieldSpacing()
) {
    operator fun times(multiplier: Int): Dp = base * multiplier

    operator fun times(multiplier: Float): Dp = base * multiplier.toInt()
}

data class FieldSpacing(
    val horizontalPadding: Dp = 12.dp,
    val verticalPadding: Dp = 6.dp
)

data class ProfilePictureCameraScreenSpacing(
    val captureButtonBottomPadding: Dp = 36.dp
)

data class ProfilePictureCropScreenSpacing(
    val horizontalPadding: Dp = 20.dp,
    val topPadding: Dp = 80.dp,
    val bottomPadding: Dp = 104.dp
)

data class ButtonSpacing(
    val iconContentPadding: Dp = 12.dp
)

data class CardSpacing(
    val bottomShadowPadding: Dp = 30.dp
)

data class StartupScreenSpacing(
    val titleGap: Dp = 18.dp
)

data class ImportIdentityScreenSpacing(
    val resultVerticalPadding: Dp = 12.dp,
    val resultIconGap: Dp = 10.dp
)

data class OverviewScreenSpacing(
    val unreadBadgeHorizontalPadding: Dp = 6.dp,
    val unreadBadgeVerticalPadding: Dp = 1.dp
)

data class MessageListSpacing(
    val horizontalPadding: Dp = 12.dp
)

data class DirectScreenSpacing(
    val securityBannerVerticalPadding: Dp = 10.dp,
    val securityDescriptionTopPadding: Dp = 2.dp,
    val verifiedBannerVerticalPadding: Dp = 6.dp,
    val verifiedContentGap: Dp = 6.dp
)

data class GroupScreenSpacing(
    val senderGap: Dp = 6.dp
)

data class GroupDetailsScreenSpacing(
    val dialogProgressPadding: Dp = 2.dp,
    val dialogErrorTopPadding: Dp = 12.dp
)

data class MessageBubbleSpacing(
    val metadataTopPadding: Dp = 3.dp,
    val senderBottomPadding: Dp = 3.dp,
    val stackedCheckStartPadding: Dp = 1.dp,
    val deliveryLabelGap: Dp = 3.dp
)

data class IdentityScreenSpacing(
    val contentTopPadding: Dp = 80.dp,
    val protectionBadgeHorizontalPadding: Dp = 12.dp,
    val protectionBadgeVerticalPadding: Dp = 6.dp
)

data class ContactsScreenSpacing(
    val dragHandleTopPadding: Dp = 10.dp
)

val LocalSpacing = compositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
