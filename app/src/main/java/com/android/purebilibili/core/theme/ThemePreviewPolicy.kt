package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.store.ThemeRoleOverrides
import com.android.purebilibili.core.store.WallpaperPaletteOverrides
import com.android.purebilibili.feature.settings.Md3ColorSource
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

internal data class ThemePreviewConfiguration(
    val uiStyle: AppUiStyle = AppUiStyle.MATERIAL3,
    val source: Md3ColorSource = Md3ColorSource.CUSTOM,
    val liquidGlassEnabled: Boolean = false,
    val amoledDarkTheme: Boolean = false,
    val roleOverrides: ThemeRoleOverrides = ThemeRoleOverrides(),
    val wallpaperOverrides: WallpaperPaletteOverrides = WallpaperPaletteOverrides(),
    val syncControlAccentToSeed: Boolean = false,
)

/** Matches saving a candidate: generation, saved edits, glass selection, wallpaper edits,
 * native bridge and foreground correction. No persisted settings are written by preview. */
internal fun resolveThemePreviewPalette(
    seed: Color,
    darkTheme: Boolean,
    style: PaletteStyle,
    spec: ColorSpec.SpecVersion,
    contrast: Contrast,
    seedStyle: SeedPaletteStyle,
    configuration: ThemePreviewConfiguration,
): AppThemePalette {
    val base = createBiliPaiStyleColorScheme(
        seed, darkTheme, configuration.amoledDarkTheme, style, spec,
        uiStyle = configuration.uiStyle, colorContrast = contrast, seedPaletteStyle = seedStyle,
    )
    val overrides = resolveEffectiveThemeRoleOverrides(configuration.source, configuration.roleOverrides, configuration.uiStyle)
        .let { if (seedStyle.enabled) it.copy(enabled = false) else it }
        .let { if (configuration.syncControlAccentToSeed) syncThemeRoleControlAccent(it, formatMd3CustomColorHex(seed)) else it }
    val configured = resolveConfiguredMaterialScheme(
        base, overrides, darkTheme, style, spec, contrast, seedStyle,
        configuration.source, seed, configuration.liquidGlassEnabled,
    )
    return resolveAppThemeEndpointPalette(
        configured, configuration.uiStyle, configuration.source, darkTheme,
        amoledDarkTheme = configuration.amoledDarkTheme,
        wallpaperOverrides = configuration.wallpaperOverrides,
        customSeed = seed, roleOverrides = overrides,
        liquidGlassEnabled = configuration.liquidGlassEnabled,
        preserveGeneratedPalette = seedStyle.enabled,
    )
}
