package com.example.ui.navigation

import android.net.Uri
import com.example.model.PdfToolId

object Routes {
    const val HOME = "home"
    const val TOOLS = "tools"
    const val FILES = "files"
    const val SETTINGS = "settings"
    const val IMAGE_TO_PDF = "image_to_pdf"
    const val PDF_READER_PICKER = "pdf_reader"
    const val PDF_READER_FILE = "pdf_reader/{encodedPath}"
    const val TOOL_INFO = "tool_info/{toolId}"

    fun pdfReaderForFile(filePath: String): String {
        return "pdf_reader/${Uri.encode(filePath)}"
    }

    fun toolInfo(toolId: PdfToolId): String {
        return "tool_info/${toolId.name}"
    }
}
