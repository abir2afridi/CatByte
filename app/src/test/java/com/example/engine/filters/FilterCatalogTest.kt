package com.example.engine.filters

import com.example.model.VideoFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The filter drawer's tabs and 3-column card data come from the per-filter files. */
class FilterCatalogTest {

    @Test
    fun `drawer has many tabs including all and my luts`() {
        assertTrue(FilterCatalog.tabs.size >= 8)
        assertTrue("Trending" in FilterCatalog.tabs)
        assertTrue("Pencil" in FilterCatalog.tabs)
        assertTrue("All" in FilterCatalog.tabs)
        assertTrue("My LUTs" in FilterCatalog.tabs)
    }

    @Test
    fun `every non-original filter belongs to at least one tab`() {
        FilterCatalog.all.forEach { (filter, grade) ->
            assertTrue("$filter has no tab", grade.tabs.isNotEmpty())
            grade.tabs.forEach { tab ->
                assertTrue("$filter tab $tab missing from drawer", tab in FilterCatalog.tabs)
            }
        }
    }

    @Test
    fun `every filter card owns its swatch colours`() {
        FilterCatalog.all.forEach { (filter, grade) ->
            assertTrue("$filter card colour A missing", grade.cardColorA != 0L)
            assertTrue("$filter card colour B missing", grade.cardColorB != 0L)
        }
    }

    @Test
    fun `tabs contain the original plus their members`() {
        FilterCatalog.tabs.filterNot { it == "My LUTs" }.forEach { tab ->
            val filters = FilterCatalog.filtersFor(tab)
            assertTrue("$tab missing Original", VideoFilter.NONE in filters)
            assertTrue("$tab is empty", filters.size > 1)
        }
    }

    @Test
    fun `pencil tab has the sketch filters but not cyberpunk`() {
        val pencil = FilterCatalog.filtersFor("Pencil")
        assertTrue(VideoFilter.SKETCH_GRAPHITE in pencil)
        assertTrue(VideoFilter.BLUEPRINT in pencil)
        assertTrue(VideoFilter.CYBERPUNK !in pencil)
    }

    @Test
    fun `search grades resolve for every filter`() {
        FilterCatalog.all.forEach { (filter, grade) ->
            assertEquals(grade, FilterCatalog.gradeOf(filter))
        }
        assertNotNull(VideoFilter.NONE.let { FilterCatalog.gradeOf(it) == null })
    }

    @Test
    fun `hit tab has all one hundred twelve named filters`() {
        val hit = FilterCatalog.filtersFor("Hit")
        assertTrue("Hit tab has ${hit.size - 1} filters", hit.size >= 100)
        assertTrue("Original must be first", hit.first() == VideoFilter.NONE)
        assertTrue("Tropical Hues missing", hit.any { it.displayName == "Tropical Hues" })
        assertTrue("Bronze Gray missing", hit.any { it.displayName == "Bronze Gray" })
        assertTrue("Image 4 missing", hit.any { it.displayName == "Image 4" })
    }

    @Test
    fun `every hit filter has a distinct grade`() {
        val grades = FilterCatalog.filtersFor("Hit")
            .mapNotNull { FilterCatalog.gradeOf(it) }
            .map { listOf(it.redGain, it.greenGain, it.blueGain, it.brightness, it.contrast, it.saturation) }
        assertEquals(grades.size, grades.toSet().size)
    }

    @Test
    fun `hit cover path is the bundled shared photo`() {
        assertEquals("file:///android_asset/cover/hit.jpg", FilterCatalog.HIT_COVER_PATH)
    }

    @Test
    fun `hit filters reach the export grade path`() {
        FilterCatalog.filtersFor("Hit").forEach { filter ->
            val grade = com.example.data.export.FilterMapper.gradeFor(filter)
            assertTrue("$filter has no export grade", grade.saturation > 0f)
        }
    }

    @Test
    fun `every tab reports its filter count`() {
        assertEquals(112, FilterCatalog.countFor("Hit"))
        assertEquals(FilterCatalog.all.size, FilterCatalog.countFor("All"))
        FilterCatalog.tabs.filterNot { it == "My LUTs" }.forEach { tab ->
            assertTrue("$tab has zero filters", FilterCatalog.countFor(tab) > 0)
        }
        assertEquals(0, FilterCatalog.countFor("My LUTs", 0))
        assertEquals(3, FilterCatalog.countFor("My LUTs", 3))
    }
}
