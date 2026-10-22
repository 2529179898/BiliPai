package com.android.purebilibili.feature.video.player

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.compose.runtime.MutableState
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.android.purebilibili.feature.audio.player.AudioNowPlayingSession
import com.android.purebilibili.core.lifecycle.BackgroundManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse

class MiniPlayerManagerLeakCleanupTest {

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `dismiss releases media session when playback is closed`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.registerReceiver(any(), any(), any(), any()) } returns null

        val context = mockContext()
        val manager = newMiniPlayerManager(context)
        val mediaSession = mockk<MediaSession>(relaxed = true)
        manager.mediaSession = mediaSession

        try {
            manager.dismiss()

            verify(exactly = 1) { mediaSession.release() }
        } finally {
            BackgroundManager.removeListener(manager)
        }
    }

    @Test
    fun `release unregisters receiver and background listener`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.registerReceiver(any(), any(), any(), any()) } returns null

        val context = mockContext()
        val manager = newMiniPlayerManager(context)

        manager.release()

        verify(exactly = 1) { context.unregisterReceiver(any()) }
        assertFalse(backgroundListeners().contains(manager))
    }

    @Test
    fun `task removal stops local audio and leaves remote snapshot cleanup to its service`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.registerReceiver(any(), any(), any(), any()) } returns null
        val context = mockContext()
        val manager = newMiniPlayerManager(context)
        val player = mockk<ExoPlayer>(relaxed = true)
        val mediaSession = mockk<MediaSession>(relaxed = true)
        @Suppress("UNCHECKED_CAST")
        val playerState = MiniPlayerManager::class.java.getDeclaredField("_player\$delegate")
            .apply { isAccessible = true }.get(manager) as MutableState<ExoPlayer?>
        playerState.value = player
        manager.mediaSession = mediaSession
        AudioNowPlayingSession.markListening()
        AudioNowPlayingSession.publishBarOverlayVisible(true)

        try {
            manager.dismissForTaskRemoval()

            verify(exactly = 1) { player.pause() }
            verify(exactly = 1) { player.stop() }
            verify(exactly = 1) { mediaSession.release() }
            // 只停止本地播放服务，独立音频服务自行完成快照清理。
            verify(exactly = 1) { context.stopService(any()) }
            verify(exactly = 0) { context.startService(any()) }
            assertFalse(AudioNowPlayingSession.active.value)
            assertFalse(AudioNowPlayingSession.barOverlayVisible.value)
        } finally {
            playerState.value = null
            manager.release()
            AudioNowPlayingSession.dismiss()
            AudioNowPlayingSession.publishBarOverlayVisible(false)
        }
    }

    private fun newMiniPlayerManager(context: Context): MiniPlayerManager {
        val constructor = MiniPlayerManager::class.java.getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(context)
    }

    private fun mockContext(): Context {
        val context = mockk<Context>(relaxed = true)
        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { context.startService(any()) } returns ComponentName("com.android.purebilibili", "PlaybackService")
        return context
    }

    @Suppress("UNCHECKED_CAST")
    private fun backgroundListeners(): MutableList<BackgroundManager.BackgroundStateListener> {
        val field = BackgroundManager::class.java.getDeclaredField("listeners")
        field.isAccessible = true
        return field.get(BackgroundManager) as MutableList<BackgroundManager.BackgroundStateListener>
    }
}
