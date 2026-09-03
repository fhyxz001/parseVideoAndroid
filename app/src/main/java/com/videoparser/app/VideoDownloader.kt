package com.videoparser.app

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.OutputStream

data class DownloadProgress(
    val percent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long
)

object VideoDownloader {

    suspend fun download(
        context: Context,
        videoUrl: String,
        title: String,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = title.replace(Regex("[\\\\/:*?\"<>|]"), "").ifBlank { "video" } + ".mp4"
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
