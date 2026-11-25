package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.PdfGenerator
import com.example.model.NormalizedCropRect
import com.example.model.SelectedImageItem
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedSoft
import kotlin.math.hypot
import kotlin.math.min

private enum class DragCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    MOVE_BOX
}

@Composable
fun ImageCropRotateDialog(
    item: SelectedImageItem,
    pdfGenerator: PdfGenerator,
    onDismiss: () -> Unit,
    onApply: (rotationDegrees: Int, cropRect: NormalizedCropRect?) -> Unit
) {
    val baseBitmapState by produceState<Result<Bitmap>?>(initialValue = null, item.uri) {
        value = pdfGenerator.loadEditableBitmap(item.uri, maxDimension = 1200)
    }

    var rotationDegrees by remember(item.id) { mutableIntStateOf(item.rotationDegrees) }
    var cropRect by remember(item.id) { mutableStateOf(item.cropRect ?: NormalizedCropRect()) }

    val rotatedBitmap = remember(baseBitmapState, rotationDegrees) {
        val base = baseBitmapState?.getOrNull() ?: return@remember null
        val normRot = ((rotationDegrees % 360) + 360) % 360
        if (normRot == 0) {
            base
        } else {
            val matrix = Matrix().apply { postRotate(normRot.toFloat()) }
            Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF101217)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("crop_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = stringResource(R.string.edit_image_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    TextButton(
                        onClick = {
                            rotationDegrees = 0
                            cropRect = NormalizedCropRect()
                        },
                        modifier = Modifier.testTag("crop_dialog_reset")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.reset_edits), color = Color.White)
                    }
                }

                // Crop Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = rotatedBitmap
                    val loadErr = baseBitmapState?.exceptionOrNull()

                    when {
                        loadErr != null -> {
                            Text(
                                text = loadErr.message ?: "Unable to load image",
                                color = Color.White
                            )
                        }
                        bmp == null -> {
                            CircularProgressIndicator(color = PdfRed)
                        }
                        else -> {
                            InteractiveCropOverlay(
                                bitmap = bmp,
                                cropRect = cropRect,
                                onCropChange = { cropRect = it }
                            )
                        }
                    }
                }

                // Aspect Ratio & Rotate Controls
                Surface(
                    color = Color(0xFF1A1D24),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Quick aspect presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val applyAspect: (Float?) -> Unit = { targetRatio ->
                                val bmp = rotatedBitmap
                                if (targetRatio == null || bmp == null) {
                                    cropRect = NormalizedCropRect()
                                } else {
                                    val imgRatio = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                                    val (wNorm, hNorm) = if (imgRatio > targetRatio) {
                                        (targetRatio / imgRatio).coerceIn(0.2f, 1f) to 0.92f
                                    } else {
                                        0.92f to (imgRatio / targetRatio).coerceIn(0.2f, 1f)
                                    }
                                    val cx = 0.5f
                                    val cy = 0.5f
                                    cropRect = NormalizedCropRect(
                                        left = (cx - wNorm / 2f).coerceIn(0f, 1f),
                                        top = (cy - hNorm / 2f).coerceIn(0f, 1f),
                                        right = (cx + wNorm / 2f).coerceIn(0f, 1f),
                                        bottom = (cy + hNorm / 2f).coerceIn(0f, 1f)
                                    )
                                }
                            }

                            AspectPresetChip(
                                label = "Free",
                                selected = cropRect.isFullImage,
                                onClick = { applyAspect(null) }
                            )
                            AspectPresetChip(
                                label = "1:1",
                                selected = false,
                                onClick = { applyAspect(1f) }
                            )
                            AspectPresetChip(
                                label = "4:3",
                                selected = false,
                                onClick = { applyAspect(4f / 3f) }
                            )
                            AspectPresetChip(
                                label = "A4",
                                selected = false,
                                onClick = { applyAspect(595f / 842f) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    rotationDegrees = (rotationDegrees - 90 + 360) % 360
                                    cropRect = cropRect.rotatedCounterClockwise90()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("crop_rotate_left")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                    contentDescription = stringResource(R.string.rotate_left),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("-90°", color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    rotationDegrees = (rotationDegrees + 90) % 360
                                    cropRect = cropRect.rotatedClockwise90()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("crop_rotate_right")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                                    contentDescription = stringResource(R.string.rotate_right),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+90°", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val finalCrop = if (cropRect.isFullImage) null else cropRect
                                    onApply(rotationDegrees, finalCrop)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("crop_apply_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.apply_edits))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PdfRedSoft,
            selectedLabelColor = PdfRed,
            containerColor = Color(0xFF282C37),
            labelColor = Color.White
        )
    )
}

@Composable
private fun InteractiveCropOverlay(
    bitmap: Bitmap,
    cropRect: NormalizedCropRect,
    onCropChange: (NormalizedCropRect) -> Unit
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { 28.dp.toPx() }
    val minCropSize = 0.14f

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val scale = min(maxW / bitmap.width.toFloat(), maxH / bitmap.height.toFloat())
        val dispW = bitmap.width * scale
        val dispH = bitmap.height * scale
        val leftOffset = (maxW - dispW) / 2f
        val topOffset = (maxH - dispH) / 2f

        val dispWdp = with(density) { dispW.toDp() }
        val dispHdp = with(density) { dispH.toDp() }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Editable image preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(dispWdp, dispHdp)
            )

            var activeDrag by remember { mutableStateOf<DragCorner?>(null) }
            var currentCrop by remember(cropRect) { mutableStateOf(cropRect) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dispW, dispH, leftOffset, topOffset) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val cLeft = leftOffset + currentCrop.left * dispW
                                val cTop = topOffset + currentCrop.top * dispH
                                val cRight = leftOffset + currentCrop.right * dispW
                                val cBottom = topOffset + currentCrop.bottom * dispH

                                val corners = listOf(
                                    DragCorner.TOP_LEFT to Offset(cLeft, cTop),
                                    DragCorner.TOP_RIGHT to Offset(cRight, cTop),
                                    DragCorner.BOTTOM_LEFT to Offset(cLeft, cBottom),
                                    DragCorner.BOTTOM_RIGHT to Offset(cRight, cBottom)
                                )
                                val nearest = corners.minByOrNull { (_, pt) ->
                                    hypot(pos.x - pt.x, pos.y - pt.y)
                                }
                                activeDrag = if (nearest != null &&
                                    hypot(pos.x - nearest.second.x, pos.y - nearest.second.y) <= touchRadiusPx * 1.5f
                                ) {
                                    nearest.first
                                } else if (pos.x in cLeft..cRight && pos.y in cTop..cBottom) {
                                    DragCorner.MOVE_BOX
                                } else {
                                    null
                                }
                            },
                            onDragEnd = { activeDrag = null },
                            onDragCancel = { activeDrag = null },
                            onDrag = { change, dragAmount ->
                                val mode = activeDrag ?: return@detectDragGestures
                                change.consume()
                                val dx = dragAmount.x / dispW.coerceAtLeast(1f)
                                val dy = dragAmount.y / dispH.coerceAtLeast(1f)
                                val c = currentCrop
                                val updated = when (mode) {
                                    DragCorner.TOP_LEFT -> c.copy(
                                        left = (c.left + dx).coerceIn(0f, c.right - minCropSize),
                                        top = (c.top + dy).coerceIn(0f, c.bottom - minCropSize)
                                    )
                                    DragCorner.TOP_RIGHT -> c.copy(
                                        right = (c.right + dx).coerceIn(c.left + minCropSize, 1f),
                                        top = (c.top + dy).coerceIn(0f, c.bottom - minCropSize)
                                    )
                                    DragCorner.BOTTOM_LEFT -> c.copy(
                                        left = (c.left + dx).coerceIn(0f, c.right - minCropSize),
                                        bottom = (c.bottom + dy).coerceIn(c.top + minCropSize, 1f)
                                    )
                                    DragCorner.BOTTOM_RIGHT -> c.copy(
                                        right = (c.right + dx).coerceIn(c.left + minCropSize, 1f),
                                        bottom = (c.bottom + dy).coerceIn(c.top + minCropSize, 1f)
                                    )
                                    DragCorner.MOVE_BOX -> {
                                        val w = c.right - c.left
                                        val h = c.bottom - c.top
                                        val newLeft = (c.left + dx).coerceIn(0f, 1f - w)
                                        val newTop = (c.top + dy).coerceIn(0f, 1f - h)
                                        NormalizedCropRect(
                                            left = newLeft,
                                            top = newTop,
                                            right = newLeft + w,
                                            bottom = newTop + h
                                        )
                                    }
                                }
                                currentCrop = updated
                                onCropChange(updated)
                            }
                        )
                    }
            ) {
                val cLeft = leftOffset + cropRect.left * dispW
                val cTop = topOffset + cropRect.top * dispH
                val cRight = leftOffset + cropRect.right * dispW
                val cBottom = topOffset + cropRect.bottom * dispH

                val fullPath = Path().apply {
                    addRect(Rect(leftOffset, topOffset, leftOffset + dispW, topOffset + dispH))
                }
                val cropPath = Path().apply {
                    addRect(Rect(cLeft, cTop, cRight, cBottom))
                }
                val scrimPath = Path.combine(PathOperation.Difference, fullPath, cropPath)
                drawPath(path = scrimPath, color = Color.Black.copy(alpha = 0.56f))

                // Crop rectangle border
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cLeft, cTop),
                    size = Size(cRight - cLeft, cBottom - cTop),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Rule of thirds subtle grid
                val thirdW = (cRight - cLeft) / 3f
                val thirdH = (cBottom - cTop) / 3f
                for (i in 1..2) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(cLeft + thirdW * i, cTop),
                        end = Offset(cLeft + thirdW * i, cBottom),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(cLeft, cTop + thirdH * i),
                        end = Offset(cRight, cTop + thirdH * i),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Corner handles
                val handleRadius = 8.dp.toPx()
                val handlePoints = listOf(
                    Offset(cLeft, cTop),
                    Offset(cRight, cTop),
                    Offset(cLeft, cBottom),
                    Offset(cRight, cBottom)
                )
                handlePoints.forEach { pt ->
                    drawCircle(color = PdfRed, radius = handleRadius, center = pt)
                    drawCircle(
                        color = Color.White,
                        radius = handleRadius,
                        center = pt,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
