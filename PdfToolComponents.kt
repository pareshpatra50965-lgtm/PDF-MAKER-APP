package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PdfFileRecord
import com.example.model.PdfToolId
import com.example.model.PdfToolSpec
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedBright
import com.example.ui.theme.PdfRedDark
import com.example.ui.theme.PdfRedSoft
import com.example.ui.theme.PdfRedTint
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft

fun toolIconFor(id: PdfToolId): ImageVector = when (id) {
    PdfToolId.IMAGE_TO_PDF -> Icons.Rounded.AddPhotoAlternate
    PdfToolId.SCAN_TO_PDF -> Icons.Rounded.DocumentScanner
    PdfToolId.MERGE_PDF -> Icons.Rounded.LibraryAdd
    PdfToolId.SPLIT_PDF -> Icons.AutoMirrored.Rounded.CallSplit
    PdfToolId.COMPRESS_PDF -> Icons.Rounded.Compress
    PdfToolId.PDF_READER -> Icons.Rounded.MenuBook
    PdfToolId.PDF_EDITOR -> Icons.Rounded.EditNote
    PdfToolId.SIGN_PDF -> Icons.Rounded.Draw
    PdfToolId.WORD_TO_PDF -> Icons.Rounded.Description
    PdfToolId.PDF_TO_IMAGE -> Icons.Rounded.Collections
}

@Composable
fun PdfBrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(PdfRedBright, PdfRedDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PictureAsPdf,
            contentDescription = stringResource(R.string.app_name),
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun PdfSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String = "search_input"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("${testTag}_clear")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PdfToolCard(
    tool: PdfToolSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = tool.isAvailableNow
    val containerColor = if (isReady) {
        PdfRedTint
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isReady) {
        PdfRed.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.id.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isReady) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (isReady) PdfRed else PdfRedSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = toolIconFor(tool.id),
                        contentDescription = stringResource(tool.titleRes),
                        tint = if (isReady) Color.White else PdfRed,
                        modifier = Modifier.size(23.dp)
                    )
                }

                if (isReady) {
                    Surface(
                        shape = CircleShape,
                        color = SuccessGreenSoft
                    ) {
                        Text(
                            text = stringResource(R.string.available_now),
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(tool.titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(tool.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun PdfFileRowCard(
    record: PdfFileRecord,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveCopyClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = onOpenClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_file_card_${record.name}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PdfRedSoft),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = PdfRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "PDF",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = PdfRedDark,
                        lineHeight = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val pageLabel = if (record.pageCount == 1) "1 page" else "${record.pageCount} pages"
                val sizeLabel = formatFileSize(context, record.sizeBytes)
                Text(
                    text = "$pageLabel · $sizeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = formatTimestamp(record.createdAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("share_pdf_${record.name}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.share_pdf),
                    tint = PdfRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("more_pdf_${record.name}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "File options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.view_pdf)) },
                        leadingIcon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.save_to_device)) },
                        leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSaveCopyClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = PdfRedDark) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = PdfRedDark
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}
