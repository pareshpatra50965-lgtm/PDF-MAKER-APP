package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.PdfImageQuality
import com.example.model.PdfPageMargin
import com.example.model.PdfPageSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pdfSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pdf_maker_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PAGE_SIZE = stringPreferencesKey("default_page_size")
        val PAGE_MARGIN = stringPreferencesKey("default_page_margin")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
    }

    val settingsFlow: Flow<AppSettings> = context.pdfSettingsDataStore.data.map { prefs ->
        val pageSize = prefs[Keys.PAGE_SIZE]?.let { name ->
            PdfPageSize.entries.firstOrNull { it.name == name }
        } ?: PdfPageSize.A4

        val margin = prefs[Keys.PAGE_MARGIN]?.let { name ->
            PdfPageMargin.entries.firstOrNull { it.name == name }
        } ?: PdfPageMargin.STANDARD

        val quality = prefs[Keys.IMAGE_QUALITY]?.let { name ->
            PdfImageQuality.entries.firstOrNull { it.name == name }
        } ?: PdfImageQuality.BALANCED

        AppSettings(
            defaultPageSize = pageSize,
            defaultMargin = margin,
            imageQuality = quality
        )
    }

    suspend fun updatePageSize(pageSize: PdfPageSize) {
        context.pdfSettingsDataStore.edit { prefs ->
            prefs[Keys.PAGE_SIZE] = pageSize.name
        }
    }

    suspend fun updateMargin(margin: PdfPageMargin) {
        context.pdfSettingsDataStore.edit { prefs ->
            prefs[Keys.PAGE_MARGIN] = margin.name
        }
    }

    suspend fun updateImageQuality(quality: PdfImageQuality) {
        context.pdfSettingsDataStore.edit { prefs ->
            prefs[Keys.IMAGE_QUALITY] = quality.name
        }
    }
}
