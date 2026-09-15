package dvamuch.aspclassiclanguagesupport2.lang

import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import org.junit.Assert.assertEquals
import org.junit.Test

class AspSyntaxHighlighterTest {
    @Test
    fun `uses PhpStorm PHP tag color for ASP delimiters`() {
        val highlighter = AspSyntaxHighlighter()

        listOf(VbTypes.ASP_OPEN, VbTypes.ASP_EXPR_OPEN, VbTypes.ASP_CLOSE).forEach { token ->
            assertEquals("PHP_TAG", highlighter.getTokenHighlights(token).single().externalName)
        }
    }
}
