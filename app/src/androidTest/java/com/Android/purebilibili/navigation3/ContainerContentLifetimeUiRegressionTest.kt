package com.android.purebilibili.navigation3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.components.AppContainerNavigationSource
import com.android.purebilibili.core.ui.components.LocalAppContainerNavigationCapture
import com.android.purebilibili.core.ui.components.LocalAppContainerNavigationSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContainerContentLifetimeUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun hiddenSourceRecordsPixelsAgainAfterCoveredPageRemounts() {
        val sourceVisible = mutableStateOf(true)
        val owned = mutableStateOf(false)
        val rowColor = mutableStateOf(Color.Red)
        val captured = mutableStateOf<AppContainerNavigationSource?>(null)
        val registry = mutableMapOf<Any, AppContainerNavigationSource>()
        rule.setContent {
            Column {
                Box(Modifier.size(80.dp).background(Color.Black).testTag("source-slot")) {
                    CompositionLocalProvider(
                        LocalAppContainerNavigationSources provides registry,
                        LocalAppContainerNavigationCapture provides { source, action ->
                            captured.value = source
                            source.retained = true
                            source.transitionOwnsContent = { owned.value }
                            owned.value = true
                            action()
                        },
                    ) {
                        if (sourceVisible.value) {
                            AppContainerNavigationSource(sourceId = "播放设置", onClick = {}) { click ->
                                // Child layers must also survive capture; checking semantics or
                                // a non-null GraphicsLayer alone cannot detect an empty export.
                                Box(Modifier.size(80.dp).graphicsLayer().background(rowColor.value)
                                    .testTag("remount-action").clickable { click?.invoke() })
                            }
                        }
                    }
                }
                Box(Modifier.size(80.dp).background(Color.Black).testTag("return-export")
                    .drawWithContent {
                        drawContent()
                        captured.value?.closedLayer?.let { drawLayer(it) }
                    })
            }
        }
        fun assertCenter(tag: String, expected: Color) {
            val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
            assertEquals(expected, pixels[pixels.width / 2, pixels.height / 2])
        }
        rule.onNodeWithTag("remount-action").performClick()
        assertCenter("source-slot", Color.Black)
        assertCenter("return-export", Color.Red)
        rule.runOnIdle { sourceVisible.value = false }
        rule.runOnIdle {
            assertFalse(requireNotNull(captured.value).attached)
            rowColor.value = Color.Green
            sourceVisible.value = true
        }
        rule.runOnIdle {
            assertTrue(requireNotNull(captured.value).attached)
        }
        // Reattached while still hidden: the transition must receive fresh row pixels.
        assertCenter("source-slot", Color.Black)
        assertCenter("return-export", Color.Green)
        rule.runOnIdle { owned.value = false }
        assertCenter("source-slot", Color.Green)
        rule.runOnIdle { requireNotNull(captured.value).releaseRetainedContent() }
        assertCenter("source-slot", Color.Green)
    }

    @Test
    fun sourcePixelsRestoreAtReturnEndpointAndAfterBoundaryRelease() {
        val owned = mutableStateOf(false)
        var captured: AppContainerNavigationSource? = null
        rule.setContent {
            Box(Modifier.size(80.dp).background(Color.Black).testTag("source-pixels")) {
                CompositionLocalProvider(
                    LocalAppContainerNavigationCapture provides { source, action ->
                        captured = source
                        source.retained = true
                        source.transitionOwnsContent = { owned.value }
                        owned.value = true
                        action()
                    },
                ) {
                    AppContainerNavigationSource(sourceId = "source", onClick = {}) { click ->
                        Box(Modifier.size(80.dp).background(Color.Red).testTag("source-action").clickable { click?.invoke() })
                    }
                }
            }
        }
        fun assertCenter(expected: Color) {
            val pixels = rule.onNodeWithTag("source-pixels").captureToImage().toPixelMap()
            assertEquals(expected, pixels[pixels.width / 2, pixels.height / 2])
        }
        assertCenter(Color.Red)
        rule.onNodeWithTag("source-action").performClick()
        assertCenter(Color.Black)
        // Returning to depth zero restores pixels before destination disposal.
        rule.runOnIdle { owned.value = false }
        assertCenter(Color.Red)
        // Cancelled return keeps the source hidden until the boundary is released.
        rule.runOnIdle { owned.value = true }
        assertCenter(Color.Black)
        rule.runOnIdle { requireNotNull(captured).releaseRetainedContent() }
        assertCenter(Color.Red)
    }

    @Test
    fun repeatedCaptureAndLazySourceRemovalKeepPageCompositionAndReleaseRetainedLayers() {
        val sourceVisible = mutableStateOf(true)
        val owned = mutableStateOf(false)
        val destinationSource = mutableStateOf<AppContainerNavigationSource?>(null)
        val registry = mutableMapOf<Any, AppContainerNavigationSource>()
        var sourceMounts = 0
        var sourceDisposals = 0
        var pageMounts = 0
        var pageDisposals = 0
        rule.setContent {
            Column {
                CompositionLocalProvider(
                    LocalAppContainerNavigationSources provides registry,
                    LocalAppContainerNavigationCapture provides { source, action ->
                        source.retained = true
                        source.transitionOwnsContent = { owned.value }
                        destinationSource.value = source
                        owned.value = true
                        action()
                    },
                ) {
                    LazyColumn(Modifier.height(80.dp)) {
                        if (sourceVisible.value) {
                            item(key = "source") {
                                AppContainerNavigationSource(sourceId = "source", onClick = {}) { click ->
                                    DisposableEffect(Unit) {
                                        sourceMounts++
                                        onDispose { sourceDisposals++ }
                                    }
                                    BasicText("Open", Modifier.size(160.dp, 60.dp).clickable { click?.invoke() })
                                }
                            }
                        }
                    }
                }
                SettingsContainerEntryContent(source = destinationSource.value, containerOwnsPage = false) {
                    DisposableEffect(Unit) {
                        pageMounts++
                        onDispose { pageDisposals++ }
                    }
                    BasicText("Page")
                }
            }
        }
        repeat(3) { cycle ->
            rule.onNodeWithText("Open").performClick()
            var captured: AppContainerNavigationSource? = null
            rule.runOnIdle {
                captured = destinationSource.value
                assertNotNull(captured?.closedLayer)
                assertTrue(requireNotNull(captured).closedDrawingReady)
                assertEquals(cycle + 1, sourceMounts)
                assertEquals(cycle, sourceDisposals)
                assertEquals(1, pageMounts)
                assertEquals(0, pageDisposals)
                sourceVisible.value = false
            }
            rule.runOnIdle {
                val source = requireNotNull(captured)
                assertFalse(source.attached)
                assertNotNull(source.closedLayer)
                assertSame(source, destinationSource.value)
                assertEquals(cycle + 1, sourceDisposals)
                source.releaseRetainedContent()
                assertNull(source.closedLayer)
                assertFalse(source.closedDrawingReady)
                destinationSource.value = null
                owned.value = false
            }
            rule.runOnIdle {
                assertEquals(1, pageMounts)
                assertEquals(0, pageDisposals)
                registry.clear()
                sourceVisible.value = true
            }
        }
    }
}
