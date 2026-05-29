package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbScriptRegressionParsingTest : BasePlatformTestCase() {
    fun testParsesBareCallWithCommaSeparatedArguments() {
        assertParses(
            """
            Response.ContentType = "text/html"
            Response.AddHeader "Content-Type", "text/html;charset=utf-8"
            Response.AddHeader "Cache-Control", "no-cache, no-store, must-revalidate"
            """.trimIndent()
        )
    }

    fun testParsesKeywordLikeMemberAfterDot() {
        assertParses(
            """
            set cmd = Server.CreateObject("ADODB.Command")
            set rs = cmd.Execute()
            """.trimIndent()
        )
    }

    fun testParsesCallResultPropertyChain() {
        assertParses(
            """
            serviceItemId = serviceNode.selectSingleNode("service_item_id").text
            customerName = serviceNode.selectSingleNode("customer_name").text
            """.trimIndent()
        )
    }

    fun testParsesSingleLineIfWithEndIf() {
        assertParses(
            """
            If IsEmpty(qsComplexity) Then Response.Write("selected") End If
            If qsComplexity = -1 Then Response.Write("selected") End If
            """.trimIndent()
        )
    }

    fun testParsesIfBlockCollapsedByAspHtmlGap() {
        assertParses(
            """
            If rowCount = -1 Then End If
            Response.Write Now()
            """.trimIndent()
        )
    }

    private fun assertParses(vbScript: String) {
        val file = myFixture.configureByText(VbScriptFileType, vbScript)
        val errors = PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java)
        val sample = errors.take(5).joinToString("\n") { "${it.errorDescription} :: ${it.text}" }
        assertTrue("Unexpected parser errors:\n$sample", errors.isEmpty())
    }
}
