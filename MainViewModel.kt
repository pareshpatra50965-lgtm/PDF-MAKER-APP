package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PdfRepository
import com.example.data.SettingsRepository
import com.example.model.AppSettings
import com.example.model.FileSortOption
import com.example.model.PdfFileRecord
import com.example.model.PdfImageQuality
import com.example.model.PdfPageMargin
import com.example.model.PdfPageSize
import com.example.model.PdfToolCatalog
import com.example.model.PdfToolSpec
import com.example.model.ToolCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val searchQuery: String = "",
    val recentFiles: List<PdfFileRecord> = emptyList(),
    val filteredTools: List<PdfToolSpec> = PdfToolCatalog.allTools,
    val totalFilesCount: Int = 0
)

data class FilesUiState(
    val searchQuery: String = "",
    val sortOption: FileSortOption = FileSortOption.NEWEST,
    val files: List<PdfFileRecord> = emptyList(),
    val totalStorageBytes: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val pdfRepository = PdfRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _homeSearchQuery = MutableStateFlow("")
    private val _filesSearchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(FileSortOption.NEWEST)
    private val _selectedCategory = MutableStateFlow(ToolCategory.ALL)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    val homeUiState: StateFlow<HomeUiState> = combine(
        pdfRepository.files,
        _homeSearchQuery
    ) { files, query ->
        val trimmed = query.trim()
        val matchingFiles = if (trimmed.isEmpty()) {
            files.take(5)
        } else {
            files.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
        val context = getApplication<Application>()
        val matchingTools = if (trimmed.isEmpty()) {
            PdfToolCatalog.allTools
        } else {
            PdfToolCatalog.allTools.filter { tool ->
                context.getString(tool.titleRes).contains(trimmed, ignoreCase = true) ||
                    context.getString(tool.subtitleRes).contains(trimmed, ignoreCase = true)
            }
        }
        HomeUiState(
            searchQuery = query,
            recentFiles = matchingFiles,
            filteredTools = matchingTools,
            totalFilesCount = files.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    val selectedCategory: StateFlow<ToolCategory> = _selectedCategory.asStateFlow()

    val filteredToolsByCategory: StateFlow<List<PdfToolSpec>> = _selectedCategory
        .combine(MutableStateFlow(PdfToolCatalog.allTools)) { category, tools ->
            if (category == ToolCategory.ALL) tools else tools.filter { it.category == category }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PdfToolCatalog.allTools
        )

    val filesUiState: StateFlow<FilesUiState> = combine(
        pdfRepository.files,
        _filesSearchQuery,
        _sortOption
    ) { files, query, sort ->
        val filtered = if (query.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
        val sorted = when (sort) {
            FileSortOption.NEWEST -> filtered.sortedByDescending { it.createdAtMillis }
            FileSortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            FileSortOption.SIZE -> filtered.sortedByDescending { it.sizeBytes }
        }
        FilesUiState(
            searchQuery = query,
            sortOption = sort,
            files = sorted,
            totalStorageBytes = files.sumOf { it.sizeBytes }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FilesUiState()
    )

    init {
        refreshFiles()
    }

    fun refreshFiles() {
        viewModelScope.launch {
            pdfRepository.refreshFiles()
        }
    }

    fun updateHomeSearch(query: String) {
        _homeSearchQuery.value = query
    }

    fun updateFilesSearch(query: String) {
        _filesSearchQuery.value = query
    }

    fun updateSortOption(option: FileSortOption) {
        _sortOption.value = option
    }

    fun selectToolCategory(category: ToolCategory) {
        _selectedCategory.value = category
    }

    fun renamePdf(record: PdfFileRecord, newName: String) {
        viewModelScope.launch {
            val result = pdfRepository.renamePdf(record, newName)
            result.onSuccess {
                _statusMessage.value = "Renamed to ${it.name}"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to rename PDF"
            }
        }
    }

    fun deletePdf(record: PdfFileRecord) {
        viewModelScope.launch {
            val ok = pdfRepository.deletePdf(record)
            _statusMessage.value = if (ok) "Deleted ${record.name}" else "Could not delete file"
        }
    }

    fun deleteAllPdfs() {
        viewModelScope.launch {
            val count = pdfRepository.deleteAllPdfs()
            _statusMessage.value = if (count > 0) "Removed $count saved PDF(s)" else "No saved PDFs to remove"
        }
    }

    fun exportPdfToUri(sourcePath: String, destinationUri: Uri) {
        viewModelScope.launch {
            val result = pdfRepository.exportPdfToUri(sourcePath, destinationUri)
            result.onSuccess {
                _statusMessage.value = "PDF saved to selected location"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to save copy"
            }
        }
    }

    fun downloadPdfToPublicStorage(
        sourcePath: String,
        displayName: String,
        onLegacyPickDestination: () -> Unit
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            viewModelScope.launch {
                val result = pdfRepository.savePdfToPublicDownloads(sourcePath, displayName)
                result.onSuccess { message ->
                    _statusMessage.value = message
                }.onFailure {
                    onLegacyPickDestination()
                }
            }
        } else {
            onLegacyPickDestination()
        }
    }

    fun updateDefaultPageSize(pageSize: PdfPageSize) {
        viewModelScope.launch { settingsRepository.updatePageSize(pageSize) }
    }

    fun updateDefaultMargin(margin: PdfPageMargin) {
        viewModelScope.launch { settingsRepository.updateMargin(margin) }
    }

    fun updateImageQuality(quality: PdfImageQuality) {
        viewModelScope.launch { settingsRepository.updateImageQuality(quality) }
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
