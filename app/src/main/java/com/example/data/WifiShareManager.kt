package com.example.data

/**
 * Nearby Wi-Fi Direct & Local Socket Project Share Manager.
 */
object WifiShareManager {

    /**
     * Serializes a project JSON payload for transmission over local network.
     */
    fun packageProjectForShare(projectJson: String): ByteArray {
        return projectJson.toByteArray(Charsets.UTF_8)
    }

    /**
     * Unpacks received project payload from nearby device.
     */
    fun parseSharedProject(bytes: ByteArray): String {
        return String(bytes, Charsets.UTF_8)
    }
}
