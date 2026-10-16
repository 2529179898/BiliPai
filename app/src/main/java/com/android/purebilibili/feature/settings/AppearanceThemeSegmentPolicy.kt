package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.AppIconStyle
import com.android.purebilibili.core.ui.AppListItemStyle
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

internal fun resolveThemeModeSegmentOptions(
    followSystemLabel: String = AppThemeMode.FOLLOW_SYSTEM.label,
    lightLabel: String = AppThemeMode.LIGHT.label,
    darkLabel: String = AppThemeMode.DARK.label
): List<AppSegmentOption<AppThemeMode>> {
    return listOf(
        AppSegmentOption(AppThemeMode.FOLLOW_SYSTEM, followSystemLabel),
        AppSegmentOption(AppThemeMode.LIGHT, lightLabel),
        AppSegmentOption(AppThemeMode.DARK, darkLabel)
    )
}

internal fun resolveColorStyleOptions(): List<AppSegmentOption<PaletteStyle>> {
    return (listOf(PaletteStyle.TonalSpot) + PaletteStyle.entries.filterNot { it == PaletteStyle.TonalSpot })
        .map { style ->
            AppSegmentOption(
                value = style,
                label = style.name,
                description = resolveColorStyleDescription(style),
            )
        }
}

/** 色彩风格在下拉里的补充说明，口径与 MaterialKolor / MCU 的语义一致。 */
internal fun resolveColorStyleDescription(style: PaletteStyle): String = when (style) {
    PaletteStyle.TonalSpot -> "默认风格，柔和低彩度，最贴近 Material 3 观感"
    PaletteStyle.Neutral -> "接近灰阶，只保留很少彩度"
    PaletteStyle.Vibrant -> "彩度最高，主色最抢眼"
    PaletteStyle.Expressive -> "主色色相刻意偏离种子色，配色更有玩味"
    PaletteStyle.Rainbow -> "种子色色相不出现在配色里，色彩更跳脱"
    PaletteStyle.FruitSalad -> "主色与副色色相都偏离种子色"
    PaletteStyle.Monochrome -> "全灰阶，完全不带彩度"
    PaletteStyle.Fidelity -> "调色板贴近种子色，高彩度种子也保留"
    PaletteStyle.Content -> "与 Fidelity 几乎一致，更强调贴合种子色"
    else -> "使用 ${style.name} 色调映射"
}

/** 色彩风格在当前设置项上的显示：枚举名 + 一行含义，不展开也能看明白。 */
internal fun resolveColorStyleSubtitle(style: PaletteStyle): String =
    "当前 ${style.name}：${resolveColorStyleDescription(style)}"

internal fun resolveColorSpecOptions(): List<AppSegmentOption<ColorSpec.SpecVersion>> {
    return listOf(
        ColorSpec.SpecVersion.SPEC_2021,
        ColorSpec.SpecVersion.SPEC_2025
    ).map { spec ->
        AppSegmentOption(
            value = spec,
            label = spec.name,
            description = resolveColorSpecDescription(spec),
        )
    }
}

internal fun resolveColorSpecDescription(spec: ColorSpec.SpecVersion): String = when (spec) {
    ColorSpec.SpecVersion.SPEC_2021 -> "旧版 Material 3 色调映射，与 Android 12 时期一致"
    ColorSpec.SpecVersion.SPEC_2025 -> "新版 Material 3 色调映射，容器层次更新"
    else -> "使用 ${spec.name} 色调映射"
}

/** 色彩标准在当前设置项上的显示：枚举名 + 一行含义。 */
internal fun resolveColorSpecSubtitle(spec: ColorSpec.SpecVersion): String =
    "当前 ${spec.name}：${resolveColorSpecDescription(spec)}"

/**
 * 对比度档。对齐 PiliPlus 的 highContrast / ultraContrast 需求，但走 MaterialKolor
 * 原生的 Contrast（相对默认对比度的加减），不引入 flex_seed_scheme 的 FlexTones。
 */
internal fun resolveColorContrastOptions(): List<AppSegmentOption<Contrast>> {
    return listOf(
        Contrast.Reduced,
        Contrast.Default,
        Contrast.Medium,
        Contrast.High,
    ).map { contrast ->
        AppSegmentOption(
            value = contrast,
            label = contrast.name,
            description = resolveColorContrastDescription(contrast),
        )
    }
}

internal fun resolveColorContrastDescription(contrast: Contrast): String = when (contrast) {
    Contrast.Reduced -> "低于默认对比，配色更柔"
    Contrast.Default -> "Material 3 默认对比度"
    Contrast.Medium -> "高于默认对比，文字更清晰"
    Contrast.High -> "最高对比，接近无障碍高对比主题"
    else -> "使用 ${contrast.name} 对比度"
}

/** 对比度在当前设置项上的显示：枚举名 + 一行含义。 */
internal fun resolveColorContrastSubtitle(contrast: Contrast): String =
    "当前 ${contrast.name}：${resolveColorContrastDescription(contrast)}"

internal fun shouldShowMd3CustomColorControls(source: Md3ColorSource): Boolean =
    source == Md3ColorSource.CUSTOM

internal fun shouldShowMd3PaletteStyleControls(source: Md3ColorSource): Boolean =
    source != Md3ColorSource.FOLLOW_WALLPAPER

internal fun resolveAdvancedPaletteSubtitle(uiStyle: AppUiStyle): String =
    if (uiStyle == AppUiStyle.MIUIX) {
        "已保留配置，仅切换到 Material 3 后生效"
    } else {
        "自定义明暗模式的背景、文字与控件色"
    }

internal fun resolveMd3ColorSourceOptions(): List<AppSegmentOption<Md3ColorSource>> {
    return listOf(
        AppSegmentOption(Md3ColorSource.FOLLOW_WALLPAPER, Md3ColorSource.FOLLOW_WALLPAPER.label),
        AppSegmentOption(Md3ColorSource.WALLPAPER_RETONED, Md3ColorSource.WALLPAPER_RETONED.label),
        AppSegmentOption(Md3ColorSource.CUSTOM, Md3ColorSource.CUSTOM.label)
    )
}

internal fun resolveAppIconStyleOptions(): List<AppSegmentOption<AppIconStyle>> {
    return listOf(
        AppSegmentOption(AppIconStyle.AUTO, "跟随预设"),
        AppSegmentOption(AppIconStyle.THEME_CONTAINER, "主题色容器"),
        AppSegmentOption(AppIconStyle.MD3_STANDARD, "MD3 官方推荐")
    )
}

internal fun resolveAppListItemStyleOptions(): List<AppSegmentOption<AppListItemStyle>> {
    return listOf(
        AppSegmentOption(AppListItemStyle.AUTO, "跟随预设"),
        AppSegmentOption(AppListItemStyle.CUSTOM, "自定义条目"),
        AppSegmentOption(AppListItemStyle.NATIVE, "原生组件")
    )
}

internal fun resolveDarkThemeStyleSegmentOptions(
    defaultLabel: String = DarkThemeStyle.DEFAULT.label,
    amoledLabel: String = DarkThemeStyle.AMOLED.label
): List<AppSegmentOption<DarkThemeStyle>> {
    return listOf(
        AppSegmentOption(DarkThemeStyle.DEFAULT, defaultLabel),
        AppSegmentOption(DarkThemeStyle.AMOLED, amoledLabel)
    )
}

internal fun resolveAppLanguageSegmentOptions(
    followSystemLabel: String = "跟随系统",
    simplifiedChineseLabel: String = "简体中文",
    traditionalChineseLabel: String = "繁體中文",
    englishLabel: String = "英语"
): List<AppSegmentOption<AppLanguage>> {
    return listOf(
        AppSegmentOption(AppLanguage.FOLLOW_SYSTEM, followSystemLabel),
        AppSegmentOption(AppLanguage.SIMPLIFIED_CHINESE, simplifiedChineseLabel),
        AppSegmentOption(AppLanguage.TRADITIONAL_CHINESE_TAIWAN, traditionalChineseLabel),
        AppSegmentOption(AppLanguage.ENGLISH, englishLabel)
    )
}
