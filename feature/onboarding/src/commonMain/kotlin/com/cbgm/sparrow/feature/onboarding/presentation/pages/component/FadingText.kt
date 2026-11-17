package com.cbgm.sparrow.feature.onboarding.presentation.pages.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.cbgm.sparrow.core.ui.theme.Dimens

/**
 * Text that appears word by word from [startMillis] on the page [clock]: each word fades in and rises a little. Words
 * between asterisks ("Nobody sees *who* you talk to.") are shown in [highlightColor]. Screen readers get the whole text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FadingText(
    text: String,
    clock: State<Long>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    startMillis: Long = 0L,
    highlightColor: Color = color
) {
    val words = remember(text) { fadingWords(text) }
    val density = LocalDensity.current
    val wordGap = with(density) { (style.fontSize * WORD_GAP_EM).toDp() }
    val slide = with(density) { Dimens.OnboardingScreen.wordSlide.toPx() }
    FlowRow(
        modifier = modifier.clearAndSetSemantics { contentDescription = words.joinToString(" ") { word -> word.text } },
        horizontalArrangement = Arrangement.spacedBy(wordGap)
    ) {
        words.forEachIndexed { index, word ->
            Text(
                text = word.text,
                style = style,
                color = if (word.highlighted) highlightColor else color,
                modifier =
                    Modifier.graphicsLayer {
                        val shown = clock.progress(startMillis + index * WORD_STEP_MILLISECONDS, WORD_FADE_MILLISECONDS)
                        alpha = shown
                        translationY = (1f - shown) * slide
                    }
            )
        }
    }
}

private data class FadingWord(
    val text: String,
    val highlighted: Boolean
)

private fun fadingWords(text: String): List<FadingWord> {
    var highlighted = false
    return text.split(' ').filter { word -> word.isNotBlank() }.map { word ->
        val opens = word.startsWith(HIGHLIGHT_MARK)
        val closes = word.trimEnd('.', ',', '?', '!').endsWith(HIGHLIGHT_MARK)
        val shown = FadingWord(word.replace(HIGHLIGHT_MARK.toString(), ""), highlighted || opens)
        highlighted = (highlighted || opens) && !closes
        shown
    }
}

private const val HIGHLIGHT_MARK = '*'
private const val WORD_GAP_EM = 0.27f
private const val WORD_STEP_MILLISECONDS = 70L
private const val WORD_FADE_MILLISECONDS = 700L
