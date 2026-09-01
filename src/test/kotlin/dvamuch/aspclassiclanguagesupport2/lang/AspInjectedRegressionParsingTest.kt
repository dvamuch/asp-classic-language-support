package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

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
        assertInjectedParses(
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
    }

    private fun assertInjectedParses(aspText: String) {
        val root = myFixture.configureByText(AspFileType, aspText)
        val aspPsi = root.viewProvider.getPsi(AspLanguage)
        assertNotNull("ASP PSI should exist", aspPsi)

        val hosts = PsiTreeUtil.collectElementsOfType(aspPsi, AspOuterPsiElement::class.java)
        assertTrue("Expected ASP scriptlet blocks", hosts.isNotEmpty())

        val analysisFile = AspVbScriptContext.getForAspFile(aspPsi!!).analysisFile
        val errors = PsiTreeUtil.collectElementsOfType(analysisFile, PsiErrorElement::class.java)
        val details = errors.take(8).joinToString("\n") { "${it.errorDescription} :: ${it.text}" }
        assertTrue("Unexpected injected parse errors:\n$details", errors.isEmpty())
    }
}
