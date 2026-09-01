package dvamuch.aspclassiclanguagesupport2.lang

import org.junit.Assert.assertEquals
import org.junit.Test

class AspLexerSegmentationTest {
    @Test
    fun `splits asp outer and html template regions`() {
        val text = AspTestData.read("smoke/lexer_segments.asp")
        val actual = dumpTokens(text).lines()
        assertEquals(4, actual.count { it.startsWith("VBScriptToken.ASP_TEMPLATE_DATA:") })
        assertEquals(2, actual.count { it == "VBScriptToken.ASP_OPEN:<%" })
        assertEquals(1, actual.count { it == "VBScriptToken.ASP_EXPR_OPEN:<%=" })
        assertEquals(3, actual.count { it == "VBScriptToken.ASP_CLOSE:%>" })
        assertEquals(4, actual.count { it == "VBScriptToken.IDENTIFIER:value" })
        assertEquals(1, actual.count { it == "VBScriptToken.DIM:Dim" })
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
