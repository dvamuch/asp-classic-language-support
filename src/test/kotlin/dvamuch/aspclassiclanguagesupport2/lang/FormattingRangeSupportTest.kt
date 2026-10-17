package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingRangeSupportTest {
    @Test
    fun `range ending at the next line start excludes that line`() {
        val text = "first\nsecond\nthird"

        assertEquals(
            TextRange(0, "first\n".length),
            FormattingRangeSupport.expandToWholeLines(text, TextRange(2, "first\n".length))
        )
    }

    @Test
    fun `selected-line merge preserves unselected CRLF lines exactly`() {
        val original = "If ready Then\r\nvalue=left+right\r\noutside=other+1\r\nEnd If"
        val normalized = "If ready Then\r\n    value = left + right\r\n    outside = other + 1\r\nEnd If"
        val selectedStart = original.indexOf("value=")
        val selectedRange = FormattingRangeSupport.expandToWholeLines(
            original,
            TextRange(selectedStart, selectedStart + "value=left+right".length)
        )

        assertEquals(
            "If ready Then\r\n    value = left + right\r\noutside=other+1\r\nEnd If",
            FormattingRangeSupport.mergeSelectedLines(original, normalized, 0, selectedRange)
        )
    }
}
