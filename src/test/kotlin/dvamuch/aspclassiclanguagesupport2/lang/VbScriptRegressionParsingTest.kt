package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbScriptRegressionParsingTest : BasePlatformTestCase() {
    fun testParsesEmptyProcedureParameterLists() {
        assertParses(
            """
            Sub Ping()
                Response.Write "pong"
            End Sub

            Function CurrentUser()
                CurrentUser = "guest"
            End Function
            """.trimIndent()
        )
    }

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

    fun testParsesIndexedAssignmentTarget() {
        assertParses(
            """
            Dim MM_fields
            Dim MM_i

            MM_fields(MM_i+1) = CStr(Request.Form(MM_fields(MM_i)))
            MM_typeArray = Split(MM_columns(MM_i+1), ",")
            """.trimIndent()
        )
    }

    fun testParsesLowercaseHexDigitsWithoutLosingFollowingConstants() {
        assertParses(
            """
            Const adInteger = 3
            Const adModeShareExclusive = &Hc
            Const adErrPropNotAllSettable = &He9f
            Const adParamInput = &H0001
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
