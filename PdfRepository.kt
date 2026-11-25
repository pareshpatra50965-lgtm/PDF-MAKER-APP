package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.example.model.PdfFileRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.roundToInt

class PdfRepository(private val context: Context) {

    val pdfDirectory: File
        get() = File(context.filesDir, "pdfs").apply {
            if (!exists()) mkdirs()
        }

    private val _files = MutableStateFlow<List<PdfFileRecord>>(emptyList())
    val files: StateFlow<List<PdfFileRecord>> = _files.asStateFlow()

    suspend fun refreshFiles() {
        val loaded = withContext(Dispatchers.IO) {
            val dir = pdfDirectory
            val pdfFiles = dir.listFiles { file ->
                file.isFile && file.name.endsWith(".pdf", ignoreCase = true) && !file.name.startsWith(".")
            } ?: emptyArray()

            pdfFiles
                .map { file ->
                    PdfFileRecord(
                        id = file.absolutePath,
                        name = file.name,
                        filePath = file.absolutePath,
                        sizeBytes = file.length(),
                        pageCount = readPdfPageCount(file),
                        createdAtMillis = file.lastModified()
                    )
                }
                .sortedByDescending { it.createdAtMillis }
        }
        _files.value = loaded
    }

    private fun readPdfPageCount(file: File): Int {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun renderFirstPageThumbnail(filePath: String, targetWidthPx: Int = 560): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext null
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (renderer.pageCount <= 0) return@withContext null
                        renderer.openPage(0).use { page ->
                            val scale = targetWidthPx.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                            val w = targetWidthPx.coerceAtLeast(160)
                            val h = (page.height * scale).roundToInt().coerceAtLeast(160)
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

    suspend fun renamePdf(record: PdfFileRecord, newName: String): Result<PdfFileRecord> =
        withContext(Dispatchers.IO) {
            try {
                val source = File(record.filePath)
                if (!source.exists()) {
                    return@withContext Result.failure(IllegalStateException("File not found."))
                }
                val cleanBase = newName
                    .trim()
                    .removeSuffix(".pdf")
                    .removeSuffix(".PDF")
                    .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                    .trim()
                if (cleanBase.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("Please enter a valid file name."))
                }
                val target = File(source.parentFile, "$cleanBase.pdf")
                if (target.exists() && target.absolutePath != source.absolutePath) {
                    return@withContext Result.failure(IllegalStateException("A file named $cleanBase.pdf already exists."))
                }
                if (!source.renameTo(target)) {
                    return@withContext Result.failure(IllegalStateException("Unable to rename file."))
                }
                refreshFiles()
                val updated = _files.value.firstOrNull { it.filePath == target.absolutePath }
                    ?: record.copy(id = target.absolutePath, name = target.name, filePath = target.absolutePath)
                Result.success(updated)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun deletePdf(record: PdfFileRecord): Boolean = withContext(Dispatchers.IO) {
        val deleted = File(record.filePath).delete()
        refreshFiles()
        deleted
    }

    suspend fun deleteAllPdfs(): Int = withContext(Dispatchers.IO) {
        val files = pdfDirectory.listFiles { f -> f.isFile && f.name.endsWith(".pdf", ignoreCase = true) }
            ?: emptyArray()
        var count = 0
        files.forEach { file ->
            if (file.delete()) count++
        }
        refreshFiles()
        count
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun savePdfToPublicDownloads(sourcePath: String, displayName: String): Result<String> =
        withContext(Dispatchers.IO) {
            var insertedUri: Uri? = null
            try {
                val sourceFile = File(sourcePath)
                if (!sourceFile.exists()) {
                    return@withContext Result.failure(IllegalStateException("Source PDF file not found."))
                }

                val safeName = displayName.ifBlank { sourceFile.name }
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PDF Maker")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                    ?: return@withContext Result.failure(IllegalStateException("Could not create entry in Downloads."))
                insertedUri = uri

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                    out.flush()
                } ?: throw IllegalStateException("Could not write PDF to Downloads.")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
                Result.success("Downloaded to Downloads/PDF Maker/$safeName")
            } catch (t: Throwable) {
                insertedUri?.let { uri ->
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: Exception) {
                    }
                }
                Result.failure(t)
            }
        }

    suspend fun exportPdfToUri(sourcePath: String, destinationUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourcePath)
                if (!sourceFile.exists()) {
                    return@withContext Result.failure(IllegalStateException("Source PDF file not found."))
                }
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                    out.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Unable to open destination file."))
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    fun getShareUri(filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun resolveImageDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                } ?: uri.lastPathSegment ?: "Image"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Image"
        }
    }
}
