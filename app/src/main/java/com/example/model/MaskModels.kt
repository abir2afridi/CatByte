package com.example.model

enum class MaskType(val displayName: String) {
    NONE("None"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    SPLIT("Split Line"),
    MIRROR("Mirror"),
    FILMSTRIP("Filmstrip")
}

data class MaskData(
    val type: MaskType = MaskType.NONE,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val width: Float = 0.8f,
    val height: Float = 0.8f,
    val rotation: Float = 0f,
    val feather: Float = 0.1f,
    val isInverted: Boolean = false
)