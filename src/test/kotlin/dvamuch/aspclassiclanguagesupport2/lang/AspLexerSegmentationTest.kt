package dvamuch.aspclassiclanguagesupport2.lang

import org.junit.Assert.assertEquals
import org.junit.Test

class AspLexerSegmentationTest {
    @Test
    fun `splits asp outer and html template regions`() {
        val text = AspTestData.read("smoke/lexer_segments.asp")
        val actual = dumpTokens(text)
        val expected = """
            ASP_TEMPLATE_DATA:<html>\n<body>\n
            ASP_OUTER:<% Dim value %>
            ASP_TEMPLATE_DATA:\n<p>
            ASP_OUTER:<%= value %>
            ASP_TEMPLATE_DATA:</p>\n
            ASP_OUTER:<% value = value + 1 %>
            ASP_TEMPLATE_DATA:\n</body>\n</html>\n
        """.trimIndent()

        assertEquals(expected, actual)
    }

    private fun dumpTokens(text: String): String {
        val lexer = AspLexer()
        lexer.start(text)
        val tokens = ArrayList<String>()
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType.toString()
            val tokenText = text.substring(lexer.tokenStart, lexer.tokenEnd)
                .replace("\r", "\\r")
                .replace("\n", "\\n")
            tokens.add("$tokenType:$tokenText")
            lexer.advance()
        }
        return tokens.joinToString("\n")
    }
}
