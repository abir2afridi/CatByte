package com.example.engine.filters

import com.example.model.VideoFilter

/**
 * The Filter drawer's tab list. Membership lives in each filter's own file,
 * so adding a filter to a tab never touches this catalog or another filter.
 */
object FilterCatalog {

    /** The drawer tabs (label shown in the sheet). */
    val tabs: List<String> = listOf(
        "Trending", "Pencil", "Cinematic", "Vintage", "Neon", "B&W", "Cool", "Warm", "All", "My LUTs"
    )

    /** Every non-original filter with its grade (and card/tab metadata). */
    val all: List<Pair<VideoFilter, FilterGrade>> = listOf(
        VideoFilter.SKETCH_GRAPHITE to SketchGraphiteFilter.grade,
        VideoFilter.BLUEPRINT to BlueprintFilter.grade,
        VideoFilter.SEPIA_NOTEBOOK to SepiaNotebookFilter.grade,
        VideoFilter.CHARCOAL_SMUDGE to CharcoalSmudgeFilter.grade,
        VideoFilter.HALFTONE_MANGA to HalftoneMangaFilter.grade,
        VideoFilter.CYBERPUNK to CyberpunkFilter.grade,
        VideoFilter.VINTAGE to VintageFilter.grade,
        VideoFilter.GOLDEN_HOUR to GoldenHourFilter.grade,
        VideoFilter.NOIR to NoirFilter.grade,
        VideoFilter.TEAL_ORANGE to TealOrangeFilter.grade,
        VideoFilter.MOODY to MoodyFilter.grade,
        VideoFilter.GLITCH_ART to GlitchArtFilter.grade
    )

    /** The filters shown under one tab (search filters span all tabs). */
    fun filtersFor(tab: String): List<VideoFilter> =
        if (tab == "All" || tab == "My LUTs") {
            listOf(VideoFilter.NONE) + all.map { it.first }
        } else {
            listOf(VideoFilter.NONE) + all.filter { tab in it.second.tabs }.map { it.first }
        }

    fun gradeOf(filter: VideoFilter): FilterGrade? = all.firstOrNull { it.first == filter }?.second
}
