package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import java.io.File
import java.nio.charset.StandardCharsets

sealed interface SaveDownloadResult {
    data class Saved(val fileName: String) : SaveDownloadResult
    data class RequiresSaf(val fileName: String, val mimeType: String, val content: String) : SaveDownloadResult
    data class Failed(val reason: String) : SaveDownloadResult
}

object FileExportHelper {

    /**
     * Saves directly to Android Downloads using MediaStore on API 29+ (zero permissions required).
     * On API 24-28, requests SAF CreateDocument picker so no legacy storage permission is ever requested.
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        content: String,
    ): SaveDownloadResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SaveDownloadResult.RequiresSaf(fileName, mimeType, content)
        }
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return SaveDownloadResult.Failed("无法创建下载文件")

            resolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: return SaveDownloadResult.Failed("无法写入文件内容")

            val doneValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, doneValues, null, null)
            SaveDownloadResult.Saved(fileName)
        } catch (e: Exception) {
            SaveDownloadResult.Failed(e.localizedMessage ?: "保存失败")
        }
    }

    fun writeToSafUri(
        context: Context,
        uri: Uri,
        content: String,
    ): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(StandardCharsets.UTF_8))
            out.flush()
        } ?: error("无法打开所选目标文件")
    }

    /**
     * Writes to app cache/exports and opens Android native share chooser via FileProvider.
     */
    fun shareExportFile(
        context: Context,
        fileName: String,
        mimeType: String,
        content: String,
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeBytes(content.toByteArray(StandardCharsets.UTF_8))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(
            shareIntent,
            context.getString(R.string.share_chooser_title),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
