package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.store.HomeWallpaperEffectMode
import com.android.purebilibili.feature.home.HomeFeedSkeletonCard
import com.android.purebilibili.feature.home.rememberHomeFeedSkeletonPulseState

/** BGM recommendations use ElegantVideoCard, so reuse its home-feed loading silhouette. */
@Composable
fun VideoCardSkeleton(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") index: Int = 0,
    coverAspectRatio: Float = 4f / 3f,
    compactMetadata: Boolean = false,
) {
    val pulse = rememberHomeFeedSkeletonPulseState()
    HomeFeedSkeletonCard(
        pulse = { pulse.value },
        wallpaperTintEnabled = false,
        wallpaperEffectMode = HomeWallpaperEffectMode.OFF,
        isDataSaverActive = true,
        coverAspectRatio = coverAspectRatio,
        modifier = modifier,
        compactMetadata = compactMetadata,
    )
}
