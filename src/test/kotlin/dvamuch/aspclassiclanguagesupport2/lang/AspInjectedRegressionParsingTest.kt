package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AspInjectedRegressionParsingTest : BasePlatformTestCase() {
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

    private fun assertInjectedParses(aspText: String) {
        val root = myFixture.configureByText(AspFileType, aspText)
        val aspPsi = root.viewProvider.getPsi(AspLanguage)
        assertNotNull("ASP PSI should exist", aspPsi)

        val hosts = PsiTreeUtil.collectElementsOfType(aspPsi, AspOuterPsiElement::class.java)
        assertTrue("Expected ASP scriptlet blocks", hosts.isNotEmpty())

        val manager = InjectedLanguageManager.getInstance(project)
        val injectedFiles = linkedSetOf<PsiFile>()
        hosts.forEach { host ->
            val start = host.textRange.startOffset
            val end = host.textRange.endOffset
            for (offset in listOf(start + 2, start + 3, start + 4)) {
                if (offset >= end) continue
                val element = manager.findInjectedElementAt(root, offset) ?: continue
                element.containingFile?.let { injectedFiles.add(it) }
                break
            }
        }

        val errors = injectedFiles.flatMap { PsiTreeUtil.collectElementsOfType(it, PsiErrorElement::class.java) }
        val details = errors.take(8).joinToString("\n") { "${it.errorDescription} :: ${it.text}" }
        assertTrue("Unexpected injected parse errors:\n$details", errors.isEmpty())
    }
}
