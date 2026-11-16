package com.example.engine

import com.example.model.AspectRatioType
import org.junit.Assert.assertEquals
import org.junit.Test

class AspectMathTest {

    @Test
    fun `landscape video maps to 16 by 9`() {
        assertEquals(AspectRatioType.RATIO_16_9, AspectMath.fromVideo(1920, 1080))
        assertEquals(AspectRatioType.RATIO_16_9, AspectMath.fromVideo(1080, 1920, rotation = 90))
    }

    @Test
    fun `portrait video maps to 9 by 16`() {
        assertEquals(AspectRatioType.RATIO_9_16, AspectMath.fromVideo(1080, 1920))
        assertEquals(AspectRatioType.RATIO_9_16, AspectMath.fromVideo(1920, 1080, rotation = 270))
    }

    @Test
    fun `square and 4 by 5 map to their presets`() {
        assertEquals(AspectRatioType.RATIO_1_1, AspectMath.fromVideo(1080, 1080))
        assertEquals(AspectRatioType.RATIO_4_5, AspectMath.fromVideo(1080, 1350))
    }

    @Test
    fun `unknown dimensions fall back to portrait`() {
        assertEquals(AspectRatioType.RATIO_9_16, AspectMath.fromVideo(0, 0))
    }

    @Test
    fun `the new fixed ratios map to themselves`() {
        assertEquals(AspectRatioType.RATIO_4_3, AspectMath.fromVideo(1920, 1440))
        assertEquals(AspectRatioType.RATIO_3_4, AspectMath.fromVideo(1440, 1920))
        assertEquals(AspectRatioType.RATIO_5_8, AspectMath.fromVideo(1080, 1728))
        assertEquals(AspectRatioType.RATIO_2_1, AspectMath.fromVideo(1920, 960))
        assertEquals(AspectRatioType.RATIO_1_85_1, AspectMath.fromVideo(1850, 1000))
        assertEquals(AspectRatioType.RATIO_128_27, AspectMath.fromVideo(1280, 270))
    }

    @Test
    fun `cinema footage maps to the closest cinema ratio`() {
        assertEquals(AspectRatioType.RATIO_2_35_1, AspectMath.fromVideo(2560, 1080))
        assertEquals(AspectRatioType.RATIO_16_9, AspectMath.fromVideo(1920, 1080))
    }
}
