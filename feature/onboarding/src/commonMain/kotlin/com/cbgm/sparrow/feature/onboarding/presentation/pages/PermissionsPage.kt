import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.ListingRow
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_contacts
import com.cbgm.sparrow.resources.base_notifications
import com.cbgm.sparrow.resources.base_permissions
import com.cbgm.sparrow.resources.base_phone_number
import com.cbgm.sparrow.resources.feature_onboarding_allow_and_continue
import com.cbgm.sparrow.resources.feature_onboarding_audio
import com.cbgm.sparrow.resources.feature_onboarding_audio_description
import com.cbgm.sparrow.resources.feature_onboarding_camera
import com.cbgm.sparrow.resources.feature_onboarding_camera_description
import com.cbgm.sparrow.resources.feature_onboarding_contacts_permission_description
import com.cbgm.sparrow.resources.feature_onboarding_notifications_description
import com.cbgm.sparrow.resources.feature_onboarding_permissions_description
import com.cbgm.sparrow.resources.feature_onboarding_permissions_settings_hint
import com.cbgm.sparrow.resources.feature_onboarding_phone_number_permission_description
import com.cbgm.sparrow.resources.feature_onboarding_storage
import com.cbgm.sparrow.resources.feature_onboarding_storage_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun PermissionsPage(
    onRequestPermissions: () -> Unit
) {
    val permissions = permissionPages()
    val pagerState = rememberPagerState(
        pageCount = { permissions.size }
    )

    Column(
        modifier = Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PermissionsHeader()

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        PermissionsPager(
            permissions = permissions,
            pagerState = pagerState
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        SparrowApprovalButton(
            onClick = onRequestPermissions,
            text = stringResource(
                Res.string.feature_onboarding_allow_and_continue
            )
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.base)
        )

        PermissionsSettingsHint()
    }
}

@Composable
private fun PermissionsHeader() {
    Text(
        text = stringResource(Res.string.base_permissions),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(
        modifier = Modifier.height(MaterialTheme.spacing.base)
    )

    Text(
        text = stringResource(
            Res.string.feature_onboarding_permissions_description
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PermissionsPager(
    permissions: List<PageData>,
    pagerState: PagerState
) {
    HorizontalPager(
        state = pagerState,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp)
    ) { page ->
        PermissionPagerPage(
            permission = permissions[page]
        )
    }

    Spacer(
        modifier = Modifier.height(MaterialTheme.spacing.base)
    )

    PermissionsPagerIndicator(
        pageCount = permissions.size,
        currentPage = pagerState.currentPage
    )
}

@Composable
private fun PermissionPagerPage(
    permission: PageData
) {
    ListingRow(
        index = permission.index,
        title = permission.title,
        description = permission.description
    )
}

@Composable
private fun PermissionsPagerIndicator(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(
                MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            PermissionsPagerIndicatorDot(
                selected = index == currentPage
            )
        }
    }
}

@Composable
private fun PermissionsPagerIndicatorDot(
    selected: Boolean
) {
    Box(
        modifier =
            Modifier
                .size(
                    if (selected) {
                        8.dp
                    } else {
                        6.dp
                    }
                )
                .clip(CircleShape)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
    )
}

@Composable
private fun PermissionsSettingsHint() {
    Text(
        text = stringResource(
            Res.string.feature_onboarding_permissions_settings_hint
        ),
        style = MaterialTheme.typography.labelMedium,
        color =
            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = Alpha.PermissionsPage.helperText
            ),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun permissionPages(): List<PageData> =
    listOf(
        PageData(
            index = "01",
            title = stringResource(
                Res.string.base_notifications
            ),
            description = stringResource(
                Res.string.feature_onboarding_notifications_description
            )
        ),
        PageData(
            index = "02",
            title = stringResource(
                Res.string.base_contacts
            ),
            description = stringResource(
                Res.string.feature_onboarding_contacts_permission_description
            )
        ),
        PageData(
            index = "03",
            title = stringResource(
                Res.string.feature_onboarding_camera
            ),
            description = stringResource(
                Res.string.feature_onboarding_camera_description
            )
        ),
        PageData(
            index = "04",
            title = stringResource(
                Res.string.feature_onboarding_audio
            ),
            description = stringResource(
                Res.string.feature_onboarding_audio_description
            )
        ),
        PageData(
            index = "05",
            title = stringResource(
                Res.string.feature_onboarding_storage
            ),
            description = stringResource(
                Res.string.feature_onboarding_storage_description
            )
        ),
        PageData(
            index = "06",
            title = stringResource(
                Res.string.base_phone_number
            ),
            description = stringResource(
                Res.string.feature_onboarding_phone_number_permission_description
            )
        )
    )

private data class PageData(
    val index: String,
    val title: String,
    val description: String
)

@Preview
@Composable
private fun PermissionsPagePreview() {
    SparrowTheme {
        PermissionsPage(
            onRequestPermissions = {}
        )
    }
}
