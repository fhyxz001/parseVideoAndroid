package com.videoparser.app

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var videoUrl by mutableStateOf("")
    var isParsing by mutableStateOf(false)
    var parseData by mutableStateOf<VideoData?>(null)
    var isDownloading by mutableStateOf(false)
    var downloadPercent by mutableIntStateOf(0)
    var downloadedBytes by mutableLongStateOf(0L)
    var downloadTotalBytes by mutableLongStateOf(0L)
    var videoFileSize by mutableLongStateOf(0L)
    var isSavingCover by mutableStateOf(false)
    var showCoverSaveDialog by mutableStateOf(false)

    // 服务器设置
    var serverUrl by mutableStateOf("")
    var serverDraft by mutableStateOf("")
    var showServerSettings by mutableStateOf(false)

    // 剪辑
    var showClipPage by mutableStateOf(false)
    var clipProbing by mutableStateOf(false)
    var clipDurationMs by mutableLongStateOf(0L)
    var clipStartMs by mutableLongStateOf(0L)
    var clipEndMs by mutableLongStateOf(0L)
    var isClipping by mutableStateOf(false)
    var clipPhaseText by mutableStateOf("")
    var clipPercent by mutableIntStateOf(0)
    var clipThumbs by mutableStateOf<List<ImageBitmap?>>(emptyList())
    var clipThumbsLoading by mutableStateOf(false)

    // 首帧/尾帧预览
    var clipStartFrame by mutableStateOf<ImageBitmap?>(null)
    var clipEndFrame by mutableStateOf<ImageBitmap?>(null)

    // 帧图片保存确认弹窗
    var showFrameSaveDialog by mutableStateOf(false)
    var frameSaveIsStart by mutableStateOf(true)
    var isSavingFrame by mutableStateOf(false)

    private var clipProbe: ClipProbe? = null
    private var clipProbeKey: String? = null
    private var clipJob: Job? = null
    private var clipThumbsKey: String? = null

    // 取帧专用（单实例复用，避免每次 setDataSource 重新建连；串行取帧防止并发崩溃）
    private var frameRetriever: MediaMetadataRetriever? = null
    private var frameUrl: String? = null
    private var pendingStartMs: Long? = null
    private var pendingEndMs: Long? = null
    private var lastLoadedStartMs = Long.MIN_VALUE
    private var lastLoadedEndMs = Long.MIN_VALUE
    private val frameMutex = Mutex()

    val toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val copyEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)

    private fun toast(message: String) {
        toastEvents.tryEmit(message)
    }

    fun onUrlChanged(value: String) {
        videoUrl = value
    }

    fun onPasted(text: String?) {
        if (!text.isNullOrBlank()) {
            videoUrl = text
            parseData = null
            videoFileSize = 0L
        } else {
            toast("剪贴板为空")
        }
    }

    init {
        serverUrl = SettingsStore.getServer(getApplication())
    }

    fun openServerSettings() {
        serverDraft = serverUrl
        showServerSettings = true
    }

    fun dismissServerSettings() {
        showServerSettings = false
    }

    fun onServerDraftChanged(value: String) {
        serverDraft = value
    }

    fun saveServerSettings() {
        val value = serverDraft.trim().trimEnd('/')
        when {
            value.isBlank() -> toast("服务器地址不能为空")
            !value.startsWith("http://") && !value.startsWith("https://") ->
                toast("地址需以 http:// 或 https:// 开头")
            else -> {
                SettingsStore.setServer(getApplication(), value)
                serverUrl = value
                showServerSettings = false
                toast("服务器地址已保存")
            }
        }
    }

    fun resetServerSettings() {
        serverDraft = VideoApi.DEFAULT_SERVER
        toast("已填入默认地址，点击保存生效")
    }

    fun parse() {
        val url = videoUrl.trim()
        if (url.isEmpty()) {
            toast("请输入链接")
            return
        }
        isParsing = true
        parseData = null
        videoFileSize = 0L
        resetClipState()
        viewModelScope.launch {
            when (val result = VideoApi.parse(url, serverUrl)) {
                is ParseResult.Success -> {
                    parseData = result.data
                    toast("解析成功")
                    fetchVideoFileSize(result.data.videoUrl)
                    ensureClipProbe() // 提前探测时长，供卡片展示与剪辑功能
                }
                is ParseResult.Error -> toast(result.message)
            }
            isParsing = false
        }
    }

    /**
     * 「粘贴并解析」按钮：输入框有内容时以输入框为准直接解析；
     * 输入框为空时读取剪贴板链接，填入后解析。
     */
    fun pasteAndParse() {
        if (videoUrl.isNotBlank()) {
            parse()
            return
        }
        val cm = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
        onPasted(text) // 填入输入框（剪贴板为空时内部会 toast 提示）
        if (videoUrl.isNotBlank()) {
            parse()
        }
    }

    fun download() {
        if (isDownloading) return
        val data = parseData
        if (data == null || data.videoUrl.isBlank()) {
            toast("没有可下载的视频")
            return
        }
        isDownloading = true
        downloadPercent = 0
        downloadedBytes = 0L
        downloadTotalBytes = 0L
        viewModelScope.launch {
            val title = data.title.ifBlank { "video" }
            val ok = VideoDownloader.download(
                getApplication(),
                data.videoUrl.trim(),
                title
            ) { progress ->
                downloadPercent = progress.percent
                downloadedBytes = progress.downloadedBytes
                downloadTotalBytes = if (progress.totalBytes > 0) {
                    progress.totalBytes
                } else {
                    videoFileSize
                }
            }
            isDownloading = false
            toast(if (ok) "下载完成" else "下载失败")
        }
    }

    private fun fetchVideoFileSize(url: String) {
        if (url.isBlank()) return
        videoFileSize = 0L
        val targetUrl = url.trim()
        viewModelScope.launch {
            val size = VideoDownloader.fetchVideoSize(targetUrl)
            // 防止异步返回时用户已经解析了新的视频
            if (parseData?.videoUrl?.trim() == targetUrl) {
                videoFileSize = size
            }
        }
    }

    fun onCoverClicked() {
        val data = parseData
        if (data == null || data.coverUrl.isBlank()) {
            toast("没有可保存的封面")
            return
        }
        showCoverSaveDialog = true
    }

    fun dismissCoverSaveDialog() {
        showCoverSaveDialog = false
    }

    fun confirmSaveCover() {
        if (isSavingCover) return
        val data = parseData
        if (data == null || data.coverUrl.isBlank()) return
        showCoverSaveDialog = false
        isSavingCover = true
        viewModelScope.launch {
            val title = data.title.ifBlank { "cover" }
            val ok = VideoDownloader.downloadImage(
                getApplication(),
                data.coverUrl.trim(),
                title
            )
            isSavingCover = false
            toast(if (ok) "封面已保存到相册" else "封面保存失败")
        }
    }

    fun copyTitle() {
        val data = parseData
        if (data == null || data.title.isBlank()) {
            toast("没有可复制的标题")
            return
        }
        copyEvents.tryEmit(data.title)
        toast("标题已复制")
    }

    fun copyAuthorName() {
        val data = parseData
        val name = data?.author?.name
        if (name.isNullOrBlank()) {
            toast("没有可复制的作者名称")
            return
        }
        copyEvents.tryEmit(name)
        toast("作者名称已复制")
    }

    /* ═════════════════════ 剪辑 ═════════════════════ */

    private fun resetClipState() {
        releaseFrameRetriever()
        clipProbe = null
        clipProbeKey = null
        clipThumbsKey = null
        clipProbing = false
        clipDurationMs = 0L
        clipStartMs = 0L
        clipEndMs = 0L
        clipThumbs = emptyList()
        clipThumbsLoading = false
        isClipping = false
        clipPercent = 0
        clipPhaseText = ""
        showFrameSaveDialog = false
        isSavingFrame = false
    }

    fun openClipPage() {
        if (parseData == null) {
            toast("请先解析视频")
            return
        }
        showClipPage = true
        ensureClipProbe()
        ensureClipThumbs()
    }

    fun dismissClipPage() {
        if (isClipping) toast("剪辑仍在后台进行，完成后会提示")
        releaseFrameRetriever()
        showClipPage = false
    }

    /** 探测视频元数据（时长 + 采样索引），按 URL 缓存，解析成功后会自动预探测 */
    private fun ensureClipProbe() {
        val data = parseData ?: return
        val key = data.videoUrl.trim()
        if (key.isBlank()) return
        if (clipProbeKey == key && clipProbe != null) return
        clipProbeKey = key
        clipProbe = null
        clipDurationMs = 0L
        clipProbing = true
        viewModelScope.launch {
            val probed = VideoClipper.probe(key)
            // 防止异步返回时用户已解析了新的视频
            if (clipProbeKey == key) {
                clipProbe = probed
                clipDurationMs = when (probed) {
                    is ClipProbe.Ready -> probed.durationMs
                    is ClipProbe.Fallback -> probed.durationMs
                }
                if (clipDurationMs > 0) {
                    // 默认选择前 10 分钟（不足则全选）
                    clipStartMs = 0L
                    clipEndMs = minOf(clipDurationMs, DEFAULT_CLIP_WINDOW_MS)
                }
                clipProbing = false
                ensureClipThumbs()
            }
        }
    }

    /** 关键帧吸附后的实际剪辑窗口，UI 用于展示“实际将剪出哪一段” */
    fun snappedClipWindow(): Pair<Long, Long> {
        val probe = clipProbe ?: return clipStartMs to clipEndMs
        return VideoClipper.snappedWindowMs(probe, clipStartMs, clipEndMs)
    }

    /** 拖动滑块设置起止时间（毫秒），自动钳制顺序与最小间隔 */
    fun setClipRange(startMs: Long, endMs: Long) {
        val max = if (clipDurationMs > 0) clipDurationMs else endMs
        var s = startMs.coerceIn(0L, max)
        var e = endMs.coerceIn(0L, max)
        if (e - s < MIN_CLIP_SPAN_MS) {
            // 保持拖动的哪一端，另一端让位
            if (s != clipStartMs) s = (e - MIN_CLIP_SPAN_MS).coerceAtLeast(0L)
            else e = (s + MIN_CLIP_SPAN_MS).coerceAtMost(max)
        }
        clipStartMs = s
        clipEndMs = e
    }

    fun nudgeClipStart(deltaMs: Long) = setClipRange(clipStartMs + deltaMs, clipEndMs)

    fun nudgeClipEnd(deltaMs: Long) = setClipRange(clipStartMs, clipEndMs + deltaMs)

    fun startClipDownload() {
        if (isClipping) return
        val data = parseData ?: return
        val url = data.videoUrl.trim()
        val probe = clipProbe
        if (url.isBlank() || probe == null) {
            toast("视频信息未就绪，请稍候")
            return
        }
        if (clipDurationMs <= 0) {
            toast("无法获取视频时长，暂不支持剪辑")
            return
        }
        if (clipEndMs - clipStartMs < MIN_CLIP_SPAN_MS) {
            toast("请选择至少 1 秒的片段")
            return
        }
        isClipping = true
        clipPercent = 0
        clipPhaseText = "准备剪辑..."
        clipJob = viewModelScope.launch {
            try {
                val result = VideoClipper.clipToGallery(
                    getApplication(),
                    url,
                    data.title.ifBlank { "video" },
                    clipStartMs,
                    clipEndMs,
                    probe,
                    onPhase = { clipPhaseText = phaseText(it) },
                    onPercent = { clipPercent = it }
                )
                when (result) {
                    is ClipResult.Success -> {
                        showClipPage = false
                        toast("剪辑已保存到相册")
                    }
                    is ClipResult.Failure -> toast(result.message)
                }
            } catch (e: CancellationException) {
                toast("已取消剪辑")
                throw e
            } finally {
                isClipping = false
                clipPhaseText = ""
            }
        }
    }

    fun cancelClip() {
        clipJob?.cancel()
        clipJob = null
    }

    private fun phaseText(phase: ClipPhase): String = when (phase) {
        ClipPhase.DOWNLOADING_SEGMENT -> "正在下载所选片段..."
        ClipPhase.BUILDING -> "正在生成剪辑文件..."
        ClipPhase.DOWNLOADING_FULL -> "服务器不支持分段下载，正在下载完整视频..."
        ClipPhase.TRIMMING -> "正在剪切视频..."
        ClipPhase.SAVING -> "正在保存到相册..."
    }

    /* ══════════════ 首帧/尾帧预览取帧 ══════════════ */

    /**
     * 开始/结束时间变化后请求对应帧（拖动、步进、点击都会触发）。
     * 后台串行加载并自动合并：拖动过程中始终只取最新时间，不堆积请求。
     */
    fun requestClipFrame(isStart: Boolean, timeMs: Long) {
        if (isStart) {
            if (timeMs == lastLoadedStartMs || timeMs == pendingStartMs) return
            pendingStartMs = timeMs
        } else {
            if (timeMs == lastLoadedEndMs || timeMs == pendingEndMs) return
            pendingEndMs = timeMs
        }
        runClipFrameLoader()
    }

    /** 串行取帧循环：同一时刻只允许一个 getFrameAtTime 在跑 */
    private fun runClipFrameLoader() {
        val url = parseData?.videoUrl?.trim() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            frameMutex.withLock {
                while (true) {
                    val startTarget = pendingStartMs
                    val endTarget = pendingEndMs
                    if (startTarget == null && endTarget == null) break
                    val retriever = ensureFrameRetriever(url) ?: break
                    if (startTarget != null) {
                        pendingStartMs = null
                        val bmp = getFrame(retriever, startTarget)
                        // 加载期间有更新的请求则丢弃旧结果，避免回闪过期帧
                        if (bmp != null && pendingStartMs == null) {
                            clipStartFrame = scaleDown(bmp, PREVIEW_TARGET_WIDTH).asImageBitmap()
                            lastLoadedStartMs = startTarget
                        }
                    }
                    if (endTarget != null) {
                        pendingEndMs = null
                        val bmp = getFrame(retriever, endTarget)
                        if (bmp != null && pendingEndMs == null) {
                            clipEndFrame = scaleDown(bmp, PREVIEW_TARGET_WIDTH).asImageBitmap()
                            lastLoadedEndMs = endTarget
                        }
                    }
                }
            }
        }
    }

    private suspend fun getFrame(retriever: MediaMetadataRetriever, timeMs: Long): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (e: Exception) {
                null
            }
        }

    /** 返回按 URL 复用的取帧器，失败返回 null */
    private fun ensureFrameRetriever(url: String): MediaMetadataRetriever? {
        if (frameRetriever != null && frameUrl == url) return frameRetriever
        releaseFrameRetriever()
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(url, HashMap<String, String>())
            frameRetriever = retriever
            frameUrl = url
            return retriever
        } catch (e: Exception) {
            frameRetriever = null
            return null
        }
    }

    private fun releaseFrameRetriever() {
        pendingStartMs = null
        pendingEndMs = null
        lastLoadedStartMs = Long.MIN_VALUE
        lastLoadedEndMs = Long.MIN_VALUE
        val retriever = frameRetriever
        frameRetriever = null
        frameUrl = null
        clipStartFrame = null
        clipEndFrame = null
        retriever?.let {
            try {
                it.release()
            } catch (e: Exception) {
            }
        }
    }

    /** 点击预览图：弹出保存确认（帧未就绪时提示等待） */
    fun onClipFrameClicked(isStart: Boolean) {
        val frame = if (isStart) clipStartFrame else clipEndFrame
        if (frame == null) {
            toast("画面加载中，请稍候")
            return
        }
        frameSaveIsStart = isStart
        showFrameSaveDialog = true
    }

    fun dismissFrameSaveDialog() {
        showFrameSaveDialog = false
    }

    fun confirmSaveFrame() {
        if (isSavingFrame) return
        val bitmap = (if (frameSaveIsStart) clipStartFrame else clipEndFrame)?.asAndroidBitmap() ?: return
        showFrameSaveDialog = false
        isSavingFrame = true
        viewModelScope.launch {
            val title = parseData?.title?.ifBlank { "video" } ?: "video"
            val fileName = frameFileName(title, frameSaveIsStart, if (frameSaveIsStart) clipStartMs else clipEndMs)
            val ok = VideoDownloader.saveBitmapToGallery(getApplication(), bitmap, fileName)
            isSavingFrame = false
            toast(if (ok) "已保存到相册" else "保存失败")
        }
    }

    /** 帧图片文件名：标题_剪辑首帧/尾帧_HHmmss.jpg */
    private fun frameFileName(title: String, isStart: Boolean, timeMs: Long): String {
        val base = title.replace(Regex("[\\\\/:*?\"<>|]"), "").ifBlank { "video" }.take(60)
        val side = if (isStart) "首帧" else "尾帧"
        val totalSec = timeMs.coerceAtLeast(0L) / 1000
        val t = String.format(
            Locale.US, "%02d%02d%02d",
            totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60
        )
        return "${base}_剪辑${side}_$t.jpg"
    }

    /** 加载时间轴缩略图（3 路并行取关键帧，逐张更新） */
    private fun ensureClipThumbs() {
        val data = parseData ?: return
        val key = data.videoUrl.trim()
        if (key.isBlank()) return
        if (clipThumbsKey == key && clipThumbs.isNotEmpty()) return
        if (clipDurationMs <= 0) return // 等探测完成后再加载
        clipThumbsKey = key
        clipThumbsLoading = true
        clipThumbs = List(CLIP_THUMB_COUNT) { null }
        viewModelScope.launch(Dispatchers.IO) {
            val frames = loadClipThumbnails(key, clipDurationMs, CLIP_THUMB_COUNT)
            if (clipThumbsKey == key) {
                clipThumbs = frames
                clipThumbsLoading = false
            }
        }
    }

    private suspend fun loadClipThumbnails(
        url: String,
        durationMs: Long,
        count: Int
    ): List<ImageBitmap?> = coroutineScope {
        val retrievers = (0 until THUMB_WORKERS).map { MediaMetadataRetriever() }
        try {
            retrievers.forEach { it.setDataSource(url, HashMap<String, String>()) }
            val results = arrayOfNulls<ImageBitmap>(count)
            (0 until THUMB_WORKERS).map { worker ->
                async {
                    val retriever = retrievers[worker]
                    for (i in worker until count step THUMB_WORKERS) {
                        val posUs = durationMs * 1000 * (i + 0.5) / count
                        val frame = try {
                            retriever.getFrameAtTime(
                                posUs.toLong(),
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
                        } catch (e: Exception) {
                            null
                        }
                        results[i] = frame?.let { scaleDown(it, THUMB_TARGET_WIDTH) }?.asImageBitmap()
                        clipThumbs = results.toList() // 逐张刷新占位
                    }
                }
            }.awaitAll()
            results.toList()
        } catch (e: Exception) {
            List(count) { null }
        } finally {
            retrievers.forEach { retriever ->
                try {
                    retriever.release()
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun scaleDown(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width <= targetWidth) return bitmap
        val ratio = targetWidth.toFloat() / bitmap.width
        return Bitmap.createScaledBitmap(
            bitmap,
            targetWidth,
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    companion object {
        private const val DEFAULT_CLIP_WINDOW_MS = 10 * 60 * 1000L
        private const val MIN_CLIP_SPAN_MS = 1000L
        private const val CLIP_THUMB_COUNT = 16
        private const val THUMB_WORKERS = 3
        private const val THUMB_TARGET_WIDTH = 320
        private const val PREVIEW_TARGET_WIDTH = 480
    }
}
