package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PdfGenerator
import com.example.data.PdfRepository
import com.example.data.SettingsRepository
import com.example.model.NormalizedCropRect
import com.example.model.PdfFileRecord
import com.example.model.PdfImageQuality
import com.example.model.PdfPageMargin
import com.example.model.PdfPageOrientation
import com.example.model.PdfPageSize
import com.example.model.SelectedImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ImageToPdfUiState(
    val selectedImages: List<SelectedImageItem> = emptyList(),
    val fileName: String = defaultDocumentName(),
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val orientation: PdfPageOrientation = PdfPageOrientation.PORTRAIT,
    val margin: PdfPageMargin = PdfPageMargin.STANDARD,
    val imageQuality: PdfImageQuality = PdfImageQuality.BALANCED,
    val isGenerating: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val errorMessage: String? = null,
    val generatedPdf: PdfFileRecord? = null,
    val generatedPreviewBitmap: Bitmap? = null
)

private fun defaultDocumentName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    return "PDF_Maker_$stamp"
}

class ImageToPdfViewModel(application: Application) : AndroidViewModel(application) {

    val pdfGenerator = PdfGenerator(application.applicationContext)
    private val pdfRepository = PdfRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ImageToPdfUiState())
    val uiState: StateFlow<ImageToPdfUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = settingsRepository.settingsFlow.first()
            _uiState.update {
                it.copy(
                    pageSize = prefs.defaultPageSize,
                    margin = prefs.defaultMargin,
                    imageQuality = prefs.imageQuality
                )
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val (validItems, invalidCount) = withContext(Dispatchers.IO) {
                var bad = 0
                val valid = mutableListOf<SelectedImageItem>()
                uris.forEach { uri ->
                    if (pdfGenerator.validateImageReadable(uri)) {
                        valid.add(
                            SelectedImageItem(
                                id = UUID.randomUUID().toString(),
                                uri = uri,
                                displayName = pdfRepository.resolveImageDisplayName(uri)
                            )
                        )
                    } else {
                        bad++
                    }
                }
                valid to bad
            }

            _uiState.update { current ->
                val err = when {
                    validItems.isEmpty() && invalidCount > 0 ->
                        "Selected image(s) could not be read or are unsupported."
                    invalidCount > 0 ->
                        "$invalidCount unreadable image(s) skipped. Added ${validItems.size} valid image(s)."
                    else -> null
                }
                current.copy(
                    selectedImages = current.selectedImages + validItems,
                    errorMessage = err,
                    generatedPdf = null,
                    generatedPreviewBitmap = null
                )
            }
        }
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        _uiState.update { current ->
            val list = current.selectedImages.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) return@update current
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            current.copy(selectedImages = list, errorMessage = null)
        }
    }

    fun moveImageUp(index: Int) {
        if (index > 0) moveImage(index, index - 1)
    }

    fun moveImageDown(index: Int) {
        val lastIndex = _uiState.value.selectedImages.lastIndex
        if (index in 0 until lastIndex) moveImage(index, index + 1)
    }

    fun rotateImageClockwise(id: String) {
        _uiState.update { current ->
            current.copy(
                selectedImages = current.selectedImages.map { item ->
                    if (item.id == id) {
                        val nextRot = (item.rotationDegrees + 90) % 360
                        val nextCrop = item.cropRect?.rotatedClockwise90()
                        item.copy(rotationDegrees = nextRot, cropRect = nextCrop)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun updateImageEdits(id: String, rotationDegrees: Int, cropRect: NormalizedCropRect?) {
        val cleanCrop = if (cropRect == null || cropRect.isFullImage) null else cropRect
        val cleanRotation = ((rotationDegrees % 360) + 360) % 360
        _uiState.update { current ->
            current.copy(
                selectedImages = current.selectedImages.map { item ->
                    if (item.id == id) {
                        item.copy(rotationDegrees = cleanRotation, cropRect = cleanCrop)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun removeImage(id: String) {
        _uiState.update { current ->
            current.copy(
                selectedImages = current.selectedImages.filterNot { it.id == id },
                errorMessage = null
            )
        }
    }

    fun clearAllImages() {
        _uiState.update {
            it.copy(
                selectedImages = emptyList(),
                errorMessage = null,
                generatedPdf = null,
                generatedPreviewBitmap = null
            )
        }
    }

    fun updateFileName(name: String) {
        _uiState.update { it.copy(fileName = name, errorMessage = null) }
    }

    fun updatePageSize(pageSize: PdfPageSize) {
        _uiState.update { it.copy(pageSize = pageSize) }
    }

    fun updateOrientation(orientation: PdfPageOrientation) {
        _uiState.update { it.copy(orientation = orientation) }
    }

    fun updateMargin(margin: PdfPageMargin) {
        _uiState.update { it.copy(margin = margin) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetForNewPdf() {
        _uiState.update {
            it.copy(
                selectedImages = emptyList(),
                fileName = defaultDocumentName(),
                isGenerating = false,
                progressCurrent = 0,
                progressTotal = 0,
                errorMessage = null,
                generatedPdf = null,
                generatedPreviewBitmap = null
            )
        }
    }

    fun generatePdf(onPdfCreated: () -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.isGenerating) return
        if (snapshot.selectedImages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one image first.") }
            return
        }
        val cleanName = snapshot.fileName.trim()
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a filename for your PDF.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    progressCurrent = 0,
                    progressTotal = snapshot.selectedImages.size,
                    errorMessage = null
                )
            }

            val result = pdfGenerator.generatePdfFromImages(
                images = snapshot.selectedImages,
                outputDirectory = pdfRepository.pdfDirectory,
                requestedFileName = cleanName,
                pageSize = snapshot.pageSize,
                orientation = snapshot.orientation,
                margin = snapshot.margin,
                imageQuality = snapshot.imageQuality,
                onProgress = { current, total ->
                    _uiState.update { state ->
                        state.copy(progressCurrent = current, progressTotal = total)
                    }
                }
            )

            result.onSuccess { generated ->
                val record = PdfFileRecord(
                    id = generated.file.absolutePath,
                    name = generated.file.name,
                    filePath = generated.file.absolutePath,
                    sizeBytes = generated.sizeBytes,
                    pageCount = generated.pageCount,
                    createdAtMillis = generated.file.lastModified()
                )
                val preview = pdfRepository.renderFirstPageThumbnail(generated.file.absolutePath)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedPdf = record,
                        generatedPreviewBitmap = preview,
                        errorMessage = null
                    )
                }
                onPdfCreated()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = err.message ?: "Failed to generate PDF. Please try again."
                    )
                }
            }
        }
    }
}
