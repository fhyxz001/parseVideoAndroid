package com.videoparser.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    var isGettingShortLink by mutableStateOf(false)

    val toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val copyEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val shareEvents = MutableSharedFlow<VideoData>(extraBufferCapacity = 8)

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
        } else {
            toast("剪贴板为空")
        }
    }

    fun parse() {
        val url = videoUrl.trim()
        if (url.isEmpty()) {
            toast("请输入链接")
            return
        }
        isParsing = true
        parseData = null
        viewModelScope.launch {
            when (val result = VideoApi.parse(url)) {
                is ParseResult.Success -> {
                    parseData = result.data
                    toast("解析成功")
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
        viewModelScope.launch {
            val title = data.title.ifBlank { "video" }
            val ok = VideoDownloader.download(
                getApplication(),
                data.videoUrl.trim(),
                title
            ) { percent -> downloadPercent = percent }
            isDownloading = false
            toast(if (ok) "下载完成" else "下载失败")
        }
    }

    fun copyLongLink() {
        val url = videoUrl.trim()
        if (url.isEmpty()) {
            toast("没有可复制的链接")
            return
        }
        copyEvents.tryEmit(url)
        toast("长链接已复制")
    }

    fun copyShortLink() {
        if (isGettingShortLink) return
        val data = parseData
        if (data == null || data.videoUrl.isBlank()) {
            toast("没有可转换的链接")
            return
        }
        isGettingShortLink = true
        viewModelScope.launch {
            val shortLink = VideoApi.createShortLink(data.videoUrl)
            isGettingShortLink = false
            if (shortLink != null) {
                copyEvents.tryEmit(shortLink)
                toast("短链接已复制")
            } else {
                toast("获取短链接失败")
            }
        }
    }

    fun share() {
        val data = parseData
        if (data == null || data.videoUrl.isBlank()) {
            toast("没有可分享的视频")
            return
        }
        shareEvents.tryEmit(data)
    }
}
