package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicCommentStructureTest {

    @Test
    fun `comment input is compact across materials and guards IME submission`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val composer = source
            .substringAfter("private fun DynamicCommentComposer(")
            .substringBefore("/**\n *  单条评论项")

        assertTrue(composer.contains("AppCompactInputField("))
        assertTrue(composer.contains("height = 40.dp"))
        assertTrue(!composer.contains("OutlinedTextField("))
        assertTrue(composer.contains("BasicTextField("))
        assertTrue(composer.contains("focusRequester = focusRequester"))
        assertTrue(composer.contains("KeyboardOptions(imeAction = ImeAction.Send)"))
        assertTrue(composer.contains("KeyboardActions(onSend = { submit() })"))
        assertTrue(composer.contains("if (isSending || (value.isBlank() && selectedImages.isEmpty())) return"))
        assertTrue(composer.contains("AppFilledIconButton("))
        assertTrue(composer.contains("stateDescription = \"正在发送\""))
    }

    @Test
    fun `dynamic comments expose sort mode state and reset pagination when it changes`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
            .readText()

        assertTrue(source.contains("val dynamicCommentSortMode: StateFlow<CommentSortMode>"))
        assertTrue(source.contains("fun setDynamicCommentSortMode(mode: CommentSortMode)"))
        assertTrue(source.contains("hasMore = false, nextPage = 1"))
        assertTrue(source.contains("commentGrpcNextOffset = null"))
        assertTrue(source.contains("loadCommentsForDynamic(item)"))
    }

    @Test
    fun `dynamic sub replies use server sorting and cursor paging`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
            .readText()

        assertTrue(source.contains("paginationOffset = request.offset"))
        assertTrue(source.contains("CommentRepository.getMobileSortedSubCommentsForSubject("))
        assertTrue(source.contains("mode = sortMode.apiMode"))
        assertTrue(source.contains("grpcNextOffset = data.grpcNextOffset"))
    }

    @Test
    fun `dynamic detail reloads comments for each screen entry`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt")
            .readText()

        assertTrue(source.contains("interactionViewModel.openCommentSheet("))
        assertTrue(!source.contains("hasAutoOpenedComments"))
        assertTrue(!source.contains("rememberSaveable(\n        dynamicId"))
    }

    @Test
    fun `dynamic detail pins the comment composer outside the scrolling list`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt")
            .readText()
        val commentContent = source
            .substringAfter("val commentContent:")
            .substringBefore("val commentComposer:")

        assertTrue(source.contains("DynamicInlineCommentComposer("))
        assertTrue(source.contains("shouldLoadMoreDynamicDetailComments("))
        assertTrue(source.contains("LaunchedEffect(detailListState, commentListState, useSplitLayout)"))
        assertTrue(commentContent.contains("dynamicInlineCommentItems("))
        assertTrue(!commentContent.contains("DynamicInlineCommentComposer("))
        assertTrue(!source.contains("item(key = \"dynamic_detail_comment_composer\")"))
    }

    @Test
    fun `dynamic detail comment composer becomes a liquid dock when enabled`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt")
            .readText()
        val composer = source
            .substringAfter("val commentComposer:")
            .substringBefore("if (useSplitLayout)")

        assertTrue(!source.contains("detailDockBackdrop"))
        assertTrue(!composer.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(composer.contains("liquidGlassEnabled = liquidGlassEnabled"))
        assertTrue(source.contains("shouldUseFloatingLiquidBottomInputBar("))
        assertTrue(source.contains("resolveBottomInputBarContentBottomPadding("))
        assertTrue(source.contains("if (liquidGlassEnabled) rememberChromeBackdropSource() else null"))
        assertTrue(source.contains("detailCommentSource.modifier.sharedGlobalWallpaperBackground(MaterialTheme.colorScheme.background)"))
        assertTrue(!source.contains(".layerBackdrop(detailCommentBackdrop)"))
        assertTrue(source.contains("bottom = commentContentBottomPadding"))

        val componentSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val inputComposer = componentSource
            .substringAfter("private fun DynamicCommentComposer(")
            .substringBefore("/**\n *  单条评论项")
        assertTrue(inputComposer.split("BottomBarMatchedReusableLiquidDock(").size - 1 == 2)
        assertTrue(inputComposer.contains("reuseEnabled = useLiquidGlassComposer"))
        assertTrue(inputComposer.contains("backdrop = backdrop"))
        assertTrue(inputComposer.contains("drawShellLens = true"))
        assertTrue(inputComposer.contains("shellLensIntensity = composerLensIntensity"))
        assertTrue(!inputComposer.contains("protectTextInputBackground"))
        assertTrue(inputComposer.contains("AppFilledIconButton("))
        assertTrue(inputComposer.contains("AppCircularProgressIndicator("))
        assertTrue(inputComposer.contains("leadingIcon = photoAction"))
        assertTrue(inputComposer.contains("readOnly = isSending"))

    }

    @Test
    fun `dynamic comment reload cancels and ignores the previous request`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
            .readText()

        assertTrue(source.contains("private var commentLoadJob: Job? = null"))
        assertTrue(source.contains("val requestId = ++commentLoadRequestId"))
        assertTrue(source.contains("commentLoadJob?.cancel()"))
        assertTrue(source.contains("if (requestId != commentLoadRequestId) return@launch"))
    }

    @Test
    fun `dynamic comment mentions forward user navigation through every comment surface`() {
        val sheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val subReplySource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSubReplyPreviewHost.kt"
        ).readText()
        val screenSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt"
        ).readText()
        val detailSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt"
        ).readText()

        assertTrue(sheetSource.contains("content = reply.content"))
        assertTrue(sheetSource.contains("content = subReply.content"))
        assertTrue(sheetSource.contains("SubReplyDetailContent("))
        assertTrue(sheetSource.contains("onUserClick = onUserClick"))
        assertTrue(sheetSource.contains("onAvatarClick = { mid -> mid.toLongOrNull()?.let(onUserClick) }"))
        assertTrue(subReplySource.contains("onAvatarClick = { mid -> mid.toLongOrNull()?.let(onUserClick) }"))
        assertTrue(subReplySource.contains("DynamicEmoteCatalog.ensureLoaded()"))
        assertTrue(subReplySource.contains("emoteMap = emoteMap"))
        assertTrue(!subReplySource.contains("emoteMap = emptyMap()"))
        assertTrue(screenSource.contains("DynamicCommentOverlayHost("))
        assertTrue(screenSource.contains("onUserClick = onUserClick"))
        assertTrue(detailSource.contains("dynamicInlineCommentItems("))
        assertTrue(detailSource.contains("onUserClick = onUserClick"))
    }

    @Test
    fun `feed and space comment buttons open dynamic detail`() {
        val screenSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt"
        ).readText()
        val spaceSource = File(
            "src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt"
        ).readText()
        val topicSource = File(
            "src/main/java/com/android/purebilibili/feature/search/TopicDetailScreen.kt"
        ).readText()

        assertTrue(screenSource.contains("onCommentClick = onDynamicDetailClick"))
        assertTrue(!screenSource.contains("onCommentClick = { viewModel.openCommentSheet(it) }"))
        assertTrue(spaceSource.contains("onCommentClick = { onDynamicDetailClick(dynamic.id_str) }"))
        assertTrue(topicSource.contains("onCommentClick = onDynamicDetailClick"))
    }
}
