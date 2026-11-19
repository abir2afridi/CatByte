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
}
