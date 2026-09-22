package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbIfBlockStmt

class AspInjectedRegressionParsingTest : BasePlatformTestCase() {
    fun testParsesCommentOnlyScriptlet() {
        assertInjectedParses(
            """
            <%
            ' first comment
            ' second comment
            %>
            """.trimIndent()
        )
    }

    fun testParsesForNextWithInlineAttributeIf() {
        assertInjectedParses(
            """
            <%
            For iVersion = 0 To versionsCount
            %>
            <option value="<%=versionsArray(COL_VERSION_ID, iVersion)%>" <%If (Not isNull(qsBugVersionID)) And (CStr(versionsArray(COL_VERSION_ID, iVersion)) = CStr(Request("BugVersionID"))) Then Response.Write("selected") : Response.Write("")%> >
              <%=versionsArray(COL_VERSION_NAME, iVersion)%>
            </option>
            <%
            Next
            %>
            """.trimIndent()
        )
    }

    fun testParsesBlockIfAndElseIfAcrossAspScriptlets() {
        val aspPsi = assertNativeAspParses(
            """
            <% If outerCondition Then %>
              <div>
                <% If inlineCondition Then Response.Write("inline") %>
                <% If nestedCondition Then %>
                  <span>nested</span>
                <% ElseIf alternateCondition Then alternateValue = 1 %>
                  <span>alternate</span>
                <% Else %>
                  <span>fallback</span>
                <% End If %>
              </div>
            <% End If %>
            """.trimIndent()
        )
        val blocks = PsiTreeUtil.collectElementsOfType(aspPsi, VbIfBlockStmt::class.java)
        assertEquals("Both cross-scriptlet If blocks must be represented in native ASP PSI", 2, blocks.size)
        assertTrue("Outer If PSI must include intervening HTML", blocks.maxBy { it.textLength }.text.contains("<div>"))
    }

    fun testParsesOutputExpressionWithClosingDelimiterOnNextLine() {
        assertNativeAspParses(
            """
            <td title="description"><%= recordset("Description")
            %></td>
            """.trimIndent()
        )
    }

    fun testParsesSelectCaseWhoseBranchesUseSeparateScriptlets() {
        assertNativeAspParses(
            """
            <% Select Case recordset("SectionID") %>
            <% Case 3 %>
              <a href="trouble.asp">Trouble</a>
            <% Case 6 %>
              <a href="order.asp">Order</a>
            <% Case Else %>
              <span>Unknown</span>
            <% End Select %>
            """.trimIndent()
        )
    }

    private fun assertInjectedParses(aspText: String) {
        assertNativeAspParses(aspText)
    }

    private fun assertNativeAspParses(aspText: String): com.intellij.psi.PsiFile {
        val root = myFixture.configureByText(AspFileType, aspText)
        val aspPsi = root.viewProvider.getPsi(AspLanguage)
        assertNotNull("ASP PSI should exist", aspPsi)

        val errors = PsiTreeUtil.collectElementsOfType(aspPsi, PsiErrorElement::class.java)
        val details = errors.take(8).joinToString("\n") { "${it.errorDescription} :: ${it.text}" }
        assertTrue("Unexpected native ASP parse errors:\n$details", errors.isEmpty())
        return aspPsi!!
    }
}
