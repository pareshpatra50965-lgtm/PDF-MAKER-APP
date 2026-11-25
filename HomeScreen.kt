package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PdfFileRecord
import com.example.model.PdfToolSpec
import com.example.ui.components.DeletePdfDialog
import com.example.ui.components.PdfBrandMark
import com.example.ui.components.PdfFileRowCard
import com.example.ui.components.PdfSearchBar
import com.example.ui.components.PdfToolCard
import com.example.ui.components.RenamePdfDialog
import com.example.ui.theme.PdfRed
import com.example.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onToolClick: (PdfToolSpec) -> Unit,
    onOpenPdfClick: (PdfFileRecord) -> Unit,
    onSharePdfClick: (PdfFileRecord) -> Unit,
    onSaveCopyClick: (PdfFileRecord) -> Unit,
    onRenamePdf: (PdfFileRecord, String) -> Unit,
    onDeletePdf: (PdfFileRecord) -> Unit,
    onViewAllFilesClick: () -> Unit,
    onQuickCreatePdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val columnCount = if (maxWidth >= 600.dp) 3 else 2
        val toolRows = remember(uiState.filteredTools, columnCount) {
            uiState.filteredTools.chunked(columnCount)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. App Logo and Name
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PdfBrandMark()
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onQuickCreatePdfClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("home_quick_create_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create PDF",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // 2. Search Bar
            item(key = "search_bar") {
                PdfSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.search_placeholder),
                    testTag = "home_search_bar"
                )
            }

            // 3. Recent Files Section
            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_files_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (uiState.totalFilesCount > 0) {
                        TextButton(
                            onClick = onViewAllFilesClick,
                            modifier = Modifier.testTag("home_view_all_files")
                        ) {
                            Text(
                                text = stringResource(R.string.view_all),
                                color = PdfRed
                            )
                        }
                    }
                }
            }

            if (uiState.recentFiles.isEmpty() && uiState.searchQuery.isBlank()) {
                item(key = "empty_recent") {
                    EmptyRecentFilesCard(onCreateClick = onQuickCreatePdfClick)
                }
            } else if (uiState.recentFiles.isNotEmpty()) {
                items(
                    items = uiState.recentFiles,
                    key = { "recent_${it.id}" }
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
            }

            // 4. PDF Tools Grid
            item(key = "tools_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pdf_tools_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.filteredTools.size} tools",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.filteredTools.isEmpty() && uiState.recentFiles.isEmpty()) {
                item(key = "no_search_results") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                    }
                }
            } else {
                items(
                    items = toolRows,
                    key = { row -> row.joinToString("_") { it.id.name } }
                ) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { tool ->
                            PdfToolCard(
                                tool = tool,
                                onClick = { onToolClick(tool) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columnCount - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun EmptyRecentFilesCard(onCreateClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_pdfs_1795549093891),
                contentDescription = stringResource(R.string.empty_recent_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.empty_recent_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.empty_recent_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("home_empty_create_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.create_first_pdf),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
