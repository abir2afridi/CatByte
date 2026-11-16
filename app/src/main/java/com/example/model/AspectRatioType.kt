package com.example.model

/**
 * Canvas ratios the editor can render and export.
 *
 * `ratioWidth` / `ratioHeight` are always positive so the render graph and
 * the export resolver can compute a frame size without special cases. The
 * "Original" ratio of the bottom navigation is not a constant here: it is
 * resolved to the closest preset of the imported footage at the moment it is
 * picked (see `com.example.engine.AspectMath`).
 */
enum class AspectRatioType(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    RATIO_9_16("9:16", 9f, 16f),
    RATIO_16_9("16:9", 16f, 9f),
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_4_5("4:5", 4f, 5f),
    RATIO_4_3("4:3", 4f, 3f),
    RATIO_3_4("3:4", 3f, 4f),
    RATIO_5_8("5:8", 5f, 8f),
    RATIO_128_27("128:27", 128f, 27f),
    RATIO_2_1("2:1", 2f, 1f),
    RATIO_2_35_1("2.35:1", 2.35f, 1f),
    RATIO_1_85_1("1.85:1", 1.85f, 1f)
}
