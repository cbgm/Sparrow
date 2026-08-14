package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cbgm.sparrow.resources.AppleGaramond
import com.cbgm.sparrow.resources.AppleGaramond_Bold
import com.cbgm.sparrow.resources.Res
import org.jetbrains.compose.resources.Font

val SparrowFontFamily @Composable get() =
    FontFamily(
        Font(
            resource = Res.font.AppleGaramond,
            weight = FontWeight.Normal
        ),
        Font(
            resource = Res.font.AppleGaramond_Bold,
            weight = FontWeight.Bold
        )
    )

val Typography @Composable get() =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 21.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
        bodyMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
        bodySmall =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
        titleLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 60.sp,
                lineHeight = 50.sp,
                letterSpacing = 0.sp
            ),
        titleMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 40.sp,
                lineHeight = 30.sp,
                letterSpacing = 0.sp
            ),
        titleSmall =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.5.sp
            ),
        labelMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp
            ),
        labelSmall =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.5.sp
            ),
        headlineLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                lineHeight = 50.sp,
                letterSpacing = 0.sp
            ),
        headlineMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 40.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            )
    )
