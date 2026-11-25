package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PdfGenerator
import com.example.model.PdfFileRecord
import com.example.model.PdfPageMargin
import com.example.model.PdfPageOrientation
import com.example.model.PdfPageSize
import com.example.model.SelectedImageItem
import com.example.ui.components.ImageCropRotateDialog
import com.example.ui.components.IndexedDragDropState
import com.example.ui.components.dragHandleReorder
import com.example.ui.components.formatFileSize
import com.example.ui.components.longPressReorder
import com.example.ui.components.rememberIndexedDragDropState
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedDark
import com.example.ui.theme.PdfRedSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.viewmodel.ImageToPdfUiState
import com.example.viewmodel.ImageToPdfViewModel

private const val IMAGE_KEY_PREFIX = "img_"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    viewModel: ImageToPdfViewModel,
    onNavigateBack: () -> Unit,
    onPdfCreatedRefresh: () -> Unit,
    onOpenPdf: (PdfFileRecord) -> Unit,
    onSharePdf: (PdfFileRecord) -> Unit,
    onDownloadPdf: (PdfFileRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingItem by remember { mutableStateOf<SelectedImageItem?>(null) }

    val context = LocalContext.current

    val fallbackOpenImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                    }
                }
                viewModel.addImages(uris)
            }
        }
    )

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.addImages(uris)
            }
        }
    )

    val launchDocumentPicker: () -> Unit = {
        try {
            fallbackOpenImages.launch(arrayOf("image/*"))
        } catch (t: Throwable) {
            viewModel.setError(t.message ?: "Unable to open device file picker.")
        }
    }

    val launchPicker: () -> Unit = {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            try {
                multiPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (_: Throwable) {
                launchDocumentPicker()
            }
        } else {
            launchDocumentPicker()
        }
    }

    editingItem?.let { item ->
        ImageCropRotateDialog(
            item = item,
            pdfGenerator = viewModel.pdfGenerator,
            onDismiss = { editingItem = null },
            onApply = { rotation, crop ->
                viewModel.updateImageEdits(item.id, rotation, crop)
                editingItem = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_image_to_pdf_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("image_to_pdf_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    if (uiState.selectedImages.isNotEmpty() && uiState.generatedPdf == null && !uiState.isGenerating) {
                        TextButton(
                            onClick = { viewModel.clearAllImages() },
                            modifier = Modifier.testTag("image_to_pdf_clear_all")
                        ) {
                            Text(stringResource(R.string.clear_all), color = PdfRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (uiState.selectedImages.isNotEmpty() && uiState.generatedPdf == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        if (uiState.isGenerating) {
                            val total = uiState.progressTotal.coerceAtLeast(1)
                            val fraction = (uiState.progressCurrent.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { fraction },
                                color = PdfRed,
                                trackColor = PdfRedSoft,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                viewModel.generatePdf(onPdfCreated = onPdfCreatedRefresh)
                            },
                            enabled = !uiState.isGenerating && uiState.selectedImages.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("create_pdf_button")
                        ) {
                            if (uiState.isGenerating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Creating page ${uiState.progressCurrent} of ${uiState.progressTotal}…",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.create_pdf)} (${uiState.selectedImages.size})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.generatedPdf != null -> {
                    val created = uiState.generatedPdf!!
                    GeneratedPdfSuccessContent(
                        record = created,
                        previewBitmap = uiState.generatedPreviewBitmap,
                        onOpenPdf = { onOpenPdf(created) },
                        onDownloadPdf = { onDownloadPdf(created) },
                        onSharePdf = { onSharePdf(created) },
                        onCreateAnother = { viewModel.resetForNewPdf() }
                    )
                }

                uiState.selectedImages.isEmpty() -> {
                    EmptyImageSelectionContent(
                        errorMessage = uiState.errorMessage,
                        onSelectImages = launchPicker,
                        onBrowseFiles = launchDocumentPicker,
                        onDismissError = viewModel::dismissError
                    )
                }

                else -> {
                    val listState = rememberLazyListState()
                    val dragDropState = rememberIndexedDragDropState(
                        lazyListState = listState,
                        keyPrefix = IMAGE_KEY_PREFIX,
                        indexForKey = { key ->
                            val id = (key as? String)?.removePrefix(IMAGE_KEY_PREFIX)
                            uiState.selectedImages.indexOfFirst { it.id == id }
                        },
                        onMove = viewModel::moveImage
                    )

                    ImageReorderAndConfigContent(
                        uiState = uiState,
                        listState = listState,
                        dragDropState = dragDropState,
                        pdfGenerator = viewModel.pdfGenerator,
                        onAddMoreClick = launchPicker,
                        onRotateClick = { item -> viewModel.rotateImageClockwise(item.id) },
                        onCropClick = { item -> editingItem = item },
                        onRemove = viewModel::removeImage,
                        onFileNameChange = viewModel::updateFileName,
                        onPageSizeChange = viewModel::updatePageSize,
                        onOrientationChange = viewModel::updateOrientation,
                        onMarginChange = viewModel::updateMargin,
                        onDismissError = viewModel::dismissError
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyImageSelectionContent(
    errorMessage: String?,
    onSelectImages: () -> Unit,
    onBrowseFiles: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (errorMessage != null) {
            ErrorBanner(message = errorMessage, onDismiss = onDismissError)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(PdfRedSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        tint = PdfRed,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.select_images_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.select_images_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSelectImages,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pick_images_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.choose_images),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBrowseFiles,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("browse_image_files_button")
                ) {
                    Text(
                        text = "Browse Device Files",
                        style = MaterialTheme.typography.labelLarge,
                        color = PdfRed
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageReorderAndConfigContent(
    uiState: ImageToPdfUiState,
    listState: LazyListState,
    dragDropState: IndexedDragDropState,
    pdfGenerator: PdfGenerator,
    onAddMoreClick: () -> Unit,
    onRotateClick: (SelectedImageItem) -> Unit,
    onCropClick: (SelectedImageItem) -> Unit,
    onRemove: (String) -> Unit,
    onFileNameChange: (String) -> Unit,
    onPageSizeChange: (PdfPageSize) -> Unit,
    onOrientationChange: (PdfPageOrientation) -> Unit,
    onMarginChange: (PdfPageMargin) -> Unit,
    onDismissError: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "error_banner") {
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { msg ->
                    ErrorBanner(message = msg, onDismiss = onDismissError)
                }
            }
        }

        item(key = "config_card") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.fileName,
                        onValueChange = onFileNameChange,
                        label = { Text(stringResource(R.string.pdf_file_name_label)) },
                        suffix = { Text(".pdf") },
                        singleLine = true,
                        enabled = !uiState.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_filename_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.page_size_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfPageSize.entries.forEach { size ->
                            FilterChip(
                                selected = uiState.pageSize == size,
                                onClick = { if (!uiState.isGenerating) onPageSizeChange(size) },
                                label = { Text(size.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PdfRedSoft,
                                    selectedLabelColor = PdfRed
                                ),
                                modifier = Modifier.testTag("page_size_${size.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.page_orientation_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfPageOrientation.entries.forEach { orientation ->
                            FilterChip(
                                selected = uiState.orientation == orientation,
                                onClick = { if (!uiState.isGenerating) onOrientationChange(orientation) },
                                label = { Text(orientation.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PdfRedSoft,
                                    selectedLabelColor = PdfRed
                                ),
                                modifier = Modifier.testTag("page_orientation_${orientation.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.page_margin_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfPageMargin.entries.forEach { margin ->
                            FilterChip(
                                selected = uiState.margin == margin,
                                onClick = { if (!uiState.isGenerating) onMarginChange(margin) },
                                label = { Text(margin.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PdfRedSoft,
                                    selectedLabelColor = PdfRed
                                ),
                                modifier = Modifier.testTag("page_margin_${margin.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        item(key = "images_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${uiState.selectedImages.size} Page(s)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.page_order_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onAddMoreClick,
                    enabled = !uiState.isGenerating,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PdfRed.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("add_more_images_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = PdfRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_more_images), color = PdfRed)
                }
            }
        }

        itemsIndexed(
            items = uiState.selectedImages,
            key = { _, item -> "$IMAGE_KEY_PREFIX${item.id}" }
        ) { index, item ->
            val itemKey = "$IMAGE_KEY_PREFIX${item.id}"
            val isDragging = dragDropState.draggingItemKey == itemKey
            val translationY = if (isDragging) dragDropState.draggingItemOffset else 0f

            SelectedImageReorderRow(
                index = index,
                item = item,
                itemKey = itemKey,
                isDragging = isDragging,
                translationY = translationY,
                dragDropState = dragDropState,
                pdfGenerator = pdfGenerator,
                enabled = !uiState.isGenerating,
                onRotate = { onRotateClick(item) },
                onCrop = { onCropClick(item) },
                onRemove = { onRemove(item.id) }
            )
        }

        item(key = "bottom_pad") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectedImageReorderRow(
    index: Int,
    item: SelectedImageItem,
    itemKey: String,
    isDragging: Boolean,
    translationY: Float,
    dragDropState: IndexedDragDropState,
    pdfGenerator: PdfGenerator,
    enabled: Boolean,
    onRotate: () -> Unit,
    onCrop: () -> Unit,
    onRemove: () -> Unit
) {
    val editedThumbnail by produceState<Bitmap?>(
        initialValue = null,
        item.uri,
        item.rotationDegrees,
        item.cropRect
    ) {
        if (item.rotationDegrees == 0 && item.cropRect == null) {
            value = null
        } else {
            val base = pdfGenerator.loadEditableBitmap(item.uri, maxDimension = 360).getOrNull()
            value = if (base != null) {
                pdfGenerator.applyTransforms(
                    source = base,
                    rotationDegrees = item.rotationDegrees,
                    cropRect = item.cropRect
                )
            } else {
                null
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isDragging) 1.5.dp else 1.dp,
            color = if (isDragging) PdfRed else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = if (isDragging) 8.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                this.translationY = translationY
                if (isDragging) {
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .then(
                if (enabled) Modifier.longPressReorder(itemKey, dragDropState) else Modifier
            )
            .testTag("selected_image_row_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (enabled) Modifier.dragHandleReorder(itemKey, dragDropState) else Modifier
                    )
                    .testTag("drag_handle_$index"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder page ${index + 1}",
                    tint = if (isDragging) PdfRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Thumbnail with live rotation/crop preview
            Box(contentAlignment = Alignment.TopStart) {
                val thumb = editedThumbnail
                if (thumb != null && !thumb.isRecycled) {
                    Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = enabled, onClick = onCrop)
                    )
                } else {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = enabled, onClick = onCrop)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = PdfRed,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Page ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.rotationDegrees != 0 || item.cropRect != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (item.rotationDegrees != 0) {
                            Surface(shape = CircleShape, color = PdfRedSoft) {
                                Text(
                                    text = "${item.rotationDegrees}°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PdfRedDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (item.cropRect != null) {
                            Surface(shape = CircleShape, color = PdfRedSoft) {
                                Text(
                                    text = "Cropped",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PdfRedDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onRotate,
                enabled = enabled,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("rotate_image_$index")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                    contentDescription = stringResource(R.string.rotate_image),
                    tint = if (enabled) PdfRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            IconButton(
                onClick = onCrop,
                enabled = enabled,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("crop_image_$index")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Crop,
                    contentDescription = stringResource(R.string.crop_rotate_image),
                    tint = if (enabled) PdfRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("remove_image_$index")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.remove_image),
                    tint = if (enabled) PdfRedDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PdfRedSoft,
        border = BorderStroke(1.dp, PdfRed.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = PdfRedDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PdfRedDark,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss error",
                        tint = PdfRedDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratedPdfSuccessContent(
    record: PdfFileRecord,
    previewBitmap: Bitmap?,
    onOpenPdf: () -> Unit,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pdf_created_success_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SuccessGreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.pdf_created_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = PdfRed,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                val pagesLabel = if (record.pageCount == 1) "1 page" else "${record.pageCount} pages"
                Text(
                    text = "$pagesLabel · ${formatFileSize(context, record.sizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (previewBitmap != null && !previewBitmap.isRecycled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val ratio = (previewBitmap.width.toFloat() / previewBitmap.height.toFloat()).coerceIn(0.55f, 1.8f)
                    Surface(
                        onClick = onOpenPdf,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .width(170.dp)
                            .aspectRatio(ratio)
                            .testTag("success_preview_thumbnail")
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Generated PDF page 1 preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOpenPdf,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("success_open_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.preview_pdf))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadPdf,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("success_download_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = PdfRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.download_pdf))
                    }

                    OutlinedButton(
                        onClick = onSharePdf,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("success_share_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = PdfRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.share_pdf))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onCreateAnother,
                    modifier = Modifier.testTag("success_create_another_button")
                ) {
                    Text(stringResource(R.string.create_another), color = PdfRed)
                }
            }
        }
    }
}
