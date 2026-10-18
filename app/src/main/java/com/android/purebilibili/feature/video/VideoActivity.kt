// 文件路径: feature/video/VideoActivity.kt
package com.android.purebilibili.feature.video

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import com.android.purebilibili.core.util.Logger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.metrics.performance.JankStats
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.common.ProvideAppTextSelectionHost
import com.android.purebilibili.core.ui.AppWindowSystemUiController
import com.android.purebilibili.core.ui.ProvideAppThemeConfig
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.ui.performance.AppRuntimeVisualGuardTracker
import com.android.purebilibili.core.ui.performance.ProvideRuntimeVisualGuard
import com.android.purebilibili.core.ui.performance.applyPreferredDisplayMode
import com.android.purebilibili.core.ui.adaptive.toAdaptiveFoldPosture
import com.android.purebilibili.core.ui.transition.LocalVideoTransitionAdaptiveInfo
import com.android.purebilibili.core.ui.transition.VideoTransitionAdaptiveInfo
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.calculateWindowSizeClass
import com.android.purebilibili.core.util.rememberAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.applyPlayerRequestedOrientation
import com.android.purebilibili.core.util.resolveAppDisplayContext
import com.android.purebilibili.core.util.resolveSafeAndroidPipRational
import com.android.purebilibili.core.util.LARGE_SCREEN_SMALLEST_WIDTH_DP
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.player.buildPipPlaybackRemoteActions
import com.android.purebilibili.feature.video.screen.isActivityInMultiWindowOrFloatingMode
import androidx.window.layout.WindowMetricsCalculator
// Imports for moved classes
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


private const val TAG = "BiliPlayerActivity"


class VideoActivity : ComponentActivity() {

    private val viewModel: VideoPlaybackViewModel by viewModels()
    private var isFullscreen by mutableStateOf(false)
    private var isInPipMode by mutableStateOf(false)
    private var runtimeJankStats: JankStats? = null
    private val runtimeVisualGuardSession = Any()
    

    //  1. 权限回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) Logger.d(TAG, " 通知权限已授予") else com.android.purebilibili.core.util.Logger.w(TAG, " 通知权限被拒绝，媒体控件可能无法显示")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            applyPreferredDisplayMode(SettingsManager.getScreenDisplayModeId(this@VideoActivity).first())
        }
        enableEdgeToEdge()
        AppWindowSystemUiController.configureEdgeToEdgeHost(this)
        val entryDisplayContext = resolveAppDisplayContext()
        val entryIsInMultiWindowOrFloatingMode = isActivityInMultiWindowOrFloatingMode(
            activity = this,
            displayContext = entryDisplayContext,
        )
        // 分屏 / 系统小窗（freeform）下窗口可以跟随横竖屏，不再强制竖屏，
        // 让视频以横屏打开；普通手机窗口维持既有竖屏策略。
        if (
            savedInstanceState == null &&
            !entryIsInMultiWindowOrFloatingMode &&
            (entryDisplayContext.isFoldableCoverWindow ||
                minOf(
                    entryDisplayContext.currentWindowWidthDp,
                    entryDisplayContext.currentWindowHeightDp,
                ) < LARGE_SCREEN_SMALLEST_WIDTH_DP)
        ) {
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                displayContext = entryDisplayContext,
            )
        }

        //  2. 请求权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        

        val bvid = intent.getStringExtra("bvid")
        if (bvid.isNullOrBlank()) {
            finish()
            return
        }

        updateStateFromConfig(resources.configuration)

        setContent {
            val configuration = LocalConfiguration.current
            val calculator = remember { WindowMetricsCalculator.getOrCreate() }
            val currentWindowMetrics = remember(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
            ) {
                calculator.computeCurrentWindowMetrics(this@VideoActivity)
            }
            val maximumWindowMetrics = remember(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
            ) {
                calculator.computeMaximumWindowMetrics(this@VideoActivity)
            }
            val materialWindowAdaptiveInfo =
                androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2()
            val windowSizeClass = calculateWindowSizeClass(
                metrics = currentWindowMetrics,
                maximumMetrics = maximumWindowMetrics,
                adaptiveWindowSizeClass = materialWindowAdaptiveInfo.windowSizeClass,
            )
            val windowWidthSizeClass = windowSizeClass.widthSizeClass
            val appWindowAdaptiveInfo = rememberAppWindowAdaptiveInfo(
                windowSizeClass = windowSizeClass,
                windowPosture = materialWindowAdaptiveInfo.windowPosture,
            )
            val videoTransitionAdaptiveInfo = remember(
                windowWidthSizeClass,
                appWindowAdaptiveInfo.posture,
            ) {
                VideoTransitionAdaptiveInfo(
                    widthSizeClass = windowWidthSizeClass,
                    foldPosture = appWindowAdaptiveInfo.posture.toAdaptiveFoldPosture(),
                )
            }
            val blurIntensity by SettingsManager.getBlurIntensity(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = BlurIntensity.THIN)
            val headerBlurEnabled by SettingsManager.getHeaderBlurEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val bottomBarBlurEnabled by SettingsManager.getBottomBarBlurEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            val progressiveTopBlurEnabled by SettingsManager
                .getProgressiveTopBlurEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val progressiveTopFadeEnabled by SettingsManager
                .getProgressiveTopFadeEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val cardDynamicTintEnabled by SettingsManager
                .getHomeCardDynamicTintEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            val cardFrostedGlassEnabled by SettingsManager
                .getHomeCardFrostedGlassEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            val hapticFeedbackEnabled by SettingsManager
                .getHapticFeedbackEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val globalTextTapCopyEnabled by SettingsManager
                .getGlobalTextTapCopyEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            val globalDragFeedbackEnabled by SettingsManager
                .getGlobalDragFeedbackEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val uiEntranceAnimationEnabled by SettingsManager
                .getUiEntranceAnimationEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val runtimeVisualGuardEnabled by SettingsManager
                .getRuntimeVisualGuardEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val liquidGlassEnabled by SettingsManager.getAndroidNativeLiquidGlassEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val appThemeConfig = remember(
                liquidGlassEnabled,
                blurIntensity,
                headerBlurEnabled,
                bottomBarBlurEnabled,
                progressiveTopBlurEnabled,
                progressiveTopFadeEnabled,
                hapticFeedbackEnabled,
                globalTextTapCopyEnabled,
                uiEntranceAnimationEnabled,
                globalDragFeedbackEnabled,
                runtimeVisualGuardEnabled,
            ) {
                AppThemeConfig(
                    liquidGlassEnabled = liquidGlassEnabled,
                    blurIntensity = blurIntensity,
                    headerBlurEnabled = headerBlurEnabled,
                    bottomBarBlurEnabled = bottomBarBlurEnabled,
                    progressiveTopBlurEnabled = progressiveTopBlurEnabled,
                    progressiveTopFadeEnabled = progressiveTopFadeEnabled,
                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                    globalTextTapCopyEnabled = globalTextTapCopyEnabled,
                    uiEntranceAnimationEnabled = uiEntranceAnimationEnabled,
                    globalDragFeedbackEnabled = globalDragFeedbackEnabled,
                    runtimeVisualGuardEnabled = runtimeVisualGuardEnabled,
                )
            }
            MaterialTheme {
                // 与 MainActivity 对齐：没有这两个 provider 时，overlay 里的每个
                // unifiedBlur 会各起一个 DataStore 收集器，且完全读不到运行时视觉守卫。
                ProvideAppThemeConfig(config = appThemeConfig) {
                ProvideRuntimeVisualGuard(widthSizeClass = windowWidthSizeClass) {
                com.android.purebilibili.core.ui.blur.ProvideUnifiedBlurIntensity {
                ProvideAppTextSelectionHost {
                CompositionLocalProvider(
                    com.android.purebilibili.feature.aicu.LocalAicuNavigation provides { uid: Long? ->
                        startActivity(android.content.Intent(this@VideoActivity, com.android.purebilibili.MainActivity::class.java).apply {
                            putExtra(com.android.purebilibili.EXTRA_PENDING_NAVIGATION_ROUTE,
                                com.android.purebilibili.navigation.ScreenRoutes.AicuQuery.createRoute(uid))
                        })
                    },
                    LocalWindowSizeClass provides windowSizeClass,
                    LocalAppWindowAdaptiveInfo provides appWindowAdaptiveInfo,
                    com.android.purebilibili.core.ui.LocalHingeSafeOverlayRegions provides
                        com.android.purebilibili.core.ui.adaptive.rememberHingeSafeOverlayRegions(
                            adaptiveInfo = appWindowAdaptiveInfo
                        ),
                    LocalVideoTransitionAdaptiveInfo provides videoTransitionAdaptiveInfo,
                    com.android.purebilibili.feature.home.components.cards.LocalHomeCardDynamicTintEnabled provides
                        cardDynamicTintEnabled,
                    com.android.purebilibili.feature.home.components.cards.LocalHomeCardFrostedGlassEnabled provides
                        cardFrostedGlassEnabled,
                ) {
                // VideoDetailScreen handles its own UI state and player initialization
                com.android.purebilibili.feature.video.screen.VideoDetailScreen(
                    bvid = bvid,
                    coverUrl = "", // Will be updated when video info loads
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onNavigateToAudioMode = {
                        viewModel.setAudioMode(true)
                    },
                    onVideoClick = { vid, options ->
                        VideoActivity.start(this, vid, options)
                    },
                    miniPlayerManager = MiniPlayerManager.getInstance(this@VideoActivity),
                    isInPipMode = isInPipMode ||
                        MiniPlayerManager.getInstance(this@VideoActivity).shouldKeepPlaybackForPipTransition()
                )
                }
                }
                }
                }
            }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 会话级重置：独立播放器每次可见都视为新会话，清掉跨会话累积的降级记账；
        // 否则一次偶发掉帧会让后续进入长期停留在 Reduced 档位（「越进越卡」）。
        AppRuntimeVisualGuardTracker.beginSession(runtimeVisualGuardSession)
        val existingJankStats = runtimeJankStats
        if (existingJankStats != null) {
            existingJankStats.isTrackingEnabled = true
        } else {
            runtimeJankStats = runCatching {
                JankStats.createAndTrack(window) { frameData ->
                    AppRuntimeVisualGuardTracker.onFrame(
                        session = runtimeVisualGuardSession,
                        frameData = frameData,
                        nowMs = SystemClock.uptimeMillis(),
                    )
                }
            }.onFailure { throwable ->
                Logger.w(TAG, "无法启动独立播放器性能采样", throwable)
            }.getOrNull()
        }
    }

    override fun onResume() {
        super.onResume()
        val mini = MiniPlayerManager.getInstance(this)
        mini.clearUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            mini.updateSystemPipActive(true)
        } else {
            mini.clearPlaybackRoutePipState()
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (
            event.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE &&
            !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed
        ) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                onBackPressedDispatcher.onBackPressed()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        AppRuntimeVisualGuardTracker.discardActiveWindow(runtimeVisualGuardSession)
        runtimeJankStats?.isTrackingEnabled = false
        super.onStop()
    }
    
    override fun onDestroy() {
        runtimeJankStats?.isTrackingEnabled = false
        runtimeJankStats = null
        super.onDestroy()
    }

    // --- 配置与全屏逻辑保持不变 ---
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateStateFromConfig(newConfig)
    }

    private fun updateStateFromConfig(config: Configuration) {
        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        isFullscreen = isLandscape && !resolveAppDisplayContext(config).usesInWindowFullscreen
    }

    private fun toggleFullscreen() {
        val displayContext = resolveAppDisplayContext()
        if (displayContext.usesInWindowFullscreen) {
            isFullscreen = !isFullscreen
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                displayContext = displayContext,
            )
            return
        }
        if (isFullscreen) {
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                displayContext = displayContext,
            )
        } else {
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                displayContext = displayContext,
            )
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val mini = MiniPlayerManager.getInstance(this)
        return PictureInPictureParams.Builder()
            .setAspectRatio(resolveSafeAndroidPipRational(mini.videoWidth, mini.videoHeight))
            .setActions(buildPipPlaybackRemoteActions(this, mini.player))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(mini.shouldEnterPip() && mini.player?.isPlaying == true)
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) return
        val mini = MiniPlayerManager.getInstance(this)
        mini.markUserLeaveHint()
        mini.refreshMediaSessionBinding()
        mini.updatePlaybackRoutePipRequest(false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !mini.shouldEnterPip() || mini.player?.isPlaying != true
        ) return

        try {
            val params = buildPipParams()
            mini.updatePlaybackRoutePipRequest(true)
            if (!enterPictureInPictureMode(params)) {
                mini.updatePlaybackRoutePipRequest(false)
                Logger.w(TAG, "System rejected PiP entry")
            }
        } catch (error: Exception) {
            mini.updatePlaybackRoutePipRequest(false)
            Logger.e(TAG, "Failed to enter PiP", error)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        MiniPlayerManager.getInstance(this).updateSystemPipActive(isInPictureInPictureMode)
        Logger.d(TAG, "PiP mode changed: $isInPictureInPictureMode")
    }

    companion object {
        fun start(context: Context, bvid: String, options: android.os.Bundle? = null) {
            val intent = Intent(context, VideoActivity::class.java).apply {
                putExtra("bvid", bvid)
            }
            context.startActivity(intent, options)
        }
    }
}
