package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class VbResolveIntegrationTest : BasePlatformTestCase() {
    fun testResolvesLocalVariableInsideAspScriptlet() {
        myFixture.configureByText(AspFileType, AspTestData.read("smoke/resolve_local.asp"))

        val caretElement = myFixture.elementAtCaret
        assertNotNull("Caret should point to VBScript identifier in injected fragment", caretElement)

        val id = PsiTreeUtil.getParentOfType(caretElement, VbId::class.java, false)
        assertNotNull("Caret should be placed on VbId", id)

        val references = id!!.references
        assertTrue("Expected at least one reference on VbId", references.isNotEmpty())

        val resolved = references.first().resolve()
        assertNotNull("Reference should resolve to a declaration", resolved)
        assertTrue("Resolved declaration should be named 'total'", resolved!!.text.equals("total", ignoreCase = true))
        assertTrue("Resolved declaration should appear before usage", resolved.textOffset < id.textOffset)
    }
}
