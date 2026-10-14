package com.android.purebilibili.feature.message.notification

import android.content.Context
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.network.grpc.DynamicPersonalFeedApi
import com.android.purebilibili.core.network.grpc.DynamicPersonalFeedPage
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.FollowedLiveData
import com.android.purebilibili.data.model.response.FollowedLiveRoom
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.data.model.response.FollowingsData
import com.android.purebilibili.data.model.response.MessageFeedUnreadData
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.data.model.response.SystemNoticeItem
import com.android.purebilibili.data.repository.MessageRepository
import com.android.purebilibili.feature.message.MessagePreviewParser
import com.android.purebilibili.navigation.ScreenRoutes
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

// Interaction polling queries unread counts; list viewing and acknowledgements stay in message pages.
internal interface MessageNotificationSource {
    suspend fun sessions(): List<SessionItem>
    suspend fun feedUnread(): MessageFeedUnreadData
    suspend fun systemNotices(): List<SystemNoticeItem>
    suspend fun dynamicUpdateCount(baseline: String): Int
    suspend fun followings(selfMid: Long, page: Int): FollowingsData
    suspend fun personalDynamicFeed(hostUid: Long, offset: String, page: Int): DynamicPersonalFeedPage
    suspend fun followedLive(page: Int): FollowedLiveData
}

private object NetworkMessageNotificationSource : MessageNotificationSource {
    override suspend fun sessions() = MessageRepository.getSessions(sessionType = 4, size = 20)
        .getOrThrow().session_list.orEmpty()
    override suspend fun feedUnread() = MessageRepository.getFeedUnread().getOrThrow()
    override suspend fun systemNotices() = MessageRepository.getSystemNotices().getOrThrow()
    override suspend fun dynamicUpdateCount(baseline: String): Int {
        val response = NetworkModule.dynamicApi.getDynamicUpdateCount(type = "all", updateBaseline = baseline)
        check(response.code == 0) { "Dynamic update code ${response.code}" }
        return checkNotNull(response.data).update_num
    }
    override suspend fun followings(selfMid: Long, page: Int): FollowingsData {
        val response = NetworkModule.api.getFollowings(vmid = selfMid, pn = page, ps = 50)
        check(response.code == 0) { "Followings code ${response.code}" }
        return checkNotNull(response.data)
    }
    override suspend fun personalDynamicFeed(hostUid: Long, offset: String, page: Int) =
        DynamicPersonalFeedApi.load(hostUid, offset, page)
    override suspend fun followedLive(page: Int): FollowedLiveData {
        val response = NetworkModule.api.getFollowedLive(page = page, pageSize = 10)
        check(response.code == 0) { "Followed live code ${response.code}" }
        // A missing payload is not evidence that every previously live room went offline.
        return checkNotNull(response.data)
    }
}

internal data class MessageNotificationCheckResult(
    val state: AccountNotificationState,
    val notifications: List<PendingMessageNotification>,
    val shouldRetry: Boolean,
)

private class NotificationSessionChanged : Exception()

internal class MessageNotificationPoller(
    private val source: MessageNotificationSource,
    private val ensureSession: () -> Unit = {},
) {
    private suspend fun <T> fetch(block: suspend () -> T): T {
        ensureSession()
        val result = block()
        ensureSession()
        return result
    }

    private suspend fun dynamicFollowingUsers(selfMid: Long): Map<Long, FollowingUser> {
        val users = linkedMapOf<Long, FollowingUser>()
        var page = 1
        var expectedTotal = 0
        while (true) {
            val response = fetch { source.followings(selfMid, page) }
            val before = users.size
            val rows = response.list.orEmpty()
            expectedTotal = maxOf(expectedTotal, response.total)
            for (user in rows) if (user.mid > 0L) users[user.mid] = user
            if (expectedTotal > 0 && users.size >= expectedTotal) break
            if (rows.size < 50) {
                check(expectedTotal == 0 || users.size >= expectedTotal) { "关注列表返回不完整" }
                break
            }
            check(users.size > before) { "关注列表分页未推进" }
            page++
        }
        users.remove(selfMid)
        return users
    }

    suspend fun check(
        settings: MessageNotificationSettings,
        previous: AccountNotificationState,
        selfMid: Long,
    ): MessageNotificationCheckResult {
        if (!settings.enabled) return MessageNotificationCheckResult(previous, emptyList(), false)
        var state = previous
        var shouldRetry = false
        val notifications = mutableListOf<PendingMessageNotification>()

        suspend fun category(id: String?, block: suspend () -> Unit): Boolean {
            val before = state
            val notificationCount = notifications.size
            try {
                block()
                if (id != null) state = state.copy(initialized = state.initialized + id)
                return true
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotificationSessionChanged) {
                throw e
            } catch (e: Exception) {
                state = before
                notifications.subList(notificationCount, notifications.size).clear()
                if (e is IOException) shouldRetry = true
                return false
            }
        }

        fun feed(id: String, unreadCount: Int, previousCount: Int?, notificationId: Int, route: String, title: String, text: (Int) -> String): Int {
            val count = unreadCount.coerceAtLeast(0)
            // A missing baseline (including after upgrading from ID-based polling) is silent.
            val increase = if (previousCount == null) 0 else (count - previousCount).coerceAtLeast(0)
            if (increase > 0) {
                notifications += PendingMessageNotification(id, notificationId, route, title, text(increase), "interactions")
            }
            return count
        }

        if (settings.notifyPrivateMessages) category("dm") {
            val sessions = fetch { source.sessions() }
            val fresh = if ("dm" in state.initialized) filterNewPrivateSessions(sessions, selfMid, state.sessionMsgKeys) else emptyList()
            for (session in fresh.take(5)) {
                val message = checkNotNull(session.last_msg)
                val name = session.account_info?.name?.takeIf { it.isNotBlank() } ?: "私信"
                val prefix = if (session.unread_count > 1) "[${session.unread_count} 条新消息] " else ""
                val key = sessionNotificationKey(session)
                notifications += PendingMessageNotification(
                    "dm:$key", 6000 + ("${session.talker_id}:${session.session_type}".hashCode() and 0xFFFF),
                    ScreenRoutes.Chat.createRoute(session.talker_id, session.session_type, name), name,
                    prefix + MessagePreviewParser.parseSessionPreview(message.content, message.msg_type), "dm",
                )
            }
            if (fresh.size > 5) notifications += PendingMessageNotification(
                "dm:summary", 5106, ScreenRoutes.Inbox.route, "新私信", "有 ${fresh.size} 个会话发来新消息", "dm",
                isGroupSummary = true,
            )
            val keys = LinkedHashMap(state.sessionMsgKeys)
            for (session in sessions.asReversed()) {
                val message = session.last_msg ?: continue
                if (session.talker_id <= 0 || message.msg_key <= 0) continue
                val key = sessionNotificationKey(session)
                keys.remove(key)
                keys[key] = message.msg_key
                if (keys.size > 100) keys.entries.iterator().apply { next(); remove() }
            }
            state = state.copy(sessionMsgKeys = keys)
        }
        if (settings.notifyReplies || settings.notifyAtMe || settings.notifyLikes) category(null) {
            // One read-only snapshot serves all enabled interaction categories. Failure rolls
            // back their baselines together without blocking private/system/dynamic/live checks.
            val unread = fetch { source.feedUnread() }
            state = state.copy(
                replyUnreadCount = if (settings.notifyReplies) {
                    feed("reply", unread.reply, state.replyUnreadCount,
                        5101, ScreenRoutes.ReplyMe.route, "回复我的") { "你有 $it 条新回复" }
                } else state.replyUnreadCount,
                atUnreadCount = if (settings.notifyAtMe) {
                    feed("at", unread.at, state.atUnreadCount,
                        5102, ScreenRoutes.AtMe.route, "@我") { "有 $it 条新消息提到你" }
                } else state.atUnreadCount,
                likeUnreadCount = if (settings.notifyLikes) {
                    feed("like", unread.like, state.likeUnreadCount,
                        5103, ScreenRoutes.LikeMe.route, "收到的赞") { "你的内容收到 $it 个新赞" }
                } else state.likeUnreadCount,
            )
        }
        if (settings.notifySystemNotices) category("sysmsg") {
            val items = fetch { source.systemNotices() }
            val known = state.seenSystemCursors.toHashSet()
            val latest = items.firstOrNull { it.cursor !in known }
            if ("sysmsg" in state.initialized && latest != null) notifications += PendingMessageNotification(
                "sysmsg", 5104, ScreenRoutes.SystemNotice.route, "系统通知", latest.title.ifBlank { latest.content }, "interactions",
            )
            state = state.copy(seenSystemCursors = mergeNotificationSeen(state.seenSystemCursors, items.map { it.cursor }, 100))
        }
        if (settings.notifyDynamicUpdates) category("dynamic") {
            // The full following set is required: an incomplete page must never look like an unfollow.
            val users = dynamicFollowingUsers(selfMid)
            if (users.isEmpty()) {
                state = state.copy(dynamicBaseline = "0", dynamicUpBaselines = emptyMap())
                return@category
            }
            val previousByUp = state.dynamicUpBaselines
            val retained = if (previousByUp.keys.all { it in users }) previousByUp
                else previousByUp.filterKeys { it in users }
            val hasUpdates = state.dynamicBaseline.isBlank() ||
                fetch { source.dynamicUpdateCount(state.dynamicBaseline) } > 0
            if (!hasUpdates && users.keys.all { it in retained }) {
                if (retained !== previousByUp) state = state.copy(dynamicUpBaselines = retained)
                return@category
            }
            val baselines = LinkedHashMap(retained)
            val previews = ArrayList<DynamicItem>(MAX_DYNAMIC_NOTIFICATION_PREVIEWS)
            var freshCount = 0
            var complete = true
            for ((mid, user) in users) {
                if (!hasUpdates && mid in retained) continue
                val success = category(null) {
                    val initializing = mid !in retained
                    val baseline = retained[mid].orEmpty()
                    var newest = baseline
                    var offset = ""
                    var pageNumber = 1
                    val offsets = hashSetOf("")
                    val seen = hashSetOf<String>()
                    val hostPreviews = ArrayList<DynamicItem>(MAX_DYNAMIC_NOTIFICATION_PREVIEWS)
                    var hostFreshCount = 0
                    while (true) {
                        val page = fetch { source.personalDynamicFeed(mid, offset, pageNumber) }
                        var lastRegularId: String? = null
                        for (item in page.items) {
                            if (item.type == "DYNAMIC_TYPE_AD" || item.type == "DYNAMIC_TYPE_BANNER" ||
                                item.type == "0" || item.type == "5" || item.id_str.isBlank()) continue
                            if (isNewerDynamicNotificationId(item.id_str, newest)) newest = item.id_str
                            if (item.modules.module_tag?.text != "置顶") lastRegularId = item.id_str
                            if (!initializing && isNewerDynamicNotificationId(item.id_str, baseline) &&
                                seen.add(item.id_str) && item.modules.module_author?.mid == mid &&
                                shouldNotifyDynamicItem(item, selfMid)) {
                                hostFreshCount++
                                addNewestDynamicNotificationPreview(hostPreviews, item)
                            }
                        }
                        // IDs are the numeric publish baseline; pinned old cards cannot stop a scan.
                        if (initializing || !page.hasMore ||
                            (lastRegularId != null && !isNewerDynamicNotificationId(lastRegularId, baseline))) break
                        check(page.offset.isNotBlank() && offsets.add(page.offset)) { "动态预加载分页未推进" }
                        offset = page.offset
                        pageNumber++
                    }
                    // Commit an UP only after its complete new range was fetched. A failed UP
                    // keeps its old baseline, while successful UPs need not replay their notices.
                    baselines[mid] = newest
                    freshCount += hostFreshCount
                    for (item in hostPreviews) addNewestDynamicNotificationPreview(previews, item)
                }
                if (!success) complete = false
            }
            var latest = state.dynamicBaseline.ifBlank { "0" }
            if (complete) {
                for (id in baselines.values) if (isNewerDynamicNotificationId(id, latest)) latest = id
            }
            state = state.copy(dynamicBaseline = latest, dynamicUpBaselines = baselines)
            for (item in previews) {
                val route = resolveDynamicNotificationRoute(item) ?: continue
                val author = item.modules.module_author?.name.orEmpty().ifBlank {
                    item.modules.module_author?.mid?.let { users[it]?.uname }.orEmpty().ifBlank { "关注的 UP 主" }
                }
                val archive = item.modules.module_dynamic?.major?.archive
                notifications += PendingMessageNotification(
                    "dynamic:${item.id_str}", 8000 + (item.id_str.hashCode() and 0xFFFF), route,
                    "$author 发布了${if (archive != null) "新视频" else "新动态"}", dynamicNotificationText(item), "dynamic",
                )
            }
            if (freshCount > MAX_DYNAMIC_NOTIFICATION_PREVIEWS) notifications += PendingMessageNotification(
                "dynamic:summary", 5105, ScreenRoutes.Dynamic.route, "关注更新", "有 $freshCount 条关注更新", "dynamic",
                isGroupSummary = true,
            )
        }
        if (settings.notifyLiveAlerts) category("live") {
            val living = linkedMapOf<Long, FollowedLiveRoom>()
            var complete = false
            for (page in 1..10) {
                val data = fetch { source.followedLive(page) }
                val rooms = data.list.orEmpty()
                for (room in rooms) if (room.liveStatus == 1) living.putIfAbsent(room.roomid, room)
                if (rooms.isEmpty() || data.livingNum == 0 || living.size >= data.livingNum ||
                    (data.pageinfo?.total_page?.let { it > 0 && page >= it } == true)) {
                    complete = true
                    break
                }
            }
            val (fresh, current) = resolveLiveSessionTransitions(living.values.toList(), state.liveSessions, "live" in state.initialized)
            for (room in fresh.take(5)) notifications += PendingMessageNotification(
                "live:${room.uid}", 7000 + (room.uid % 100000).toInt(),
                ScreenRoutes.Live.createRoute(room.roomid, room.title, room.uname), "开播提醒",
                "${room.uname} 正在直播" + if (room.title.isBlank()) "" else "：${room.title}", "live",
            )
            // Hitting the page bound is not an offline transition for rooms outside the scan.
            state = state.copy(liveSessions = if (complete) current else state.liveSessions + current)
        }
        return MessageNotificationCheckResult(state, notifications, shouldRetry)
    }
}

internal class MessageNotificationChecker(private val context: Context) {
    suspend fun runCheck(): Boolean = messageNotificationMutex.withLock {
        val settings = MessageNotificationSettingsStore.getSettings(context).first()
        if (!settings.enabled || !MessageNotificationNotifier.canPost(context)) return@withLock true
        val mid = TokenManager.midCache?.takeIf { it > 0 } ?: return@withLock true
        val session = TokenManager.sessDataCache?.takeIf { it.isNotBlank() } ?: return@withLock true
        fun isSameSession() = TokenManager.midCache == mid && TokenManager.sessDataCache == session
        val result = try {
            MessageNotificationPoller(NetworkMessageNotificationSource) {
                if (!isSameSession()) throw NotificationSessionChanged()
            }.check(settings, MessageNotificationStateStore.getState(context, mid), mid)
        } catch (_: NotificationSessionChanged) {
            return@withLock true
        }
        if (!isSameSession() || MessageNotificationSettingsStore.getSettings(context).first() != settings ||
            !MessageNotificationNotifier.canPost(context)) return@withLock true
        MessageNotificationStateStore.putState(context, mid, result.state)
        MessageNotificationNotifier.postAll(context, mid, result.notifications, ::isSameSession)
        !result.shouldRetry
    }
}
