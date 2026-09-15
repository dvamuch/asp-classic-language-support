package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class AspInjectionIntegrationTest : BasePlatformTestCase() {
    fun testNativeVbscriptContextSpansMultipleScriptlets() {
        val file = myFixture.configureByText(AspFileType, AspTestData.read("smoke/multi_scriptlet.asp"))
        val aspPsi = file.viewProvider.getPsi(AspLanguage)
        assertNotNull("ASP PSI should exist in view provider", aspPsi)
        val usagePrefix = "Response.Write total"
        val usageLineOffset = file.text.indexOf(usagePrefix)
        assertTrue("Test fixture must contain VBScript usage line", usageLineOffset >= 0)

        val idOffset = usageLineOffset + "Response.Write ".length
        val id = PsiTreeUtil.getParentOfType(aspPsi!!.findElementAt(idOffset), VbId::class.java, false)
        assertNotNull("Usage offset should point to VbId", id)

        val resolved = id!!.references.firstOrNull()?.resolve()
        assertNotNull("Reference from the last scriptlet should resolve to declaration from the first scriptlet", resolved)
        assertTrue("Resolved declaration should be named 'total'", resolved!!.text.equals("total", ignoreCase = true))
    }
}
