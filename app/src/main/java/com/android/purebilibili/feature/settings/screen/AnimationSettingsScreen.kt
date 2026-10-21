// 文件路径: feature/settings/AnimationSettingsScreen.kt
package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppContentColors
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.feature.settings.ui.LocalSettingsTopContentPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import com.android.purebilibili.core.ui.components.AppLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.*
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.ui.blur.shouldAllowHomeChromeLiquidGlass
import com.android.purebilibili.core.store.AppNavigationSettings
import com.android.purebilibili.core.store.LiquidGlassAdvancedSettings
import com.android.purebilibili.core.store.LiquidGlassReadabilityMode
import com.android.purebilibili.core.store.home.LiquidGlassSettingsStore
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_TRANSITION_CUSTOM_MAX_MILLIS
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold
import com.android.purebilibili.feature.settings.share.SettingsShareService
import com.android.purebilibili.feature.settings.share.SettingsShareImportSession
import com.android.purebilibili.feature.settings.share.flattenSettingsShareSections
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionSpeed
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VideoTransitionAdaptiveInfo
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionDurationMillis
import com.android.purebilibili.core.ui.adaptive.toAdaptiveFoldPosture
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.ui.transition.normalizeVideoSharedTransitionCustomDurationMillis
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackExitDirection
import com.android.purebilibili.navigation.resolveVisibleBottomBarItems
import com.android.purebilibili.feature.home.components.resolveBottomBarVisibleItemsForSearchMode
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.ui.animation.EntranceGroup
import com.android.purebilibili.core.ui.animation.entrance
import com.android.purebilibili.core.ui.animation.rememberEffectiveEntranceMotionSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Build
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

/**
 *  动画与效果设置二级页面
 * 管理卡片动画、过渡效果、磨砂效果等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    advancedOnly: Boolean = false,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val screenTitle = if (advancedOnly) "玻璃高级调节" else stringResource(R.string.animation_effects_title)
    val backLabel = stringResource(R.string.common_back)
    val bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    SettingsPageScaffold(
        title = screenTitle,
        onBack = onBack,
        backContentDescription = backLabel,
        bottomContentPadding = bottomContentPadding,
        scrollHost = SettingsPageScrollHost.External,
        externalContentHandlesTopPadding = true,
        topBarBlurEnabled = state.headerBlurEnabled,
    ) {
        CompositionLocalProvider(LocalSettingsLiquidGlassEnabled provides state.isLiquidGlassEnabled) {
            AnimationSettingsContent(
                state = state,
                viewModel = viewModel,
                advancedOnly = advancedOnly,
            )
        }
    }
}

@Composable
fun AnimationSettingsContent(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    advancedOnly: Boolean = false,
) {
    val context = LocalContext.current
    val previewWindowSize = LocalWindowSizeClass.current
    val previewWindowInfo = LocalAppWindowAdaptiveInfo.current
    val previewTransitionDurationMillis = resolveVideoSharedTransitionDurationMillis(
        VideoSharedTransitionSpeedSettings(state.videoSharedTransitionSpeed, state.videoSharedTransitionCustomDurationMillis),
        VideoTransitionAdaptiveInfo(
            widthSizeClass = previewWindowSize.widthSizeClass,
            foldPosture = previewWindowInfo.posture.toAdaptiveFoldPosture(),
        ),
    )
    val scope = rememberCoroutineScope()
    val liquidGlassShareService = remember(context.applicationContext) {
        SettingsShareService(context.applicationContext)
    }
    var pendingLiquidGlassImport by remember {
        mutableStateOf<SettingsShareImportSession?>(null)
    }
    var isLiquidGlassImporting by remember { mutableStateOf(false) }
    val liquidGlassImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isLiquidGlassImporting = true
            try {
                liquidGlassShareService.readLiquidGlassImportSession(uri)
                    .onSuccess { pendingLiquidGlassImport = it }
                    .onFailure { error ->
                        Toast.makeText(
                            context,
                            error.message ?: "无法读取液态玻璃设置文件",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
            } finally {
                isLiquidGlassImporting = false
            }
        }
    }
    val listState = rememberLazyListState()
    val focusRequest by SettingsSearchFocusController.request.collectAsStateWithLifecycle()
    val windowSizeClass = LocalWindowSizeClass.current
    val warningTint = AppContentColors.warning()
    val deviceUiProfile = remember(windowSizeClass.widthSizeClass) {
        resolveDeviceUiProfile(
            widthSizeClass = windowSizeClass.widthSizeClass
        )
    }
    val cardMotionTier = resolveAnimationSettingsCardMotionTier(
        baseTier = deviceUiProfile.motionTier,
        cardAnimationEnabled = state.cardAnimationEnabled
    )
    val motionTierLabel = remember(cardMotionTier) {
        when (cardMotionTier) {
            MotionTier.Reduced -> "低动效"
            MotionTier.Normal -> "标准"
            MotionTier.Enhanced -> "增强"
        }
    }
    val motionTierHint = remember(cardMotionTier) {
        when (cardMotionTier) {
            MotionTier.Reduced -> "动画更轻、更快，减少性能开销"
            MotionTier.Normal -> "平衡性能与动效，适合大多数设备"
            MotionTier.Enhanced -> "动画幅度更大，层次更明显"
        }
    }
    val isLiquidGlassAvailable = shouldAllowHomeChromeLiquidGlass(Build.VERSION.SDK_INT)
    val liquidGlassPreviewImageUri by SettingsManager
        .getLiquidGlassPreviewImageUri(context)
        .collectAsStateWithLifecycle(initialValue = null)
    val liquidGlassAdvancedSettings by SettingsManager
        .getLiquidGlassAdvancedSettings(context)
        .collectAsStateWithLifecycle(
            initialValue = LiquidGlassAdvancedSettings()
        )
    val liquidGlassReadabilityMode by LiquidGlassSettingsStore
        .observeReadabilityMode(context)
        .collectAsStateWithLifecycle(initialValue = LiquidGlassReadabilityMode.STABLE)
    val uiEntranceAnimationEnabled by SettingsManager.getUiEntranceAnimationEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val globalDragFeedbackEnabled by SettingsManager.getGlobalDragFeedbackEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val maidLottieAnimationEnabled by SettingsManager.getMaidLottieAnimationEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val skeletonBreathingEnabled = com.android.purebilibili.core.ui.skeleton.rememberSkeletonBreathingEnabled()
    val globalTextTapCopyEnabled by SettingsManager
        .getGlobalTextTapCopyEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val appNavigationSettings by SettingsManager.getAppNavigationSettings(context)
        .collectAsStateWithLifecycle(initialValue = AppNavigationSettings())
    val previewBottomBarItems = remember(
        appNavigationSettings.orderedVisibleTabIds,
        state.bottomBarSearchEnabled,
        state.bottomBarSearchLayoutMode,
    ) {
        resolveBottomBarVisibleItemsForSearchMode(
            visibleItems = resolveVisibleBottomBarItems(
                appNavigationSettings.orderedVisibleTabIds
            ),
            bottomBarSearchEnabled = state.bottomBarSearchEnabled,
            searchLayoutMode = state.bottomBarSearchLayoutMode,
        )
    }
    val videoTransitionRealtimeBlurEnabled by SettingsManager
        .getVideoTransitionRealtimeBlurEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val liveSurfaceCardTransitionEnabled by SettingsManager
        .getLiveSurfaceCardTransitionEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val relatedVideoTransitionEnabled by SettingsManager
        .getRelatedVideoTransitionEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val fullScreenSwipeBackEnabled by SettingsManager
        .getFullScreenSwipeBackEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val effectiveEntranceSpec = rememberEffectiveEntranceMotionSpec()
    // 开关开着、但有效参数被降级为不动画 → 系统减弱动效在生效。
    val entranceDowngradedBySystem = uiEntranceAnimationEnabled && !effectiveEntranceSpec.animate
    val sharedTransitionSpeedOptions = remember {
        listOf(
            AppSegmentOption(VideoSharedTransitionSpeed.FAST, "快速"),
            AppSegmentOption(VideoSharedTransitionSpeed.STANDARD, "标准"),
            AppSegmentOption(VideoSharedTransitionSpeed.SLOW, "慢速"),
            AppSegmentOption(VideoSharedTransitionSpeed.CUSTOM, "自定")
        )
    }
    val predictiveBackStyle = remember(appNavigationSettings) {
        if (appNavigationSettings.predictiveBackEnabled) {
            BiliPaiPredictiveBackAnimationStyle.fromStorageValue(
                appNavigationSettings.predictiveBackAnimationStyle
            )
        } else {
            BiliPaiPredictiveBackAnimationStyle.NONE
        }
    }
    val predictiveBackStyleOptions = remember {
        listOf(
            AppSegmentOption(BiliPaiPredictiveBackAnimationStyle.NONE, "无"),
            AppSegmentOption(BiliPaiPredictiveBackAnimationStyle.AOSP, "AOSP"),
            AppSegmentOption(BiliPaiPredictiveBackAnimationStyle.MIUIX, "Miuix"),
            AppSegmentOption(BiliPaiPredictiveBackAnimationStyle.SCALE, "缩放"),
            AppSegmentOption(BiliPaiPredictiveBackAnimationStyle.CLASSIC, "经典"),
        )
    }
    val predictiveBackExitDirection = remember(appNavigationSettings.predictiveBackExitDirection) {
        BiliPaiPredictiveBackExitDirection.fromStorageValue(
            appNavigationSettings.predictiveBackExitDirection
        )
    }
    val predictiveBackExitDirectionOptions = remember {
        listOf(
            AppSegmentOption(BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE, "跟随手势"),
            AppSegmentOption(BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT, "始终向右"),
            AppSegmentOption(BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT, "始终向左"),
        )
    }
    var customTransitionDurationMillis by remember(state.videoSharedTransitionCustomDurationMillis) {
        mutableIntStateOf(state.videoSharedTransitionCustomDurationMillis)
    }
    fun snapCustomTransitionDuration(value: Float): Int {
        val stepMillis = 20
        val min = VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS
        val snapped = min + (((value - min) / stepMillis).roundToInt() * stepMillis)
        return normalizeVideoSharedTransitionCustomDurationMillis(snapped)
    }
    SettingsSectionFocusEffect(
        listState = listState,
        target = SettingsSearchTarget.ANIMATION,
        sectionKeys = (if (advancedOnly) emptyList() else listOf("animation_page_title", "animation_page", "animation_maid_lottie_title", "animation_maid_lottie", "animation_card_title", "animation_card")) + listOf("animation_glass_title", "animation_glass"),
        legacyKeys = mapOf(
            SettingsSearchFocusIds.ANIMATION_START to "animation_page_title",
            SettingsSearchFocusIds.ANIMATION_VISUAL_EFFECTS to "animation_glass_title",
        ),
    )

    EntranceGroup {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = LocalSettingsTopContentPadding.current,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        )
    ) {

            //  界面动效（全 App 入场）
            if (!advancedOnly) {
            item(key = "animation_page_title") {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("页面动画")
                }
            }
            item(key = "animation_page") {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        SettingsItemAnchor("animation.ui_entrance_animation_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.UI_ENTRANCE_ANIMATION),
                                title = settingItemTitle("animation.ui_entrance_animation_enabled"),
                                subtitle = "进入页面时内容依次淡入",
                                checked = uiEntranceAnimationEnabled,
                                onCheckedChange = { value ->
                                    scope.launch {
                                        SettingsManager.setUiEntranceAnimationEnabled(context, value)
                                    }
                                },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.global_drag_feedback_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.DRAG_FEEDBACK),
                                title = settingItemTitle("animation.global_drag_feedback_enabled"),
                                subtitle = "统一控制按钮与胶囊的拖动形变，关闭后保留按压反馈",
                                checked = globalDragFeedbackEnabled,
                                onCheckedChange = { value ->
                                    scope.launch { SettingsManager.setGlobalDragFeedbackEnabled(context, value) }
                                },
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.haptic_feedback_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.HAPTIC_FEEDBACK),
                                title = settingItemTitle("animation.haptic_feedback_enabled"),
                                subtitle = "切换页面或完成操作时轻微振动",
                                checked = state.hapticFeedbackEnabled,
                                onCheckedChange = viewModel::toggleHapticFeedback,
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.global_text_tap_copy_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.COPY_TEXT),
                                title = settingItemTitle("animation.global_text_tap_copy_enabled"),
                                subtitle = "点按正文文字即可复制",
                                checked = globalTextTapCopyEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        SettingsManager.setGlobalTextTapCopyEnabled(context, enabled)
                                    }
                                },
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                        if (entranceDowngradedBySystem) {
                            AppPreferenceDivider()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                AppText(
                                    text = "系统已开启「减弱动效」，入场动画已自动关闭。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppContentColors.secondaryText()
                                )
                            }
                        }
                    }
                }
            }

            item(key = "animation_maid_lottie_title") {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("蓝雪女仆")
                }
            }
            item(key = "animation_maid_lottie") {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        SettingsItemAnchor("animation.maid_lottie_animation_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.ANIMATION),
                                title = settingItemTitle("animation.maid_lottie_animation_enabled"),
                                subtitle = "关闭后隐藏女仆动画和静态图",
                                checked = maidLottieAnimationEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        SettingsManager.setMaidLottieAnimationEnabled(context, enabled)
                                    }
                                },
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                    }
                }
            }

            //  卡片动画
            item(key = "animation_card_title") {
                Box(modifier = Modifier.entrance()) {
                    SettingsCardPreviewGroupHeader(
                        enabledStates = mapOf(
                            SettingsFeaturePreview.CARD_ENTRANCE to state.cardAnimationEnabled,
                            SettingsFeaturePreview.CARD_EXPAND to state.cardTransitionEnabled,
                            SettingsFeaturePreview.RELATED_EXPAND to relatedVideoTransitionEnabled,
                            SettingsFeaturePreview.LIVE_FRAME to liveSurfaceCardTransitionEnabled,
                            SettingsFeaturePreview.RETURN_CONTENT to appNavigationSettings.videoReturnContentFollowProgressEnabled,
                            SettingsFeaturePreview.RETURN_TILT to appNavigationSettings.videoSharedReturnGestureFollowEnabled,
                            SettingsFeaturePreview.RETURN_FOLLOW to appNavigationSettings.videoSharedReturnGestureTranslationEnabled,
                            SettingsFeaturePreview.TRANSITION_BLUR to videoTransitionRealtimeBlurEnabled,
                        ),
                        transitionDurationMillis = previewTransitionDurationMillis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item(key = "animation_card") {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                            SettingsItemAnchor("animation.card_animation_enabled") {
                                AppSwitchPreference(
                                    icon = rememberSettingsSemanticIcon(SettingsIconRole.CARD_ENTRANCE_ANIMATION),
                                title = settingItemTitle("animation.card_animation_enabled"),
                                subtitle = "打开首页时卡片依次淡入",
                                checked = state.cardAnimationEnabled,
                                onCheckedChange = { viewModel.toggleCardAnimation(it) },
                                iconTint = AppContentColors.secondaryText()
                            )
                            }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.card_transition_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.CARD_TRANSITION_ANIMATION),
                                title = settingItemTitle("animation.card_transition_enabled"),
                                subtitle = "封面和标题平滑过渡到详情页",
                                checked = state.cardTransitionEnabled,
                                onCheckedChange = { viewModel.toggleCardTransition(it) },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.related_video_transition_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.RELATED_VIDEO_TRANSITION),
                                title = settingItemTitle("animation.related_video_transition_enabled"),
                                subtitle = if (relatedVideoTransitionEnabled) {
                                    "点开相关推荐时，卡片展开为视频页"
                                } else {
                                    "点开相关推荐时，使用普通页面动画"
                                },
                                checked = relatedVideoTransitionEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        SettingsManager.setRelatedVideoTransitionEnabled(context, enabled)
                                    }
                                },
                                enabled = state.cardTransitionEnabled,
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.live_surface_card_transition_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.LIVE_SURFACE_TRANSITION),
                                title = settingItemTitle("animation.live_surface_card_transition_enabled"),
                                subtitle = "过渡时保留正在播放的画面",
                                checked = liveSurfaceCardTransitionEnabled,
                                onCheckedChange = { viewModel.toggleLiveSurfaceCardTransition(it) },
                                enabled = state.cardTransitionEnabled,
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.app_navigation_settings.video_return_content_follow_progress_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.RETURN_CONTENT_FOLLOW),
                                title = settingItemTitle("animation.app_navigation_settings.video_return_content_follow_progress_enabled"),
                                subtitle = if (appNavigationSettings.videoReturnContentFollowProgressEnabled) {
                                    "返回途中，封面和信息逐渐恢复为卡片"
                                } else {
                                    "返回完成后，再切换为视频卡片"
                                },
                                checked = appNavigationSettings.videoReturnContentFollowProgressEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        SettingsManager.setVideoReturnContentFollowProgressEnabled(context, enabled)
                                    }
                                },
                                enabled = state.cardTransitionEnabled,
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.app_navigation_settings.video_shared_return_gesture_follow_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.RETURN_GESTURE_POSE),
                                title = settingItemTitle("animation.app_navigation_settings.video_shared_return_gesture_follow_enabled"),
                                subtitle = if (appNavigationSettings.videoSharedReturnGestureFollowEnabled) {
                                    "滑动返回时，画面随手指倾斜"
                                } else {
                                    "画面不倾斜；是否随手指移动由下方控制"
                                },
                                checked = appNavigationSettings.videoSharedReturnGestureFollowEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        SettingsManager.setVideoSharedReturnGestureFollowEnabled(
                                            context,
                                            enabled,
                                        )
                                    }
                                },
                                enabled = state.cardTransitionEnabled,
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.app_navigation_settings.video_shared_return_gesture_translation_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.RETURN_GESTURE_TRANSLATION),
                                title = settingItemTitle("animation.app_navigation_settings.video_shared_return_gesture_translation_enabled"),
                                subtitle = if (appNavigationSettings.videoSharedReturnGestureTranslationEnabled) {
                                    "滑动返回时，画面随手指移动"
                                } else {
                                    "画面沿固定路线返回；倾斜由上方控制"
                                },
                                checked = appNavigationSettings.videoSharedReturnGestureTranslationEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        SettingsManager.setVideoSharedReturnGestureTranslationEnabled(context, enabled)
                                    }
                                },
                                enabled = state.cardTransitionEnabled,
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.video_transition_realtime_blur_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.WALLPAPER_EFFECT),
                                title = settingItemTitle("animation.video_transition_realtime_blur_enabled"),
                                subtitle = "转场更有层次感；关闭可省电",
                                checked = videoTransitionRealtimeBlurEnabled,
                                onCheckedChange = { viewModel.toggleVideoTransitionRealtimeBlur(it) },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsSingleChoicePreference(
                            icon = rememberSettingsSemanticIcon(SettingsIconRole.PREDICTIVE_BACK),
                            title = "页面切换动画",
                            subtitle = "页面进入与返回共用的动画样式",
                            options = predictiveBackStyleOptions,
                            selectedValue = predictiveBackStyle,
                            onSelectionChange = { style ->
                                scope.launch {
                                    SettingsManager.setPredictiveBackEnabled(context, true)
                                    SettingsManager.setPredictiveBackAnimationStyle(
                                        context,
                                        style.storageValue,
                                    )
                                }
                            },
                            iconTint = AppContentColors.secondaryText()
                        )
                        if (predictiveBackStyle == BiliPaiPredictiveBackAnimationStyle.MIUIX) {
                            AppPreferenceDivider()
                            AppSliderDialogPreference(
                                title = "返回预览幅度",
                                subtitle = "调整滑动返回时的预览幅度",
                                value = appNavigationSettings
                                    .miuixPredictiveBackMaxProgressPercent
                                    .toFloat(),
                                onValueChange = { value ->
                                    scope.launch {
                                        SettingsManager.setMiuixPredictiveBackMaxProgressPercent(
                                            context,
                                            value.roundToInt(),
                                        )
                                    }
                                },
                                valueRange = 0f..100f,
                                steps = 99,
                                valueFormatter = { value -> "${value.roundToInt()}%" },
                            )
                        }
                        if (predictiveBackStyle != BiliPaiPredictiveBackAnimationStyle.NONE) {
                            AppPreferenceDivider()
                            SettingsItemAnchor("animation.app_navigation_settings.miuix_transition_blur_enabled") {
                                AppSwitchPreference(
                                    icon = rememberSettingsSemanticIcon(
                                        SettingsIconRole.MIUIX_TRANSITION_BLUR
                                    ),
                                    title = settingItemTitle("animation.app_navigation_settings.miuix_transition_blur_enabled"),
                                    subtitle = if (appNavigationSettings.miuixTransitionBlurEnabled) {
                                        "滑动返回时，模糊后方页面"
                                    } else {
                                        "返回时不模糊后方页面"
                                    },
                                    checked = appNavigationSettings.miuixTransitionBlurEnabled,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            SettingsManager.setMiuixTransitionBlurEnabled(
                                                context,
                                                enabled,
                                            )
                                        }
                                    },
                                    iconTint = AppContentColors.secondaryText(),
                                )
                            }
                        }
                        if (predictiveBackStyle == BiliPaiPredictiveBackAnimationStyle.SCALE) {
                            AppPreferenceDivider()
                            SettingsSingleChoicePreference(
                                title = "缩放退出方向",
                                subtitle = "仅缩放样式使用",
                                options = predictiveBackExitDirectionOptions,
                                selectedValue = predictiveBackExitDirection,
                                onSelectionChange = { direction ->
                                    scope.launch {
                                        SettingsManager.setPredictiveBackExitDirection(
                                            context,
                                            direction.storageValue,
                                        )
                                    }
                                },
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.full_screen_swipe_back_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.FULLSCREEN_SWIPE_BACK),
                                title = settingItemTitle("animation.full_screen_swipe_back_enabled"),
                                subtitle = if (fullScreenSwipeBackEnabled) {
                                    "列表和设置页可从屏幕中间右滑返回"
                                } else {
                                    "使用屏幕边缘的系统返回手势"
                                },
                                checked = fullScreenSwipeBackEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        SettingsManager.setFullScreenSwipeBackEnabled(context, enabled)
                                    }
                                },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.video_shared_transition_speed") {
                            SettingsSingleChoicePreference(
                                title = settingItemTitle("animation.video_shared_transition_speed"),
                                subtitle = "进出详情页的转场速度",
                                options = sharedTransitionSpeedOptions,
                                selectedValue = state.videoSharedTransitionSpeed,
                                onSelectionChange = viewModel::setVideoSharedTransitionSpeed
                            )
                        }
                        if (state.videoSharedTransitionSpeed == VideoSharedTransitionSpeed.CUSTOM) {
                            AppPreferenceDivider()
                            AppSliderDialogPreference(
                                title = "自定义时长",
                                subtitle = "数值越大，视频转场越慢",
                                value = customTransitionDurationMillis.toFloat(),
                                onValueChange = { value ->
                                    val snappedValue = snapCustomTransitionDuration(value)
                                    customTransitionDurationMillis = snappedValue
                                    viewModel.setVideoSharedTransitionCustomDurationMillis(snappedValue)
                                },
                                valueRange = VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS.toFloat()..
                                    VIDEO_SHARED_TRANSITION_CUSTOM_MAX_MILLIS.toFloat(),
                                steps = (
                                    (VIDEO_SHARED_TRANSITION_CUSTOM_MAX_MILLIS -
                                        VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS) / 20
                                    ) - 1,
                                valueFormatter = { value -> "${value.roundToInt()}ms" },
                            )
                        }
                        AppPreferenceDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            AppText(
                                text = "首页动画强度",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppContentColors.secondaryText()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AppText(
                                text = motionTierLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppContentColors.primaryText()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AppText(
                                text = motionTierHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppContentColors.secondaryText()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            AppText(
                                text = "此项只调整首页卡片，设置页动画独立控制。",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppContentColors.secondaryText()
                            )
                        }
                    }
                }
            }

            // ✨ 视觉效果
            }
            item(key = "animation_glass_title") {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle(if (advancedOnly) "玻璃高级调节" else "液态玻璃与磨砂")
                }
            }
            item(key = "animation_glass") {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        if (!isLiquidGlassAvailable || !state.androidNativeLiquidGlassEnabled) {
                            AppText(
                                text = if (!isLiquidGlassAvailable) "当前设备不支持玻璃效果，以下参数暂不生效" else "先在外观设置中启用液态玻璃，以下参数暂不生效",
                                modifier = Modifier.padding(16.dp),
                                color = AppContentColors.secondaryText(),
                            )
                        }
                        LiquidGlassAdjustmentPanel(
                                alwaysShowAdvanced = false,
                                persistedProgress = state.liquidGlassProgress,
                                previewImageUri = liquidGlassPreviewImageUri,
                                persistedAdvancedSettings = liquidGlassAdvancedSettings,
                                persistedReadabilityMode = liquidGlassReadabilityMode,
                                bottomBarItems = previewBottomBarItems,
                                bottomBarSearchEnabled = state.bottomBarSearchEnabled,
                                onProgressCommitted = viewModel::setLiquidGlassProgress,
                                onPreviewImageChanged = viewModel::setLiquidGlassPreviewImageUri,
                                onAdvancedSettingsCommitted =
                                    viewModel::setLiquidGlassAdvancedSettings,
                                onReadabilityModeChanged =
                                    viewModel::setLiquidGlassReadabilityMode,
                                onImportSettings = {
                                    liquidGlassImportLauncher.launch(
                                        arrayOf(
                                            "application/json",
                                            "text/json",
                                            "text/plain",
                                        )
                                    )
                                },
                                isImportingSettings = isLiquidGlassImporting,
                                onShareSettings = {
                                    scope.launch {
                                        liquidGlassShareService
                                            .createLiquidGlassShareUri()
                                            .onSuccess { shareUri ->
                                                runCatching {
                                                    val shareIntent = Intent(
                                                        Intent.ACTION_SEND
                                                    ).apply {
                                                        type = "application/json"
                                                        putExtra(
                                                            Intent.EXTRA_STREAM,
                                                            shareUri,
                                                        )
                                                        putExtra(
                                                            Intent.EXTRA_TEXT,
                                                            "BiliPai 液态玻璃设置，可在“动画与触感 > 液态玻璃与磨砂”中导入。",
                                                        )
                                                        addFlags(
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        )
                                                    }
                                                    context.startActivity(
                                                        Intent.createChooser(
                                                            shareIntent,
                                                            "分享液态玻璃设置",
                                                        )
                                                    )
                                                }.onFailure { error ->
                                                    Toast.makeText(
                                                        context,
                                                        error.message ?: "无法打开系统分享",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            }
                                            .onFailure { error ->
                                                Toast.makeText(
                                                    context,
                                                    error.message ?: "液态玻璃设置导出失败",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                    }
                                },
                            )
                        AppPreferenceDivider()
                        if (!advancedOnly) {
                        SettingsItemAnchor("animation.skeleton_breathing_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.SKELETON_BREATHING),
                                title = settingItemTitle("animation.skeleton_breathing_enabled"),
                                subtitle = "加载时，占位卡片轻轻明暗变化",
                                checked = skeletonBreathingEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        com.android.purebilibili.core.store.SkeletonSettingsStore
                                            .setBreathingEnabled(context, enabled)
                                    }
                                },
                                iconTint = AppContentColors.secondaryText(),
                            )
                        }
                        AppPreferenceDivider()
                        // 磨砂效果 (始终显示)
                        SettingsItemAnchor("animation.header_blur_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.TOP_BAR_BLUR),
                                title = settingItemTitle("animation.header_blur_enabled"),
                                subtitle = "模糊顶栏背后的内容，不含折射和光效",
                                checked = state.headerBlurEnabled,
                                onCheckedChange = { viewModel.toggleHeaderBlur(it) },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.progressive_top_blur_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.PROGRESSIVE_TOP_BLUR),
                                title = settingItemTitle("animation.progressive_top_blur_enabled"),
                                subtitle = "顶栏模糊随滚动渐变（需 Android 13+）",
                                checked = state.progressiveTopBlurEnabled,
                                onCheckedChange = { viewModel.toggleProgressiveTopBlur(it) },
                                titleTrailingContent = {
                                    SettingsFeaturePreviewEntry(
                                        feature = SettingsFeaturePreview.TOP_BLUR,
                                        currentEnabled = state.progressiveTopBlurEnabled,
                                    )
                                },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.progressive_top_fade_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.PROGRESSIVE_TOP_FADE),
                                title = settingItemTitle("animation.progressive_top_fade_enabled"),
                                subtitle = "状态栏到顶栏用纯色渐变过渡，比模糊更省电",
                                checked = state.progressiveTopFadeEnabled,
                                onCheckedChange = { viewModel.toggleProgressiveTopFade(it) },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }
                        AppPreferenceDivider()
                        SettingsItemAnchor("animation.bottom_bar_blur_enabled") {
                            AppSwitchPreference(
                                icon = rememberSettingsSemanticIcon(SettingsIconRole.BOTTOM_BAR_BLUR),
                                title = settingItemTitle("animation.bottom_bar_blur_enabled"),
                                subtitle = "模糊底栏背后的内容，不含折射和光效",
                                checked = state.bottomBarBlurEnabled,
                                onCheckedChange = { viewModel.toggleBottomBarBlur(it) },
                                iconTint = AppContentColors.secondaryText()
                            )
                        }

                        // 模糊强度（仅在任意模糊开启时显示）
                        if (state.headerBlurEnabled || state.progressiveTopBlurEnabled || state.bottomBarBlurEnabled) {
                            AppPreferenceDivider()
                            BlurIntensitySelector(
                                selectedIntensity = state.blurIntensity,
                                onIntensityChange = { viewModel.setBlurIntensity(it) }
                            )
                        }
                        }
                    }
                }
            }

            //  提示
            if (!advancedOnly) {
            item {
                Box(modifier = Modifier.entrance()) {
                    AppSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = AppShapes.container(ContainerLevel.Card),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_lightbulb_24),
                                contentDescription = null,
                                tint = warningTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            AppText(
                                text = "如果出现掉帧或耗电增加，可关闭部分动画或玻璃效果。",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppContentColors.secondaryText()
                            )
                        }
                    }
                }
            }

            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    val importSession = pendingLiquidGlassImport
    if (importSession != null) {
        val importCount = remember(importSession) {
            flattenSettingsShareSections(importSession.profile.sections).size
        }
        AppAlertDialog(
            onDismissRequest = {
                if (!isLiquidGlassImporting) pendingLiquidGlassImport = null
            },
            title = {
                AppText(
                    text = "导入液态玻璃设置？",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(
                        text = "配置：${importSession.profile.profileName}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AppText(
                        text = "将替换 $importCount 项液态玻璃参数，包括质感强度、预设、图标与文字颜色以及高级调节。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AppText(
                        text = "预览图片和其他应用设置不会改变。",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppContentColors.link(),
                    )
                }
            },
            confirmButton = {
                AppDialogAction(
                    onClick = {
                        if (isLiquidGlassImporting) return@AppDialogAction
                        scope.launch {
                            isLiquidGlassImporting = true
                            try {
                                liquidGlassShareService.applyLiquidGlassImport(importSession)
                                    .onSuccess { result ->
                                        pendingLiquidGlassImport = null
                                        Toast.makeText(
                                            context,
                                            "已导入 ${result.appliedKeys.size} 项液态玻璃设置",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    .onFailure { error ->
                                        Toast.makeText(
                                            context,
                                            error.message ?: "液态玻璃设置导入失败",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                            } finally {
                                isLiquidGlassImporting = false
                            }
                        }
                    },
                ) {
                    AppText(if (isLiquidGlassImporting) "正在应用" else "确认导入")
                }
            },
            dismissButton = {
                AppDialogAction(
                    onClick = {
                        if (!isLiquidGlassImporting) pendingLiquidGlassImport = null
                    },
                ) {
                    AppText("取消")
                }
            },
        )
    }
}
