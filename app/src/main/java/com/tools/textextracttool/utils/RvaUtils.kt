package com.tools.textextracttool.utils

/**
 * Parses and formats RVA values from user input to keep the UI and storage consistent.
 */
object RvaUtils {
    /**
     * Formats a numeric RVA into hex string with 0x prefix.
     */
    fun formatRva(value: Long): String {
        return "0x" + value.toString(16)
    }

    /**
     * Attempts to parse user-provided RVA text in hex (0x) or decimal form.
     */
    fun parseRva(input: String): Long? {
        val value = input.trim()
        if (value.isEmpty()) return null
        return if (value.startsWith("0x", ignoreCase = true)) {
            value.removePrefix("0x").removePrefix("0X").toLongOrNull(16)
        } else {
            value.toLongOrNull(10)
        }
    }

    /**
     * Converts a list of raw text inputs into a cleaned numeric list, dropping invalid entries.
     */
    fun normalizeInputs(inputs: List<String>): List<Long> {
        return inputs.mapNotNull { parseRva(it) }
    }

    /**
     * Converts numeric RVAs to formatted strings to display in the UI.
     */
    fun formatInputs(values: List<Long>): List<String> {
        return values.map { formatRva(it) }
    }
}
