package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use the reference mockup’s clear system sans; no extra font asset is bundled.
val SparrowFontFamily = FontFamily.Default

val Typography =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
        bodyMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp
            ),
        bodySmall =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp
            ),
        titleLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.sp
            ),
        titleMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp
            ),
        titleSmall =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp
            ),
        labelMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
        labelSmall =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.sp
            ),
        headlineLarge =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.sp
            ),
        headlineMedium =
            TextStyle(
                fontFamily = SparrowFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = 0.sp
            )
    )
