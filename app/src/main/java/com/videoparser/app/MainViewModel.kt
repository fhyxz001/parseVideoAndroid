package com.videoparser.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            when (val result = VideoApi.parse(url, serverUrl)) {
                is ParseResult.Success -> {
                    parseData = result.data
                    toast("解析成功")
                    fetchVideoFileSize(result.data.videoUrl)
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
}
