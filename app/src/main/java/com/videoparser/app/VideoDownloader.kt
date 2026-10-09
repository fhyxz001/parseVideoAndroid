package com.videoparser.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.EOFException
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong

data class DownloadProgress(
    val percent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long
)

object VideoDownloader {

    /** 多线程下载参数：片段达到该大小才并行；每片目标 8MB；线程数上限 4（OkHttp 单主机默认并发 5） */
    private const val MIN_PARALLEL_BYTES = 8L * 1024 * 1024
    private const val TARGET_CHUNK_BYTES = 8L * 1024 * 1024
    private const val MAX_PARALLEL_THREADS = 4

    private const val PROGRESS_INTERVAL_MS = 200L

    suspend fun download(
        context: Context,
        videoUrl: String,
        title: String,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = title.replace(Regex("[\\\\/:*?\"<>|]"), "").ifBlank { "video" } + ".mp4"
        try {
            // 大文件且服务器支持 Range：多线程分片下载到缓存，再整体写入相册（0~95 下载 / 95~100 落盘）
            val (size, rangeOk) = probeSizeAndRange(videoUrl)
            if (rangeOk && size >= MIN_PARALLEL_BYTES) {
                val temp = File(context.cacheDir, "dl_${System.currentTimeMillis()}.mp4")
                try {
                    val ok = downloadRangeToFile(videoUrl, 0L, size, temp) { downloaded, total ->
                        onProgress(
                            DownloadProgress(
                                percent = (downloaded * 95 / total).toInt().coerceIn(0, 95),
                                downloadedBytes = downloaded,
                                totalBytes = total
                            )
                        )
                    }
                    if (ok) {
                        val tempLen = temp.length().coerceAtLeast(1L)
                        val caller = coroutineContext
                        val saved = saveVideoToGallery(context, fileName) { output ->
                            temp.inputStream().use { input ->
                                val buf = ByteArray(128 * 1024)
                                var copied = 0L
                                while (true) {
                                    caller.ensureActive()
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    output.write(buf, 0, n)
                                    copied += n
                                    onProgress(
                                        DownloadProgress(
                                            percent = (95 + copied * 5 / tempLen).toInt().coerceIn(95, 99),
                                            downloadedBytes = copied,
                                            totalBytes = tempLen
                                        )
                                    )
                                }
                            }
                        }
                        onProgress(DownloadProgress(100, tempLen, tempLen))
                        return@withContext saved
                    }
                } finally {
                    temp.delete()
                }
            }
            // 服务器不支持 Range / 文件过小 / 并行失败：单流直写
            downloadSingleToGallery(context, videoUrl, fileName, onProgress)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** 单连接流式下载并直写相册（原始实现，作为多线程路径的兜底） */
    private suspend fun downloadSingleToGallery(
        context: Context,
        videoUrl: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(videoUrl).get().build()
            VideoApi.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val totalSize = body.contentLength()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaMediaStore(context, fileName, totalSize, body.byteStream(), onProgress)
                } else {
                    saveToDcimLegacy(fileName, totalSize, body.byteStream(), onProgress)
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /** 获取视频文件大小（字节），获取失败时返回 -1 */
    suspend fun fetchVideoSize(videoUrl: String): Long = withContext(Dispatchers.IO) {
        try {
            // 先尝试 HEAD 请求
            val headRequest = Request.Builder().url(videoUrl).head().build()
            VideoApi.client.newCall(headRequest).execute().use { response ->
                val len = response.header("Content-Length")?.toLongOrNull() ?: -1L
                if (response.isSuccessful && len > 0) return@withContext len
            }

            // 部分服务器不支持 HEAD，退化为 Range 请求只取 1 字节
            val rangeRequest = Request.Builder()
                .url(videoUrl)
                .header("Range", "bytes=0-0")
                .get()
                .build()
            VideoApi.client.newCall(rangeRequest).execute().use { response ->
                response.header("Content-Range")
                    ?.substringAfterLast('/', "")
                    ?.toLongOrNull()
                    ?.takeIf { it > 0 }
                    ?: -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * 由调用方直接向相册输出流写入视频数据（剪辑功能使用）。
     * writer 抛异常或返回失败时会清理未完成的相册条目。
     */
    suspend fun saveVideoToGallery(
        context: Context,
        fileName: String,
        writer: (OutputStream) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                try {
                    resolver.openOutputStream(uri)?.use { output -> writer(output) } ?: return@withContext false
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                try {
                    file.outputStream().use { output -> writer(output) }
                    true
                } catch (e: Exception) {
                    file.delete()
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /** 全量下载到指定缓存文件（剪辑降级路径使用），优先多线程分片，失败回退单流，支持协程取消 */
    suspend fun downloadWholeToFile(
        videoUrl: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val (size, rangeOk) = probeSizeAndRange(videoUrl)
        if (rangeOk && size >= MIN_PARALLEL_BYTES) {
            val ok = downloadRangeToFile(videoUrl, 0L, size, dest, onProgress)
            if (ok) return@withContext true
            dest.delete()
        }
        downloadWholeSingle(videoUrl, dest, onProgress)
    }

    /** 单连接全量下载（原始实现，兜底路径） */
    private suspend fun downloadWholeSingle(
        videoUrl: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val context = coroutineContext
        try {
            val request = Request.Builder().url(videoUrl).get().build()
            VideoApi.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val total = body.contentLength()
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        while (true) {
                            context.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            downloaded += n
                            onProgress(downloaded, total)
                        }
                    }
                }
                true
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            dest.delete()
            throw e
        } catch (e: Exception) {
            dest.delete()
            false
        }
    }

    /** 探测文件大小与 Range 支持：返回 (总字节, 是否支持分段)。任一不满足返回 (-1, false) */
    private fun probeSizeAndRange(url: String): Pair<Long, Boolean> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .get()
                .build()
            VideoApi.client.newCall(request).execute().use { response ->
                when {
                    response.code == 206 -> {
                        val total = response.header("Content-Range")
                            ?.substringAfterLast('/')?.toLongOrNull() ?: -1L
                        if (total > 0) total to true else -1L to false
                    }
                    response.isSuccessful ->
                        (response.header("Content-Length")?.toLongOrNull() ?: -1L) to false
                    else -> -1L to false
                }
            }
        } catch (e: Exception) {
            -1L to false
        }
    }

    /**
     * 多线程分片下载字节区间 [contentStart, contentEndExclusive) 到 dest
     * （dest 最终长度 = 区间长度）。任一分片失败即整体失败并清理 dest。
     * 区间过小或服务器不返回 206 时返回 false，由调用方回退单流。
     */
    suspend fun downloadRangeToFile(
        url: String,
        contentStart: Long,
        contentEndExclusive: Long,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val caller = coroutineContext
        val total = contentEndExclusive - contentStart
        if (total < MIN_PARALLEL_BYTES) return@withContext false
        val chunkCount = ((total + TARGET_CHUNK_BYTES - 1) / TARGET_CHUNK_BYTES)
            .toInt().coerceIn(1, MAX_PARALLEL_THREADS)
        try {
            RandomAccessFile(dest, "rw").use { it.setLength(total) }
        } catch (e: Exception) {
            return@withContext false
        }
        val downloadedAll = AtomicLong(0)
        val lastReport = AtomicLong(0)
        try {
            coroutineScope {
                val baseChunk = total / chunkCount
                var chunkStart = contentStart
                var destOffset = 0L
                repeat(chunkCount) { index ->
                    val chunkEnd =
                        if (index == chunkCount - 1) contentEndExclusive else chunkStart + baseChunk
                    val chunkLen = chunkEnd - chunkStart
                    val start = chunkStart
                    val end = chunkEnd
                    val offset = destOffset
                    launch {
                        downloadChunk(url, start, end, dest, offset, caller) { n ->
                            val now = downloadedAll.addAndGet(n)
                            val ts = System.currentTimeMillis()
                            val last = lastReport.get()
                            if (ts - last >= PROGRESS_INTERVAL_MS &&
                                lastReport.compareAndSet(last, ts)
                            ) {
                                onProgress(now, total)
                            }
                        }
                    }
                    chunkStart = chunkEnd
                    destOffset += chunkLen
                }
            }
            if (downloadedAll.get() != total) return@withContext false
            onProgress(total, total)
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            dest.delete()
            throw e
        } catch (e: Exception) {
            dest.delete()
            false
        }
    }

    /** 下载单个分片 [start, end) 并写入 dest 的 destOffset 偏移处；字节数不符视为失败 */
    private fun downloadChunk(
        url: String,
        start: Long,
        endExclusive: Long,
        dest: File,
        destOffset: Long,
        caller: kotlin.coroutines.CoroutineContext,
        onChunkBytes: (Long) -> Unit
    ) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-${endExclusive - 1}")
            .get()
            .build()
        VideoApi.client.newCall(request).execute().use { response ->
            if (response.code != 206) throw EOFException("服务器不支持 Range 分片")
            val body = response.body ?: throw EOFException("分片响应为空")
            val expected = endExclusive - start
            RandomAccessFile(dest, "rw").use { raf ->
                raf.seek(destOffset)
                val buf = ByteArray(128 * 1024)
                var got = 0L
                body.byteStream().use { input ->
                    while (true) {
                        caller.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        raf.write(buf, 0, n)
                        got += n
                        onChunkBytes(n.toLong())
                    }
                }
                if (got != expected) throw EOFException("分片不完整: $got/$expected")
            }
        }
    }

    private fun copyWithProgress(
        input: java.io.InputStream,
        output: OutputStream,
        totalSize: Long,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val buffer = ByteArray(64 * 1024)
        var downloaded = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            downloaded += read
            onProgress(
                DownloadProgress(
                    percent = if (totalSize > 0) (downloaded * 100 / totalSize).toInt() else -1,
                    downloadedBytes = downloaded,
                    totalBytes = totalSize
                )
            )
        }
        output.flush()
    }

    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        totalSize: Long,
        input: java.io.InputStream,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                input.use { copyWithProgress(it, output, totalSize, onProgress) }
            } ?: return false
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToDcimLegacy(
        fileName: String,
        totalSize: Long,
        input: java.io.InputStream,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        return try {
            file.outputStream().use { output ->
                input.use { copyWithProgress(it, output, totalSize, onProgress) }
            }
            true
        } catch (e: Exception) {
            file.delete()
            false
        }
    }

    /** 将内存中的 Bitmap 以 JPEG 保存到相册 Pictures 目录（剪辑帧预览保存使用） */
    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false
                    try {
                        val compressed = resolver.openOutputStream(uri)
                            ?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                            ?: return@withContext false
                        if (!compressed) {
                            resolver.delete(uri, null, null)
                            return@withContext false
                        }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        true
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        false
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, fileName)
                    try {
                        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                        true
                    } catch (e: Exception) {
                        file.delete()
                        false
                    }
                }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun downloadImage(
        context: Context,
        imageUrl: String,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val safeName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "").ifBlank { "cover" } + ".jpg"
        try {
            val request = Request.Builder().url(imageUrl).get().build()
            VideoApi.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageViaMediaStore(context, safeName, body.byteStream())
                } else {
                    saveImageLegacy(safeName, body.byteStream())
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun saveImageViaMediaStore(
        context: Context,
        fileName: String,
        input: java.io.InputStream
    ): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                input.use { it.copyTo(output) }
            } ?: return false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveImageLegacy(
        fileName: String,
        input: java.io.InputStream
    ): Boolean {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        return try {
            file.outputStream().use { output ->
                input.use { it.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            file.delete()
            false
        }
    }
}
