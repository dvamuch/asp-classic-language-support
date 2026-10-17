package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange

/** Shared range helpers for formatter passes that need whole-file context. */
internal object FormattingRangeSupport {
    fun expandToWholeLines(text: String, range: TextRange): TextRange {
        if (text.isEmpty()) return TextRange.EMPTY_RANGE
        val boundedStart = range.startOffset.coerceIn(0, text.length)
        val boundedEnd = range.endOffset.coerceIn(boundedStart, text.length)
        if (boundedStart == 0 && boundedEnd == text.length) return TextRange(0, text.length)

        var start = boundedStart
        while (start > 0 && text[start - 1] != '\n' && text[start - 1] != '\r') start--

        // A selection ending exactly at the beginning of a line does not select
        // that line. Use the preceding character to find the final selected line.
        val endAnchor = if (boundedEnd > boundedStart) boundedEnd - 1 else boundedEnd
        var end = endAnchor.coerceIn(0, text.length)
        while (end < text.length && text[end] != '\n' && text[end] != '\r') end++
        if (end < text.length && text[end] == '\r') end++
        if (end < text.length && text[end] == '\n') end++
        return TextRange(start, end.coerceAtLeast(start))
    }

    fun contains(container: TextRange, nested: TextRange): Boolean =
        nested.startOffset >= container.startOffset && nested.endOffset <= container.endOffset

    fun selectsOffset(range: TextRange?, offset: Int): Boolean =
        range == null || offset >= range.startOffset && offset < range.endOffset

    /**
     * Merge normalized physical lines into an original fragment. The normalized
     * text must preserve the line count; otherwise a partial merge would not
     * have an unambiguous host mapping and is rejected.
     */
    fun mergeSelectedLines(
        original: String,
        normalized: String,
        absoluteStart: Int,
        selectedRange: TextRange
    ): String? {
        val originalLines = splitLines(original)
        val normalizedLines = splitLines(normalized)
        if (originalLines.size != normalizedLines.size) return null

        var hostOffset = absoluteStart
        return buildString(original.length + (normalized.length - original.length).coerceAtLeast(0)) {
            originalLines.indices.forEach { index ->
                val originalLine = originalLines[index]
                val normalizedLine = normalizedLines[index]
                val segmentLength = originalLine.content.length + originalLine.separator.length
                val selected = if (segmentLength > 0) {
                    hostOffset < selectedRange.endOffset && hostOffset + segmentLength > selectedRange.startOffset
                } else {
                    hostOffset >= selectedRange.startOffset && hostOffset < selectedRange.endOffset
                }
                append(if (selected) normalizedLine.content else originalLine.content)
                append(originalLine.separator)
                hostOffset += segmentLength
            }
        }
    }

    private fun splitLines(text: String): List<Line> {
        if (text.isEmpty()) return listOf(Line("", ""))
        val result = mutableListOf<Line>()
        var offset = 0
        while (offset < text.length) {
            var end = offset
            while (end < text.length && text[end] != '\n' && text[end] != '\r') end++
            val content = text.substring(offset, end)
            val separatorStart = end
            if (end < text.length && text[end] == '\r') end++
            if (end < text.length && text[end] == '\n') end++
            result += Line(content, text.substring(separatorStart, end))
            offset = end
        }
        return result
    }

    private data class Line(val content: String, val separator: String)
}
