package com.android.purebilibili.feature.video.ui.overlay

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import androidx.media3.common.Player
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Timer lifetime follows the playback session, not a transient settings/overlay composition. */
internal class PiliPlayerSleepTimer private constructor(player: Player, context: Context) {
    private val playerRef = WeakReference(player)
    private val appContext = context.applicationContext
    private var activityRef = WeakReference<Activity>(null)
    private val selectedMinutes = MutableStateFlow<Int?>(null)
    val minutes = selectedMinutes.asStateFlow()
    private var job: Job? = null
    private var waiting = false
    var waitForEnd: Boolean = false
        set(value) {
            field = value
            if (!value && waiting) finish()
        }
    var exitApp: Boolean = false
    private val endedListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (waiting && playbackState == Player.STATE_ENDED) finish()
        }
    }

    fun attachActivity(activity: Activity?) { activityRef = WeakReference(activity) }

    fun setTimer(minutes: Int?) {
        cancel()
        val duration = minutes?.takeIf { it > 0 } ?: return
        selectedMinutes.value = duration
        val deadline = SystemClock.elapsedRealtime() + duration * 60_000L
        job = scope.launch {
            delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            val player = playerRef.get()
            if (player == null) { cancel(); return@launch }
            if (waitForEnd && player.playbackState != Player.STATE_ENDED) {
                waiting = true
                player.addListener(endedListener)
            } else finish()
        }
    }

    private fun cancel() {
        job?.cancel()
        job = null
        waiting = false
        playerRef.get()?.removeListener(endedListener)
        selectedMinutes.value = null
    }

    private fun finish() {
        playerRef.get()?.pause()
        cancel()
        if (exitApp) activityRef.get()?.finishAffinity()
        else Toast.makeText(appContext, "定时结束，已暂停播放", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val sessions = WeakHashMap<Player, PiliPlayerSleepTimer>()
        fun forPlayer(player: Player, context: Context): PiliPlayerSleepTimer =
            sessions.getOrPut(player) { PiliPlayerSleepTimer(player, context) }
    }
}
