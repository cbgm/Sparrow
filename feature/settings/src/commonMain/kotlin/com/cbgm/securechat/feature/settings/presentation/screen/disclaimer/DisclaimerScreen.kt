package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.securechat.core.ui.component.SecureChatScrollScaffold
import com.cbgm.securechat.core.ui.theme.spacing
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

private val MarkdownCardColor = Color(0xFF102A46)

@Composable
fun MarkdownDisclaimerScreen(
    title: String,
    markdownContent: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            MarkdownDisclaimerTopBar(
                title = title,
                containerColor = containerColor,
                onBack = onBack
            )
        }
    ) { innerPadding, scrollState ->
        Markdown(
            content = markdownContent,
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.small,
                        bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.small,
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium
                    ),
            colors =
                markdownColor(
                    text = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    codeBackground = MarkdownCardColor,
                    inlineCodeBackground = MarkdownCardColor,
                    dividerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    tableBackground = MarkdownCardColor
                ),
            typography =
                markdownTypography(
                    h1 =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                    h2 =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                    h3 =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        ),
                    text =
                        MaterialTheme.typography.labelMedium.copy(
                            color =
                                MaterialTheme.colorScheme.onBackground.copy(
                                    alpha = 0.85f
                                )
                        ),
                    paragraph =
                        MaterialTheme.typography.bodySmall.copy(
                            color =
                                MaterialTheme.colorScheme.onBackground.copy(
                                    alpha = 0.85f
                                )
                        ),
                    list =
                        MaterialTheme.typography.bodySmall.copy(
                            color =
                                MaterialTheme.colorScheme.onBackground.copy(
                                    alpha = 0.85f
                                )
                        )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkdownDisclaimerTopBar(
    title: String,
    containerColor: Color,
    onBack: () -> Unit
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = title,
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
