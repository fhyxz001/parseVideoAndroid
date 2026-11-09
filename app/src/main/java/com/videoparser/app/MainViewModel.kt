package com.videoparser.app

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

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

    /** 当前选中的快捷截取时长（秒）；0 表示自定义 */
    var clipPresetSec by mutableIntStateOf(DEFAULT_CLIP_PRESET_SEC)

    /** 时长是否处于自定义状态（输入自定义秒数后置位） */
    var clipCustomDuration by mutableStateOf(false)

    /** 剪辑成功后的结果文件名；非空表示剪辑页展示完成状态 */
    var clipResultFileName by mutableStateOf<String?>(null)

    private var clipProbe: ClipProbe? = null
    private var clipProbeKey: String? = null
    private var clipJob: Job? = null

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
            value.isNotEmpty() &&
                !value.startsWith("http://") && !value.startsWith("https://") ->
                toast("地址需以 http:// 或 https:// 开头")
            else -> {
                SettingsStore.setServer(getApplication(), value)
                serverUrl = value
                showServerSettings = false
                toast(if (value.isEmpty()) "已清空服务器地址" else "服务器地址已保存")
            }
        }
    }

    fun parse() {
        val url = videoUrl.trim()
        if (url.isEmpty()) {
            toast("请输入链接")
            return
        }
        if (serverUrl.isBlank()) {
            toast("请先设置解析服务器地址")
            openServerSettings()
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
            // 防止异步返回时用户已经解析了新的视频；失败(-1)时不要覆盖剪辑探测拿到的有效大小
            if (size > 0 && parseData?.videoUrl?.trim() == targetUrl) {
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
        clipProbing = false
        clipDurationMs = 0L
        clipStartMs = 0L
        clipEndMs = 0L
        clipPresetSec = DEFAULT_CLIP_PRESET_SEC
        clipCustomDuration = false
        clipResultFileName = null
        isClipping = false
        clipPercent = 0
        clipPhaseText = ""
    }

    fun openClipPage() {
        if (parseData == null) {
            toast("请先解析视频")
            return
        }
        showClipPage = true
        ensureClipProbe()
    }

    fun dismissClipPage() {
        if (isClipping) toast("剪辑仍在后台进行，完成后会提示")
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
            // 已拿到文件大小时直接复用，省去一次网络请求
            val probed = VideoClipper.probe(key, knownFileSize = videoFileSize)
            // 防止异步返回时用户已解析了新的视频
            if (clipProbeKey == key) {
                clipProbe = probed
                clipDurationMs = when (probed) {
                    is ClipProbe.Ready -> probed.durationMs
                    is ClipProbe.Fallback -> probed.durationMs
                }
                if (clipDurationMs > 0) {
                    // 默认快速截取：从视频开头起，选中默认时长（不足则截到视频末尾）
                    clipPresetSec = DEFAULT_CLIP_PRESET_SEC
                    clipCustomDuration = false
                    clipStartMs = 0L
                    clipEndMs = minOf(clipDurationMs, DEFAULT_CLIP_PRESET_SEC * 1000L)
                    if (clipEndMs - clipStartMs < MIN_CLIP_SPAN_MS) {
                        clipStartMs = (clipEndMs - MIN_CLIP_SPAN_MS).coerceAtLeast(0L)
                    }
                }
                // 探测顺带拿到文件大小则回填，避免主界面大小一直空白
                val probedSize = when (probed) {
                    is ClipProbe.Ready -> probed.fileSize
                    is ClipProbe.Fallback -> probed.fileSize
                }
                if (videoFileSize <= 0 && probedSize > 0) {
                    videoFileSize = probedSize
                }
                clipProbing = false
            }
        }
    }

    /**
     * 选中快捷截取时长：保持开始时间不变，按所选秒数重算结束时间。
     * 超出视频末尾时截到末尾（此时实际时长小于所选时长，界面按实际时长展示）。
     */
    fun applyClipPreset(seconds: Int) {
        if (clipDurationMs <= 0 || seconds <= 0) return
        clipPresetSec = seconds
        clipCustomDuration = false
        applySpanMs(seconds * 1000L)
    }

    /** 自定义截取时长（秒）：与快捷时长同样联动，但标记为自定义状态 */
    fun applyClipCustomDuration(seconds: Double) {
        if (clipDurationMs <= 0 || seconds <= 0) return
        clipPresetSec = 0
        clipCustomDuration = true
        applySpanMs((seconds * 1000).roundToLong())
    }

    /** 保持开始时间，按给定跨度设置结束时间（钳制到视频范围并保证最小间隔） */
    private fun applySpanMs(spanMs: Long) {
        val span = spanMs.coerceAtLeast(MIN_CLIP_SPAN_MS)
        clipEndMs = (clipStartMs + span).coerceAtMost(clipDurationMs)
        if (clipEndMs - clipStartMs < MIN_CLIP_SPAN_MS) {
            clipStartMs = (clipEndMs - MIN_CLIP_SPAN_MS).coerceAtLeast(0L)
        }
    }

    /**
     * 调整开始时间（时/分/秒输入）：保持当前截取时长，结束时间联动。
     * 剩余长度不足当前时长时，结束时间贴到视频末尾，实际时长随之缩短。
     */
    fun setClipStartKeepingDuration(posMs: Long) {
        if (clipDurationMs <= 0) return
        // 视频本身不足 1 秒时，下限退化为视频时长，避免 coerceIn 区间非法
        val minSpan = minOf(MIN_CLIP_SPAN_MS, clipDurationMs)
        val span = (clipEndMs - clipStartMs).coerceIn(minSpan, clipDurationMs)
        val s = posMs.coerceIn(0L, (clipDurationMs - span).coerceAtLeast(0L))
        clipStartMs = s
        clipEndMs = (s + span).coerceAtMost(clipDurationMs)
        if (clipEndMs - clipStartMs < minSpan) {
            clipStartMs = (clipEndMs - minSpan).coerceAtLeast(0L)
        }
    }

    /** 当前实际截取时长（秒） */
    val clipSelectedSeconds: Double
        get() = (clipEndMs - clipStartMs).coerceAtLeast(0L) / 1000.0

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
                        clipResultFileName = result.fileName
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

    /** 关闭完成状态并退出剪辑页 */
    fun dismissClipResult() {
        clipResultFileName = null
        showClipPage = false
    }

    private fun phaseText(phase: ClipPhase): String = when (phase) {
        ClipPhase.DOWNLOADING_SEGMENT -> "正在下载所选片段..."
        ClipPhase.BUILDING -> "正在生成剪辑文件..."
        ClipPhase.DOWNLOADING_FULL -> "服务器不支持分段下载，正在下载完整视频..."
        ClipPhase.TRIMMING -> "正在剪切视频..."
        ClipPhase.SAVING -> "正在保存到相册..."
    }

    companion object {
        private const val MIN_CLIP_SPAN_MS = 1000L

        /** 默认快捷截取时长（秒） */
        const val DEFAULT_CLIP_PRESET_SEC = 20

        /** 快捷截取时长选项（秒） */
        val CLIP_PRESET_SECONDS = listOf(20, 60, 120)
    }
}
