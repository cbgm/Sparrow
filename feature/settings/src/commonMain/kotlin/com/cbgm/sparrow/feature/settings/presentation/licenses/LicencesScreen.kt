package com.cbgm.sparrow.feature.settings.presentation.licenses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.component.SparrowStaticScaffold
import com.cbgm.sparrow.feature.settings.presentation.licenses.model.LicensesUiEvent
import com.cbgm.sparrow.feature.settings.presentation.licenses.model.LicensesUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_open_source_licenses
import com.mikepenz.aboutlibraries.ui.compose.DefaultChipColors
import com.mikepenz.aboutlibraries.ui.compose.DefaultLibraryColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.style.m3VariantColors
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    uiState: LicensesUiState,
    onUiEvent: (LicensesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraries by produceLibraries { uiState.libraries }

    SparrowStaticScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LicensesTopBar(onBack = { onUiEvent(LicensesUiEvent.BackClicked) })
        }
    ) { innerPadding ->
        LibrariesContainer(
            libraries = libraries,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            colors = licensesColors(),
            variantColors =
                LibraryDefaults.m3VariantColors(
                    headerBackground = MaterialTheme.colorScheme.background,
                    headerOnBackground = MaterialTheme.colorScheme.onBackground,
                    rowBackground = MaterialTheme.colorScheme.background,
                    rowExpandedBackground = MaterialTheme.colorScheme.surfaceContainer,
                    rowOnBackground = MaterialTheme.colorScheme.onBackground,
                    rowSubtleContent = MaterialTheme.colorScheme.onSurfaceVariant,
                    rowDivider = MaterialTheme.colorScheme.outlineVariant,
                    actionLinkColor = MaterialTheme.colorScheme.primary
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicensesTopBar(onBack: () -> Unit) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_open_source_licenses),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun licensesColors(): DefaultLibraryColors {
    val chipColors =
        DefaultChipColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )

    return DefaultLibraryColors(
        libraryBackgroundColor = MaterialTheme.colorScheme.background,
        libraryContentColor = MaterialTheme.colorScheme.onBackground,
        versionChipColors = chipColors,
        licenseChipColors = chipColors,
        fundingChipColors = chipColors,
        dialogBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dialogContentColor = MaterialTheme.colorScheme.onSurface,
        dialogConfirmButtonColor = MaterialTheme.colorScheme.primary
    )
}
