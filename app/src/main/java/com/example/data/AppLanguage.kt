package com.example.data

/**
 * App language options. [tag] is the BCP-47 language tag, or null for the device default.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    BENGALI("bn")
}
