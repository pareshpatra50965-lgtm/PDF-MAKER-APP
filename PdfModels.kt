package com.example.model

import android.net.Uri
import androidx.annotation.StringRes
import com.example.R

enum class PdfToolId {
    IMAGE_TO_PDF,
    SCAN_TO_PDF,
    MERGE_PDF,
    SPLIT_PDF,
    COMPRESS_PDF,
    PDF_READER,
    PDF_EDITOR,
    SIGN_PDF,
    WORD_TO_PDF,
    PDF_TO_IMAGE
}

enum class ToolCategory(@StringRes val labelRes: Int) {
    ALL(R.string.category_all),
    CREATE(R.string.category_create),
    ORGANIZE(R.string.category_organize),
    CONVERT(R.string.category_convert),
    READ_EDIT(R.string.category_read_edit)
}

data class PdfToolSpec(
    val id: PdfToolId,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val category: ToolCategory,
    val isAvailableNow: Boolean,
    val summaryDetail: String
)

object PdfToolCatalog {
    val allTools: List<PdfToolSpec> = listOf(
        PdfToolSpec(
            id = PdfToolId.IMAGE_TO_PDF,
            titleRes = R.string.tool_image_to_pdf_title,
            subtitleRes = R.string.tool_image_to_pdf_subtitle,
            category = ToolCategory.CREATE,
            isAvailableNow = true,
            summaryDetail = "Select multiple gallery photos, reorder pages, rotate or crop images, choose paper size and orientation, and export a sharp multi-page PDF."
        ),
        PdfToolSpec(
            id = PdfToolId.SCAN_TO_PDF,
            titleRes = R.string.tool_scan_to_pdf_title,
            subtitleRes = R.string.tool_scan_to_pdf_subtitle,
            category = ToolCategory.CREATE,
            isAvailableNow = false,
            summaryDetail = "Capture physical documents with automatic edge detection, perspective correction, and high-contrast page filters."
        ),
        PdfToolSpec(
            id = PdfToolId.MERGE_PDF,
            titleRes = R.string.tool_merge_pdf_title,
            subtitleRes = R.string.tool_merge_pdf_subtitle,
            category = ToolCategory.ORGANIZE,
            isAvailableNow = false,
            summaryDetail = "Combine two or more PDF documents in your chosen sequence into a single unified file."
        ),
        PdfToolSpec(
            id = PdfToolId.SPLIT_PDF,
            titleRes = R.string.tool_split_pdf_title,
            subtitleRes = R.string.tool_split_pdf_subtitle,
            category = ToolCategory.ORGANIZE,
            isAvailableNow = false,
            summaryDetail = "Extract specific page ranges or split a large PDF into individual single-page documents."
        ),
        PdfToolSpec(
            id = PdfToolId.COMPRESS_PDF,
            titleRes = R.string.tool_compress_pdf_title,
            subtitleRes = R.string.tool_compress_pdf_subtitle,
            category = ToolCategory.ORGANIZE,
            isAvailableNow = false,
            summaryDetail = "Reduce PDF file size for faster email and messaging sharing while preserving readable clarity."
        ),
        PdfToolSpec(
            id = PdfToolId.PDF_READER,
            titleRes = R.string.tool_pdf_reader_title,
            subtitleRes = R.string.tool_pdf_reader_subtitle,
            category = ToolCategory.READ_EDIT,
            isAvailableNow = true,
            summaryDetail = "Open and read PDFs created in PDF Maker or imported from your device storage with smooth page rendering."
        ),
        PdfToolSpec(
            id = PdfToolId.PDF_EDITOR,
            titleRes = R.string.tool_pdf_editor_title,
            subtitleRes = R.string.tool_pdf_editor_subtitle,
            category = ToolCategory.READ_EDIT,
            isAvailableNow = false,
            summaryDetail = "Add text notes, highlights, and markup annotations directly on top of existing PDF pages."
        ),
        PdfToolSpec(
            id = PdfToolId.SIGN_PDF,
            titleRes = R.string.tool_sign_pdf_title,
            subtitleRes = R.string.tool_sign_pdf_subtitle,
            category = ToolCategory.READ_EDIT,
            isAvailableNow = false,
            summaryDetail = "Draw or insert your handwritten signature and place it accurately onto contracts and forms."
        ),
        PdfToolSpec(
            id = PdfToolId.WORD_TO_PDF,
            titleRes = R.string.tool_word_to_pdf_title,
            subtitleRes = R.string.tool_word_to_pdf_subtitle,
            category = ToolCategory.CONVERT,
            isAvailableNow = false,
            summaryDetail = "Convert Word (.docx) documents into portable, print-ready PDF files with preserved layout."
        ),
        PdfToolSpec(
            id = PdfToolId.PDF_TO_IMAGE,
            titleRes = R.string.tool_pdf_to_image_title,
            subtitleRes = R.string.tool_pdf_to_image_subtitle,
            category = ToolCategory.CONVERT,
            isAvailableNow = false,
            summaryDetail = "Render PDF pages into crisp high-resolution PNG or JPEG image files for easy sharing."
        )
    )

    fun findById(id: PdfToolId): PdfToolSpec =
        allTools.first { it.id == id }
}

enum class PdfPageSize(val label: String, val dimensionsLabel: String) {
    A4("A4", "210 × 297 mm"),
    LETTER("Letter", "8.5 × 11 in"),
    FIT_IMAGE("Original", "Original image ratio")
}

enum class PdfPageOrientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    AUTO("Auto")
}

enum class PdfPageMargin(val label: String, val marginPoints: Float) {
    NONE("No Margin", 0f),
    STANDARD("Standard", 24f),
    WIDE("Wide", 44f)
}

enum class PdfImageQuality(val label: String, val description: String, val maxDecodeDimension: Int) {
    HIGH("High Quality", "Best clarity (up to 2200px)", 2200),
    BALANCED("Balanced", "Crisp text & moderate size (1600px)", 1600),
    COMPACT("Compact", "Smaller file size (1100px)", 1100)
}

enum class FileSortOption(val label: String) {
    NEWEST("Newest"),
    NAME("Name"),
    SIZE("Size")
}

data class AppSettings(
    val defaultPageSize: PdfPageSize = PdfPageSize.A4,
    val defaultMargin: PdfPageMargin = PdfPageMargin.STANDARD,
    val imageQuality: PdfImageQuality = PdfImageQuality.BALANCED
)

data class PdfFileRecord(
    val id: String,
    val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val createdAtMillis: Long
)

data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isFullImage: Boolean
        get() = left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f

    fun rotatedClockwise90(): NormalizedCropRect = NormalizedCropRect(
        left = (1f - bottom).coerceIn(0f, 1f),
        top = left.coerceIn(0f, 1f),
        right = (1f - top).coerceIn(0f, 1f),
        bottom = right.coerceIn(0f, 1f)
    )

    fun rotatedCounterClockwise90(): NormalizedCropRect = NormalizedCropRect(
        left = top.coerceIn(0f, 1f),
        top = (1f - right).coerceIn(0f, 1f),
        right = bottom.coerceIn(0f, 1f),
        bottom = (1f - left).coerceIn(0f, 1f)
    )
}

data class SelectedImageItem(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val rotationDegrees: Int = 0,
    val cropRect: NormalizedCropRect? = null
)
