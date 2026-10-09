package com.videoparser.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * 剪辑页播放器封装：远程视频边下边播，驱动"播放定位 + 设为起点/终点 + 选区循环"的选段交互。
 * 由 ClipPage 持有（进入创建、离开释放），播放失败通过 [onError] 通知 UI 降级为静态时间轴模式。
 */
class ClipPlaybackController(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    /** 元数据就绪，可以 seek / 取时长 */
    var isReady by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    /** 播放头位置；播放中由 tick() 刷新，seek 后立即同步 */
    var positionMs by mutableStateOf(0L)
        private set

    /** 播放失败（编码不支持/防盗链等），UI 收到后切换降级模式 */
    var onError: (() -> Unit)? = null

    private var loopStartMs = 0L
    private var loopEndMs = -1L

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            isReady = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onPlayerError(error: PlaybackException) {
            onError?.invoke()
        }
    }

    init {
        player.addListener(listener)
    }

    fun prepare(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    fun play() = player.play()

    fun pause() = player.pause()

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(ms: Long) {
        if (!isReady) return
        val clamped = ms.coerceAtLeast(0L)
        player.seekTo(clamped)
        positionMs = clamped
    }

    /**
     * 设置选区循环窗口：播放越过终点自动回跳起点。
     * 全选（终点=时长）时不启用循环，让播放自然结束。
     */
    fun setLoopWindow(startMs: Long, endMs: Long, durationMs: Long) {
        loopStartMs = startMs
        loopEndMs = if (durationMs > 0 && endMs < durationMs && endMs > startMs) endMs else -1L
    }

    /** 由 UI 轮询调用（播放中约 100ms 一次）：刷新播放头并处理选区循环 */
    fun tick() {
        if (!isReady) return
        val pos = player.currentPosition
        positionMs = pos
        if (loopEndMs > loopStartMs && player.isPlaying && pos >= loopEndMs) {
            player.seekTo(loopStartMs)
            positionMs = loopStartMs
        }
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
