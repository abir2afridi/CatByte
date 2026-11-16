package com.example.engine

/** Small helper for building a previewable list of transitions in the UI. */
object TransitionCatalog {

    private val twoFrame: Set<TransitionKind> = setOf(
        TransitionKind.CROSS_DISSOLVE,
        TransitionKind.WIPE_LINEAR,
        TransitionKind.WIPE_RADIAL,
        TransitionKind.ORGANIC_WIPE,
        TransitionKind.INK_BLEED,
        TransitionKind.ZOOM,
        TransitionKind.PUSH_LEFT,
        TransitionKind.GLITCH
    )

    fun requiresTwoFrames(kind: TransitionKind): Boolean = kind in twoFrame

    fun ordered(): List<TransitionKind> = TransitionKind.entries

    /** Frame the shader mix is evaluated at, snapped to the project timebase. */
    fun mixFrame(elapsedUs: Long, spec: TransitionSpec, fps: Rational): Long =
        TimeUs(elapsedUs).snapToFrame(fps, FrameRounding.NEAREST).toFrame(fps)

    fun supportsRotation(kind: TransitionKind): Boolean =
        kind == TransitionKind.SPIN || kind == TransitionKind.FLIP

    fun supportsFill(kind: TransitionKind): Boolean =
        kind == TransitionKind.DIP_TO_BLACK || kind == TransitionKind.FLASH_WHITE
}
