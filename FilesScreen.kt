package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FileSortOption
import com.example.model.PdfFileRecord
import com.example.ui.components.DeletePdfDialog
import com.example.ui.components.PdfFileRowCard
import com.example.ui.components.PdfSearchBar
import com.example.ui.components.RenamePdfDialog
import com.example.ui.components.formatFileSize
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedSoft
import com.example.viewmodel.FilesUiState

@Composable
fun FilesScreen(
    uiState: FilesUiState,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (FileSortOption) -> Unit,
    onOpenPdfClick: (PdfFileRecord) -> Unit,
    onSharePdfClick: (PdfFileRecord) -> Unit,
    onSaveCopyClick: (PdfFileRecord) -> Unit,
    onRenamePdf: (PdfFileRecord, String) -> Unit,
    onDeletePdf: (PdfFileRecord) -> Unit,
    onBrowseDevicePdfClick: () -> Unit,
    onCreateNewPdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<PdfFileRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<PdfFileRecord?>(null) }

    renameTarget?.let { target ->
        RenamePdfDialog(
            record = target,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onRenamePdf(target, newName)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        DeletePdfDialog(
            record = target,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeletePdf(target)
                deleteTarget = null
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.nav_files),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val summaryText = if (uiState.files.isEmpty()) {
                        "No saved PDFs"
                    } else {
                        "${uiState.files.size} PDF(s) · ${formatFileSize(context, uiState.totalStorageBytes)}"
                    }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onBrowseDevicePdfClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PdfRed.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("files_browse_device_pdf")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = PdfRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.open_device_pdf), color = PdfRed)
                }
            }

            PdfSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search saved PDFs…",
                testTag = "files_search_bar",
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FileSortOption.entries) { option ->
                    val selected = option == uiState.sortOption
                    FilterChip(
                        selected = selected,
                        onClick = { onSortOptionChange(option) },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PdfRedSoft,
                            selectedLabelColor = PdfRed
                        ),
                        modifier = Modifier.testTag("files_sort_${option.name.lowercase()}")
                    )
                }
            }

            if (uiState.files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_pdfs_1795549093891),
                                contentDescription = stringResource(R.string.empty_files_title),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isBlank()) {
                                    stringResource(R.string.empty_files_title)
                                } else {
                                    stringResource(R.string.no_search_results)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_files_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCreateNewPdfClick,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                modifier = Modifier.testTag("files_empty_create_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.create_first_pdf))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.files,
                        key = { it.id }
                    ) { file ->
                        PdfFileRowCard(
                            record = file,
                            onOpenClick = { onOpenPdfClick(file) },
                            onShareClick = { onSharePdfClick(file) },
                            onSaveCopyClick = { onSaveCopyClick(file) },
                            onRenameClick = { renameTarget = file },
                            onDeleteClick = { deleteTarget = file }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}
