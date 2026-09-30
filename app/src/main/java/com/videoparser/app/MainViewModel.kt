package com.videoparser.app

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ImageBitmap
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
import kotlinx.coroutines.withContext

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
    var showClipSheet by mutableStateOf(false)
    var clipProbing by mutableStateOf(false)
    var clipDurationMs by mutableLongStateOf(0L)
    var clipStartMs by mutableLongStateOf(0L)
    var clipEndMs by mutableLongStateOf(0L)
    var isClipping by mutableStateOf(false)
    var clipPhaseText by mutableStateOf("")
    var clipPercent by mutableIntStateOf(0)
    var clipThumbs by mutableStateOf<List<ImageBitmap?>>(emptyList())
    var clipThumbsLoading by mutableStateOf(false)

    private var clipProbe: ClipProbe? = null
    private var clipProbeKey: String? = null
    private var clipJob: Job? = null
    private var clipThumbsKey: String? = null

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
    }

    fun openClipSheet() {
        if (parseData == null) {
            toast("请先解析视频")
            return
        }
        showClipSheet = true
        ensureClipProbe()
        ensureClipThumbs()
    }

    fun dismissClipSheet() {
        if (isClipping) toast("剪辑仍在后台进行，完成后会提示")
        showClipSheet = false
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
                        showClipSheet = false
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
        private const val CLIP_THUMB_COUNT = 8
        private const val THUMB_WORKERS = 3
        private const val THUMB_TARGET_WIDTH = 320
    }
}
