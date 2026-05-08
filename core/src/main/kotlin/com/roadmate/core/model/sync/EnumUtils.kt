package com.roadmate.core.model.sync

/**
 * Safely converts a string to an enum value, returning [default] if the
 * value doesn't match any enum constant.
 *
 * Used by sync DTO `toEntity()` mappers to prevent crashes from
 * corrupted or newer-version sync data received over Bluetooth.
 */
internal inline fun <reified T : Enum<T>> safeEnumValueOf(value: String, default: T): T =
    try {
        enumValueOf<T>(value)
    } catch (_: IllegalArgumentException) {
        default
    }
