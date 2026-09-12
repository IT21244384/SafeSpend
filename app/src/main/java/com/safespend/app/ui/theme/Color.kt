package com.safespend.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * SafeSpend palette — built on the 60-30-10 rule.
 *
 *   60%  Neutral canvas. Backgrounds, cards, list rows, most text.
 *        Light: Mist #F4F6F7 on White #FFFFFF.  Dark: Ink #0E1517 on #151D1F.
 *
 *   30%  Deep Teal #0E4F52. The brand. Balance hero card, selected navigation,
 *        section headers, primary chart series, filled primary buttons.
 *
 *   10%  Coral #FF7A45. One accent, used sparingly and only where the user must
 *        act or look: the add-transaction FAB, over-budget warnings, the
 *        safe-to-spend figure when it runs low.
 *
 * Income/expense green and red are *data* colours, not brand colours. They only
 * ever tint an amount or a chart mark, never a surface, so they don't compete
 * with the 10% accent.
 *
 * Material You dynamic colour is deliberately NOT enabled: it would replace this
 * palette with wallpaper-derived hues on Android 12+ and the ratio would no
 * longer hold.
 */

// 60% — neutral canvas
val MistLight = Color(0xFFF4F6F7)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE3EAEA)
val InkLight = Color(0xFF0F1D21)
val MutedLight = Color(0xFF5A6B6E)
val OutlineLight = Color(0xFFBDCBCB)

val MistDark = Color(0xFF0E1517)
val SurfaceDark = Color(0xFF151D1F)
val SurfaceVariantDark = Color(0xFF232E30)
val InkDark = Color(0xFFE3E9EA)
val MutedDark = Color(0xFF9DAFB1)
val OutlineDark = Color(0xFF3C4A4C)

// 30% — brand teal
val TealDeep = Color(0xFF0E4F52)
val TealDeeper = Color(0xFF06282A)
val TealContainerLight = Color(0xFFCFE7E6)
val TealSoft = Color(0xFF4A6365)
val TealSoftContainerLight = Color(0xFFDDE8E8)

val TealBright = Color(0xFF7FD2D2)
val TealContainerDark = Color(0xFF0B4043)
val TealSoftContainerDark = Color(0xFF2A3B3C)

// 10% — coral accent
val Coral = Color(0xFFFF7A45)
val CoralContainerLight = Color(0xFFFFE1D3)
val CoralOnContainerLight = Color(0xFF3A1400)
val CoralLight = Color(0xFFFFB08C)
val CoralContainerDark = Color(0xFF5C2410)

// Data colours
val IncomeLight = Color(0xFF1E8E5A)
val ExpenseLight = Color(0xFFC4443A)
val IncomeDark = Color(0xFF6FD79E)
val ExpenseDark = Color(0xFFFF938A)

val LightColors = lightColorScheme(
    primary = TealDeep,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealDeeper,
    secondary = TealSoft,
    onSecondary = Color.White,
    secondaryContainer = TealSoftContainerLight,
    onSecondaryContainer = TealDeeper,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = CoralContainerLight,
    onTertiaryContainer = CoralOnContainerLight,
    background = MistLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = MutedLight,
    outline = OutlineLight,
    outlineVariant = SurfaceVariantLight,
    error = ExpenseLight,
    onError = Color.White,
)

val DarkColors = darkColorScheme(
    primary = TealBright,
    onPrimary = TealDeeper,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealContainerLight,
    secondary = TealBright,
    onSecondary = TealDeeper,
    secondaryContainer = TealSoftContainerDark,
    onSecondaryContainer = TealContainerLight,
    tertiary = CoralLight,
    onTertiary = Color(0xFF4A1A05),
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = CoralContainerLight,
    background = MistDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
    outlineVariant = SurfaceVariantDark,
    error = ExpenseDark,
    onError = Color(0xFF3A0906),
)

/** Colours Material 3 has no slot for: income/expense and the chart series ramp. */
data class MoneyColors(
    val income: Color,
    val expense: Color,
    val chartSeries: List<Color>,
)

val LightMoneyColors = MoneyColors(
    income = IncomeLight,
    expense = ExpenseLight,
    chartSeries = listOf(
        TealDeep,
        Color(0xFF2F7F82),
        Color(0xFF58A9A4),
        Color(0xFF8CC6BE),
        Coral,
        Color(0xFFE8A33D),
        Color(0xFF7E6BB0),
        Color(0xFFB0AFAF),
    ),
)

val DarkMoneyColors = MoneyColors(
    income = IncomeDark,
    expense = ExpenseDark,
    chartSeries = listOf(
        TealBright,
        Color(0xFF58B3B3),
        Color(0xFF89CFC8),
        Color(0xFFB6E3DA),
        CoralLight,
        Color(0xFFF0C178),
        Color(0xFFB4A5DC),
        Color(0xFF8E9A9B),
    ),
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoneyColors }

/** Shorthand so screens can write `moneyColors.income`. */
val moneyColors: MoneyColors
    @Composable @ReadOnlyComposable
    get() = LocalMoneyColors.current
