package com.localstream.app.server

data class ByteRange(
    val start: Long,
    val end: Long,
    val totalLength: Long
) {
    val length: Long get() = (end - start) + 1L

    val contentRangeHeader: String
        get() = "bytes $start-$end/$totalLength"
}

object RangeRequestParser {

    /**
     * Parses an HTTP "Range: bytes=..." header against a known file total length.
     * Returns null if no valid range header was specified (full content 200 OK).
     * Throws InvalidRangeException if requested range cannot be satisfied (416 Range Not Satisfiable).
     */
    fun parseRange(rangeHeader: String?, totalLength: Long): ByteRange? {
        if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=", ignoreCase = true)) {
            return null
        }

        val rangeValue = rangeHeader.substringAfter("=").trim()
        if (rangeValue.contains(",")) {
            // Take the first range for single range streaming
            val firstRange = rangeValue.substringBefore(",").trim()
            return parseSingleRange(firstRange, totalLength)
        }

        return parseSingleRange(rangeValue, totalLength)
    }

    private fun parseSingleRange(rangeValue: String, totalLength: Long): ByteRange {
        if (totalLength <= 0L) {
            throw InvalidRangeException("Total file length is 0")
        }

        val dashIndex = rangeValue.indexOf('-')
        if (dashIndex == -1) {
            throw InvalidRangeException("Malformed Range header: $rangeValue")
        }

        val startStr = rangeValue.substring(0, dashIndex).trim()
        val endStr = rangeValue.substring(dashIndex + 1).trim()

        val start: Long
        val end: Long

        when {
            startStr.isEmpty() && endStr.isNotEmpty() -> {
                // Suffix byte range: "-500" -> last 500 bytes
                val suffixLength = endStr.toLongOrNull() ?: throw InvalidRangeException("Invalid suffix length")
                if (suffixLength <= 0) throw InvalidRangeException("Suffix length must be positive")
                start = (totalLength - suffixLength).coerceAtLeast(0L)
                end = totalLength - 1L
            }
            startStr.isNotEmpty() && endStr.isEmpty() -> {
                // From start to end: "500-"
                start = startStr.toLongOrNull() ?: throw InvalidRangeException("Invalid range start")
                end = totalLength - 1L
            }
            startStr.isNotEmpty() && endStr.isNotEmpty() -> {
                // Explicit bounds: "500-999"
                start = startStr.toLongOrNull() ?: throw InvalidRangeException("Invalid range start")
                val parsedEnd = endStr.toLongOrNull() ?: throw InvalidRangeException("Invalid range end")
                end = parsedEnd.coerceAtMost(totalLength - 1L)
            }
            else -> {
                throw InvalidRangeException("Invalid range specifier")
            }
        }

        if (start > end || start >= totalLength || start < 0) {
            throw InvalidRangeException("Range $start-$end is not satisfiable for length $totalLength")
        }

        return ByteRange(start, end, totalLength)
    }

    class InvalidRangeException(message: String) : Exception(message)
}
