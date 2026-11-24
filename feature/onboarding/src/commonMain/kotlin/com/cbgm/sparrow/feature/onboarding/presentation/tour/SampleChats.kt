package com.cbgm.sparrow.feature.onboarding.presentation.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.component.SwipeActionTone
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_attachments_media
import com.cbgm.sparrow.resources.feature_onboarding_tour_sample_family
import com.cbgm.sparrow.resources.feature_onboarding_tour_sample_follow_up
import com.cbgm.sparrow.resources.feature_onboarding_tour_sample_tomorrow
import com.cbgm.sparrow.resources.main_title_and_nav_chats
import org.jetbrains.compose.resources.stringResource

/** A chat list like the real one, with a contact still to verify, a verified one and a group. */
@Composable
internal fun SampleChats(
    ringModifier: Modifier,
    rowModifier: Modifier,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        SparrowTopBar(
            title = { Text(stringResource(Res.string.main_title_and_nav_chats)) },
            windowInsets = WindowInsets(0)
        )
        SparrowSwipeRevealItem(
            modifier = rowModifier.fillMaxWidth(),
            actions =
                listOf(
                    SwipeRevealAction(tone = SwipeActionTone.DESTRUCTIVE, onClick = onDelete) {
                        Icon(imageVector = SparrowIcons.Delete, contentDescription = null)
                    }
                )
        ) {
            SampleRow(
                name = SAMPLE_CONTACT,
                preview = stringResource(Res.string.feature_onboarding_tour_sample_follow_up),
                time = "09:16",
                unreadCount = 2,
                ring = AvatarRing.PENDING,
                avatarModifier = ringModifier
            )
        }
        SampleDivider()
        SampleRow(
            name = "Alice",
            preview = stringResource(Res.string.feature_onboarding_tour_sample_tomorrow),
            time = "08:41",
            unreadCount = 0,
            ring = AvatarRing.VERIFIED
        )
        SampleDivider()
        SampleRow(
            name = stringResource(Res.string.feature_onboarding_tour_sample_family),
            preview = stringResource(Res.string.feature_attachments_media),
            time = "07:55",
            unreadCount = 0,
            ring = AvatarRing.NONE
        )
    }
}

@Composable
private fun SampleRow(
    name: String,
    preview: String,
    time: String,
    unreadCount: Int,
    ring: AvatarRing,
    avatarModifier: Modifier = Modifier
) {
    val hasUnread = unreadCount > 0
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { SparrowAvatar(name = name, target = null, ring = ring, modifier = avatarModifier) },
        headlineContent = {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Text(
                text = preview,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = if (hasUnread) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasUnread) {
                    Box(
                        modifier =
                            Modifier
                                .sizeIn(minWidth = Dimens.OverviewScreen.unreadBadgeMinSize, minHeight = Dimens.OverviewScreen.unreadBadgeMinSize)
                                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.circle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier =
                                Modifier.padding(
                                    horizontal = MaterialTheme.spacing.overviewScreen.unreadBadgeHorizontalPadding,
                                    vertical = MaterialTheme.spacing.overviewScreen.unreadBadgeVerticalPadding
                                )
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun SampleDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(start = MaterialTheme.spacing.listDividerStart),
        thickness = Dimens.Base.dividerThickness,
        color = MaterialTheme.colorScheme.divider
    )
}

internal const val SAMPLE_CONTACT = "Bob"
