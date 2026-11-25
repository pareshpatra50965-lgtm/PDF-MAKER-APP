package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class PdfReaderUiState(
    val documentTitle: String = "",
    val pageCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activeFilePath: String? = null,
    val activeContentUri: Uri? = null,
    val renderedPages: Map<Int, Bitmap> = emptyMap()
)

class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfRepository = PdfRepository(application.applicationContext)
    private val rendererMutex = Mutex()
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    private val pageCache = object : LruCache<Int, Bitmap>(12) {}

    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    fun openPdfFromPath(filePath: String) {
        viewModelScope.launch {
            openSource(filePath = filePath, contentUri = null)
        }
    }

    fun openPdfFromUri(uri: Uri) {
        viewModelScope.launch {
            openSource(filePath = null, contentUri = uri)
        }
    }

    fun clearDocument() {
        viewModelScope.launch {
            closeRenderer()
            _uiState.value = PdfReaderUiState()
        }
    }

    private suspend fun openSource(filePath: String?, contentUri: Uri?) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                renderedPages = emptyMap(),
                pageCount = 0,
                activeFilePath = filePath,
                activeContentUri = contentUri
            )
        }

        val result = withContext(Dispatchers.IO) {
            rendererMutex.withLock {
                closeRendererInternal()
                try {
                    val context = getApplication<Application>()
                    val pfd: ParcelFileDescriptor
                    val title: String
                    if (filePath != null) {
                        val file = File(filePath)
                        if (!file.exists()) {
                            return@withLock Result.failure<Pair<String, Int>>(
                                IllegalStateException("PDF file could not be found.")
                            )
                        }
                        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        title = file.name
                    } else if (contentUri != null) {
                        pfd = context.contentResolver.openFileDescriptor(contentUri, "r")
                            ?: return@withLock Result.failure<Pair<String, Int>>(
                                IllegalStateException("Unable to open the selected PDF file.")
                            )
                        title = pdfRepository.resolveImageDisplayName(contentUri)
                    } else {
                        return@withLock Result.failure<Pair<String, Int>>(
                            IllegalArgumentException("No PDF selected.")
                        )
                    }

                    val renderer = PdfRenderer(pfd)
                    val count = renderer.pageCount
                    if (count <= 0) {
                        renderer.close()
                        pfd.close()
                        return@withLock Result.failure<Pair<String, Int>>(
                            IllegalStateException("This PDF has no readable pages.")
                        )
                    }
                    fileDescriptor = pfd
                    pdfRenderer = renderer
                    Result.success(title to count)
                } catch (t: Throwable) {
                    closeRendererInternal()
                    Result.failure(t)
                }
            }
        }

        result.onSuccess { (title, count) ->
            _uiState.update {
                it.copy(
                    documentTitle = title,
                    pageCount = count,
                    isLoading = false,
                    errorMessage = null
                )
            }
            requestPageRender(0)
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = err.message ?: "Failed to open PDF document."
                )
            }
        }
    }

    fun requestPageRender(pageIndex: Int, targetWidthPx: Int = 1120) {
        val cached = pageCache.get(pageIndex)
        if (cached != null && !cached.isRecycled) {
            if (!_uiState.value.renderedPages.containsKey(pageIndex)) {
                _uiState.update { it.copy(renderedPages = it.renderedPages + (pageIndex to cached)) }
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val rendered = rendererMutex.withLock {
                val renderer = pdfRenderer ?: return@withLock null
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null
                try {
                    renderer.openPage(pageIndex).use { page ->
                        val width = targetWidthPx.coerceIn(480, 1400)
                        val scale = width.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                        val height = (page.height * scale).roundToInt().coerceAtLeast(240)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageCache.put(pageIndex, bitmap)
                        bitmap
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (rendered != null) {
                _uiState.update { state ->
                    state.copy(renderedPages = state.renderedPages + (pageIndex to rendered))
                }
            }
        }
    }

    private suspend fun closeRenderer() {
        withContext(Dispatchers.IO) {
            rendererMutex.withLock {
                closeRendererInternal()
            }
        }
    }

    private fun closeRendererInternal() {
        pageCache.evictAll()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {
        }
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {
        }
        pdfRenderer = null
        fileDescriptor = null
    }

    override fun onCleared() {
        super.onCleared()
        closeRendererInternal()
    }
}
