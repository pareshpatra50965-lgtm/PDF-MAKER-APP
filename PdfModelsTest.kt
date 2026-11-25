package com.example

import com.example.model.NormalizedCropRect
import com.example.model.PdfPageOrientation
import com.example.model.PdfPageSize
import com.example.model.PdfToolCatalog
import com.example.model.PdfToolId
import com.example.model.ToolCategory
import com.example.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfModelsTest {

    @Test
    fun catalogContainsAllTenRequiredTools() {
        val ids = PdfToolCatalog.allTools.map { it.id }.toSet()
        assertEquals(10, PdfToolCatalog.allTools.size)
        assertEquals(PdfToolId.entries.toSet(), ids)
    }

    @Test
    fun imageToPdfAndReaderAreAvailableInStageOne() {
        assertTrue(PdfToolCatalog.findById(PdfToolId.IMAGE_TO_PDF).isAvailableNow)
        assertTrue(PdfToolCatalog.findById(PdfToolId.PDF_READER).isAvailableNow)
    }

    @Test
    fun categoriesCoverToolsCleanly() {
        val nonAllCategories = ToolCategory.entries.filter { it != ToolCategory.ALL }
        val categorizedIds = PdfToolCatalog.allTools.filter { it.category in nonAllCategories }
        assertEquals(PdfToolCatalog.allTools.size, categorizedIds.size)
    }

    @Test
    fun toolInfoRouteIncludesToolName() {
        assertEquals("tool_info/MERGE_PDF", Routes.toolInfo(PdfToolId.MERGE_PDF))
    }

    @Test
    fun pageSizeIncludesA4LetterAndOriginal() {
        val labels = PdfPageSize.entries.map { it.label }
        assertEquals(listOf("A4", "Letter", "Original"), labels)
    }

    @Test
    fun orientationIncludesPortraitAndLandscape() {
        val labels = PdfPageOrientation.entries.map { it.label }
        assertTrue(labels.contains("Portrait"))
        assertTrue(labels.contains("Landscape"))
    }

    @Test
    fun normalizedCropRectRotationRoundTrips() {
        val original = NormalizedCropRect(left = 0.1f, top = 0.2f, right = 0.7f, bottom = 0.9f)
        assertFalse(original.isFullImage)
        val rotatedTwice = original.rotatedClockwise90().rotatedCounterClockwise90()
        assertEquals(original.left, rotatedTwice.left, 0.001f)
        assertEquals(original.top, rotatedTwice.top, 0.001f)
        assertEquals(original.right, rotatedTwice.right, 0.001f)
        assertEquals(original.bottom, rotatedTwice.bottom, 0.001f)
    }
}
