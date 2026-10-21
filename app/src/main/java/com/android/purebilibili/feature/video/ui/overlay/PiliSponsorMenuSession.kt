package com.android.purebilibili.feature.video.ui.overlay

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.data.repository.SponsorBlockRepository
import com.android.purebilibili.feature.plugin.SPONSOR_BLOCK_PLUGIN_ID
import com.android.purebilibili.feature.plugin.SponsorBlockConfig
import com.android.purebilibili.feature.plugin.SponsorBlockSegmentBehavior
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Keep this session above chrome/dialog visibility branches so dispatched requests survive closing. */
data class PiliSponsorMenuSession(
    val configReady: Boolean,
    val behaviorForCategory: (String) -> SponsorBlockSegmentBehavior,
    val categoryColors: Map<String, String>,
    val onVote: (segmentId: String, voteType: Int) -> Unit,
    val onChangeCategory: (segmentId: String, category: String) -> Unit,
)

private val SponsorMenuConfigJson = Json { ignoreUnknownKeys = true }
private val SponsorMenuConfigMutex = Mutex()

internal fun decodePiliSponsorMenuConfig(raw: String?): SponsorBlockConfig =
    raw?.let { SponsorMenuConfigJson.decodeFromString<SponsorBlockConfig>(it) } ?: SponsorBlockConfig()

/** Use the same persisted configuration/identity migration as SponsorBlockPlugin.loadConfigSuspend. */
private suspend fun loadPiliSponsorMenuConfig(context: Context): SponsorBlockConfig = SponsorMenuConfigMutex.withLock {
    val raw = PluginStore.getConfigJson(context, SPONSOR_BLOCK_PLUGIN_ID)
    val decoded = decodePiliSponsorMenuConfig(raw)
    val normalized = decoded.normalized()
    if (raw == null || decoded.userId.isBlank()) {
        PluginStore.setConfigJson(context, SPONSOR_BLOCK_PLUGIN_ID, SponsorMenuConfigJson.encodeToString(normalized))
    }
    normalized
}

@Composable
fun rememberPiliSponsorMenuSession(bvid: String): PiliSponsorMenuSession {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var config by remember(context) { mutableStateOf(SponsorBlockConfig()) }
    var configReady by remember(context) { mutableStateOf(false) }

    LaunchedEffect(context, bvid) {
        try {
            config = loadPiliSponsorMenuConfig(context)
            configReady = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Toast.makeText(context, "读取空降助手设置失败: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    return remember(context, scope, bvid, config, configReady) {
        fun submit(segmentId: String, voteType: Int? = null, category: String? = null) {
            scope.launch {
                val success = if (category == null) "投票成功" else "类别更改成功"
                val failure = if (category == null) "投票失败" else "类别更改失败"
                try {
                    // Read again at submission: changing server/user identity in settings must
                    // never send a vote to the previous server with a stale identity.
                    val current = loadPiliSponsorMenuConfig(context)
                    config = current
                    configReady = true
                    SponsorBlockRepository.voteOnSegment(
                        baseUrl = current.serverBaseUrl,
                        userId = current.userId,
                        segmentId = segmentId,
                        voteType = voteType,
                        category = category,
                    ).getOrThrow()
                    Toast.makeText(context, success, Toast.LENGTH_SHORT).show()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Toast.makeText(context, "$failure: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
        PiliSponsorMenuSession(
            configReady = configReady,
            behaviorForCategory = config::behaviorFor,
            categoryColors = config.categoryColorHex,
            onVote = { segmentId, voteType -> submit(segmentId, voteType = voteType) },
            onChangeCategory = { segmentId, category -> submit(segmentId, category = category) },
        )
    }
}
