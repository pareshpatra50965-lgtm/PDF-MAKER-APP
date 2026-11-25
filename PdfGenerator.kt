package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.example.model.NormalizedCropRect
import com.example.model.PdfImageQuality
import com.example.model.PdfPageMargin
import com.example.model.PdfPageOrientation
import com.example.model.PdfPageSize
import com.example.model.SelectedImageItem
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class GeneratedPdfResult(
    val file: File,
    val pageCount: Int,
    val sizeBytes: Long
)

class PdfGenerator(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun generatePdfFromImages(
        images: List<SelectedImageItem>,
        outputDirectory: File,
        requestedFileName: String,
        pageSize: PdfPageSize,
        orientation: PdfPageOrientation,
        margin: PdfPageMargin,
        imageQuality: PdfImageQuality,
        onProgress: (currentPage: Int, totalPages: Int) -> Unit
    ): Result<GeneratedPdfResult> = withContext(Dispatchers.IO) {
        if (images.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please select at least one image."))
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val finalFile = resolveUniquePdfFile(outputDirectory, requestedFileName)
        val tempFile = File(outputDirectory, ".tmp_${System.currentTimeMillis()}.pdf")
        val document = PDDocument()

        try {
            val total = images.size
            val jpegQuality = when (imageQuality) {
                PdfImageQuality.HIGH -> 0.94f
                PdfImageQuality.BALANCED -> 0.85f
                PdfImageQuality.COMPACT -> 0.72f
            }

            images.forEachIndexed { index, item ->
                currentCoroutineContext().ensureActive()
                onProgress(index + 1, total)

                val decoded = decodeScaledBitmap(item.uri, imageQuality.maxDecodeDimension)
                    ?: throw IllegalStateException(
                        "Could not read \"${item.displayName}\" (page ${index + 1}). Remove or replace this image."
                    )

                val preparedBitmap = applyTransforms(
                    source = decoded,
                    rotationDegrees = item.rotationDegrees,
                    cropRect = item.cropRect
                )

                try {
                    val (pageWidth, pageHeight) = computePageDimensions(
                        pageSize = pageSize,
                        orientation = orientation,
                        imageWidth = preparedBitmap.width,
                        imageHeight = preparedBitmap.height
                    )

                    val page = PDPage(PDRectangle(pageWidth, pageHeight))
                    document.addPage(page)

                    val marginPt = if (pageSize == PdfPageSize.FIT_IMAGE && margin == PdfPageMargin.NONE) {
                        0f
                    } else {
                        margin.marginPoints.coerceAtMost(min(pageWidth, pageHeight) / 4f)
                    }

                    val availWidth = (pageWidth - marginPt * 2f).coerceAtLeast(1f)
                    val availHeight = (pageHeight - marginPt * 2f).coerceAtLeast(1f)

                    val scale = min(
                        availWidth / preparedBitmap.width.toFloat(),
                        availHeight / preparedBitmap.height.toFloat()
                    )
                    val drawW = preparedBitmap.width * scale
                    val drawH = preparedBitmap.height * scale
                    val x = marginPt + (availWidth - drawW) / 2f
                    val y = marginPt + (availHeight - drawH) / 2f

                    val pdImage = JPEGFactory.createFromImage(document, preparedBitmap, jpegQuality)
                    PDPageContentStream(document, page).use { contentStream ->
                        contentStream.drawImage(pdImage, x, y, drawW, drawH)
                    }
                } finally {
                    if (!preparedBitmap.isRecycled) {
                        preparedBitmap.recycle()
                    }
                }
            }

            document.save(tempFile)

            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(
                GeneratedPdfResult(
                    file = finalFile,
                    pageCount = images.size,
                    sizeBytes = finalFile.length()
                )
            )
        } catch (t: Throwable) {
            tempFile.delete()
            Result.failure(t)
        } finally {
            try {
                document.close()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun loadEditableBitmap(
        uri: Uri,
        maxDimension: Int = 1200
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val bmp = decodeScaledBitmap(uri, maxDimension)
        if (bmp != null) {
            Result.success(bmp)
        } else {
            Result.failure(IllegalStateException("Unable to load this image for editing."))
        }
    }

    fun applyTransforms(
        source: Bitmap,
        rotationDegrees: Int,
        cropRect: NormalizedCropRect?,
        recycleSource: Boolean = true
    ): Bitmap {
        var current = source

        val normalizedDeg = ((rotationDegrees % 360) + 360) % 360
        if (normalizedDeg != 0) {
            val matrix = Matrix().apply { postRotate(normalizedDeg.toFloat()) }
            val rotated = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
            if (recycleSource && rotated !== current && !current.isRecycled) {
                current.recycle()
            }
            current = rotated
        }

        if (cropRect != null && !cropRect.isFullImage) {
            val leftPx = (cropRect.left * current.width).roundToInt().coerceIn(0, current.width - 1)
            val topPx = (cropRect.top * current.height).roundToInt().coerceIn(0, current.height - 1)
            val rightPx = (cropRect.right * current.width).roundToInt().coerceIn(leftPx + 1, current.width)
            val bottomPx = (cropRect.bottom * current.height).roundToInt().coerceIn(topPx + 1, current.height)
            val cropW = (rightPx - leftPx).coerceAtLeast(1)
            val cropH = (bottomPx - topPx).coerceAtLeast(1)

            val cropped = Bitmap.createBitmap(current, leftPx, topPx, cropW, cropH)
            if ((recycleSource || current !== source) && cropped !== current && !current.isRecycled) {
                current.recycle()
            }
            current = cropped
        }

        val flattened = flattenAlphaIfNeeded(current)
        if ((recycleSource || current !== source) && flattened !== current && !current.isRecycled) {
            current.recycle()
        }
        return flattened
    }

    private fun flattenAlphaIfNeeded(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val rgbBitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rgbBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, 0f, 0f, null)
        return rgbBitmap
    }

    private fun computePageDimensions(
        pageSize: PdfPageSize,
        orientation: PdfPageOrientation,
        imageWidth: Int,
        imageHeight: Int
    ): Pair<Float, Float> {
        val imageIsLandscape = imageWidth > imageHeight
        val useLandscape = when (orientation) {
            PdfPageOrientation.PORTRAIT -> false
            PdfPageOrientation.LANDSCAPE -> true
            PdfPageOrientation.AUTO -> imageIsLandscape
        }

        return when (pageSize) {
            PdfPageSize.A4 -> {
                val rect = PDRectangle.A4
                if (useLandscape) rect.height to rect.width else rect.width to rect.height
            }
            PdfPageSize.LETTER -> {
                val rect = PDRectangle.LETTER
                if (useLandscape) rect.height to rect.width else rect.width to rect.height
            }
            PdfPageSize.FIT_IMAGE -> {
                val maxDim = 842f
                val maxCurrent = max(imageWidth, imageHeight).toFloat().coerceAtLeast(1f)
                val scale = maxDim / maxCurrent
                val w = (imageWidth * scale).coerceAtLeast(100f)
                val h = (imageHeight * scale).coerceAtLeast(100f)
                when (orientation) {
                    PdfPageOrientation.AUTO -> w to h
                    PdfPageOrientation.PORTRAIT -> min(w, h) to max(w, h)
                    PdfPageOrientation.LANDSCAPE -> max(w, h) to min(w, h)
                }
            }
        }
    }

    fun validateImageReadable(uri: Uri): Boolean {
        val resolver = context.contentResolver
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
            if (opts.outWidth > 0 && opts.outHeight > 0) return true
        } catch (_: Exception) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return try {
                var valid = false
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    valid = info.size.width > 0 && info.size.height > 0
                    decoder.setTargetSize(1, 1)
                }.recycle()
                valid
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    private fun decodeScaledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val width = info.size.width
                    val height = info.size.height
                    val largest = max(width, height)
                    if (largest > maxDimension && maxDimension > 0) {
                        val ratio = maxDimension.toFloat() / largest.toFloat()
                        decoder.setTargetSize(
                            (width * ratio).roundToInt().coerceAtLeast(1),
                            (height * ratio).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (_: Exception) {
                null
            }
        } else {
            decodeBitmapLegacy(uri, maxDimension)
        }
    }

    private fun decodeBitmapLegacy(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        var sampleSize = 1
        while (max(rawW / sampleSize, rawH / sampleSize) > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return decoded
        }

        return try {
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) {
                decoded.recycle()
            }
            rotated
        } catch (_: Exception) {
            decoded
        }
    }

    private fun resolveUniquePdfFile(directory: File, requestedName: String): File {
        val cleanBase = requestedName
            .trim()
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .trim()
            .ifEmpty { "PDF_${System.currentTimeMillis()}" }

        var candidate = File(directory, "$cleanBase.pdf")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(directory, "${cleanBase}_$counter.pdf")
            counter++
        }
        return candidate
    }
}
