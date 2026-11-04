package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskBlendChromaTest {

    @Test
    fun `rectangle mask covers the centre and clears the corner`() {
        val spec = MaskSpec(kind = MaskKind.RECTANGLE, width = 0.6f, height = 0.6f)
        assertEquals(1f, MaskMath.coverage(spec, 0.5f, 0.5f), 0.001f)
        assertEquals(0f, MaskMath.coverage(spec, 0.95f, 0.95f), 0.001f)
    }

    @Test
    fun `circle mask falls off with radius`() {
        val spec = MaskSpec(kind = MaskKind.CIRCLE, width = 0.6f, height = 0.6f)
        assertEquals(1f, MaskMath.coverage(spec, 0.5f, 0.5f), 0.001f)
        assertEquals(0f, MaskMath.coverage(spec, 0.95f, 0.95f), 0.001f)
    }

    @Test
    fun `split mask keeps one side`() {
        val spec = MaskSpec(kind = MaskKind.SPLIT, centerX = 0.5f, feather = 0.02f)
        assertEquals(1f, MaskMath.coverage(spec, 0.8f, 0.5f), 0.001f)
        assertEquals(0f, MaskMath.coverage(spec, 0.2f, 0.5f), 0.001f)
    }

    @Test
    fun `mirror mask keeps a centre band`() {
        val spec = MaskSpec(kind = MaskKind.MIRROR, width = 0.6f)
        assertEquals(1f, MaskMath.coverage(spec, 0.5f, 0.5f), 0.001f)
        assertEquals(0f, MaskMath.coverage(spec, 0.1f, 0.5f), 0.001f)
    }

    @Test
    fun `filmstrip alternates bands`() {
        val spec = MaskSpec(kind = MaskKind.FILMSTRIP, bands = 4)
        assertEquals(1f, MaskMath.coverage(spec, 0.5f, 0.05f), 0.001f)
        assertEquals(0f, MaskMath.coverage(spec, 0.5f, 0.30f), 0.001f)
    }

    @Test
    fun `invert flips the mask`() {
        val spec = MaskSpec(kind = MaskKind.RECTANGLE, width = 0.6f, height = 0.6f, invert = true)
        assertEquals(0f, MaskMath.coverage(spec, 0.5f, 0.5f), 0.001f)
        assertEquals(1f, MaskMath.coverage(spec, 0.95f, 0.95f), 0.001f)
    }

    @Test
    fun `feather ramps monotonically across the edge`() {
        val spec = MaskSpec(kind = MaskKind.RECTANGLE, width = 0.6f, height = 0.6f, feather = 0.1f)
        val outside = MaskMath.coverage(spec, 0.79f, 0.5f)
        val middle = MaskMath.coverage(spec, 0.75f, 0.5f)
        val inside = MaskMath.coverage(spec, 0.70f, 0.5f)
        assertTrue(outside < middle)
        assertTrue(middle < inside)
        assertEquals(1f, inside, 0.001f)
    }

    @Test
    fun `blend channels follow the standard formulas`() {
        assertEquals(0.25f, BlendMath.blendChannel(0.5f, 0.5f, BlendKind.MULTIPLY), 0.001f)
        assertEquals(0.75f, BlendMath.blendChannel(0.5f, 0.5f, BlendKind.SCREEN), 0.001f)
        assertEquals(0.5f, BlendMath.blendChannel(0.25f, 1f, BlendKind.OVERLAY), 0.001f)
        assertEquals(0.5f, BlendMath.blendChannel(0.2f, 0.7f, BlendKind.DIFFERENCE), 0.001f)
    }

    @Test
    fun `multiply blend darkens a white bottom`() {
        val result = BlendMath.blend(0xFFFFFFFF.toInt(), 0xFF808080.toInt(), BlendKind.MULTIPLY)
        assertEquals(0xFF808080.toInt(), result)
    }

    @Test
    fun `zero opacity leaves the bottom untouched`() {
        assertEquals(0xFF00FF00.toInt(), BlendMath.blend(0xFF00FF00.toInt(), 0xFFFF0000.toInt(), BlendKind.SCREEN, 0f))
    }

    @Test
    fun `chroma key removes the key colour and keeps the rest`() {
        val spec = ChromaSpec()
        assertEquals(0f, ChromaKey.alphaFor(0f, 1f, 0f, spec), 0.001f)
        assertEquals(1f, ChromaKey.alphaFor(1f, 0f, 0f, spec), 0.001f)
        val partial = ChromaKey.alphaFor(0.4f, 0.8f, 0.4f, spec)
        assertTrue("expected partial alpha, was $partial", partial > 0.1f && partial < 0.6f)
    }

    @Test
    fun `spill suppression pulls green back to the neutral level`() {
        val cleaned = ChromaKey.suppressSpill(0xFF646464.toInt(), ChromaSpec(spill = 0f))
        assertEquals(100, (cleaned shr 8) and 0xFF)
        assertEquals(100, (cleaned shr 16) and 0xFF)
    }

    @Test
    fun `apply makes keyed pixels transparent`() {
        val result = ChromaKey.apply(0xFF00FF00.toInt(), ChromaSpec())
        assertEquals(0, (result ushr 24) and 0xFF)
    }
}
