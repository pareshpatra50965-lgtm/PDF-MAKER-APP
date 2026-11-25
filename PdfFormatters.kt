package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFileSize(context: Context, bytes: Long): String {
    return Formatter.formatShortFileSize(context, bytes.coerceAtLeast(0L))
}

fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun sharePdfUri(
    context: Context,
    shareUri: Uri,
    documentTitle: String,
    onError: ((String) -> Unit)? = null
) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        putExtra(Intent.EXTRA_SUBJECT, documentTitle)
        clipData = ClipData.newRawUri(documentTitle, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, "Share $documentTitle").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        onError?.invoke("No compatible app found on this device to share PDF files.")
    } catch (t: Throwable) {
        onError?.invoke(t.message ?: "Unable to share PDF file.")
    }
}
