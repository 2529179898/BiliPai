package com.android.purebilibili.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.purebilibili.core.ui.AppContentColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.core.ui.AppSemanticAccentRole
import com.android.purebilibili.core.ui.AppSemanticIconFamily
import com.android.purebilibili.core.ui.AppSemanticVisualPolicy
import com.android.purebilibili.core.ui.rememberAppSemanticVisualPolicy

internal data class SettingsEntryVisual(
    val icon: ImageVector? = null,
    @DrawableRes val iconResId: Int? = null,
    val iconTint: Color,
    val iconSizeDp: Int = 20,
)

@Composable
internal fun rememberSettingsEntryTint(
    role: AppSemanticAccentRole,
    iosTint: Color,
): Color {
    return AppContentColors.secondaryText()
}

@Composable
internal fun rememberSettingsEntryVisual(
    target: SettingsSearchTarget,
): SettingsEntryVisual {
    val policy = rememberAppSemanticVisualPolicy()
    val neutralColor = AppContentColors.secondaryText()
    val resolved = remember(target, policy, neutralColor) {
        resolveSettingsEntryVisual(target, policy, neutralColor)
    }
    // Material 3's native ListItem renders ImageVector leading content more
    // consistently than a VectorDrawable painter (whose XML tint can be
    // overridden by the platform theme). Keep the resource for legacy skins,
    // but also expose the vector form so MD3 rows never lose their glyph.
    return if (resolved.icon == null && resolved.iconResId != null) {
        resolved.copy(icon = rememberMaterialSymbol(resolved.iconResId))
    } else {
        resolved
    }
}

internal fun resolveSettingsEntryVisual(
    target: SettingsSearchTarget,
    policy: AppSemanticVisualPolicy = AppSemanticVisualPolicy(
        iconFamily = AppSemanticIconFamily.MATERIAL,
        accentPalette = null,
        prefersNativeChrome = false,
        supportsIndependentLiquidGlass = true,
    ),
    neutralColor: Color = Color.Unspecified,
): SettingsEntryVisual {
    val iconTint = neutralColor
    return when (target) {
        SettingsSearchTarget.TELEGRAM -> SettingsEntryVisual(
            iconResId = R.drawable.ic_telegram_mono,
            iconTint = iconTint
        )
        SettingsSearchTarget.TWITTER -> SettingsEntryVisual(
            icon = AppIcons.Twitter,
            iconTint = iconTint
        )
        else -> {
            val role = resolveSettingsSearchTargetIconRole(target)
            SettingsEntryVisual(
                iconResId = resolveSettingsMaterialSymbolResource(role),
                iconTint = iconTint,
                iconSizeDp = resolveSettingsSemanticIconSizeDp(role, policy.effectiveIconFamily),
            )
        }
    }
}
