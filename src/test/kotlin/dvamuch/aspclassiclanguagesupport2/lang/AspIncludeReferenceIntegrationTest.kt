package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.html.HTMLLanguage
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlComment
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AspIncludeReferenceIntegrationTest : BasePlatformTestCase() {
    fun testResolvesFileIncludeRelativeToCurrentFile() {
        val target = myFixture.addFileToProject("site/includes/common.inc", "<% Dim sharedValue %>")
        val source = myFixture.addFileToProject(
            "site/pages/default.asp",
            """<!-- #include file="../includes/common.inc" -->"""
        )

        val reference = includeReference(source)

        assertEquals("../includes/common.inc", reference.canonicalText)
        assertEquals(target.virtualFile, reference.resolve()?.containingFile?.virtualFile)
    }

    fun testResolvesVirtualIncludeFromContentRoot() {
        val target = myFixture.addFileToProject("shared/navigation.inc", "<% Sub RenderNavigation() %>")
        val source = myFixture.addFileToProject(
            "site/pages/default.asp",
            """<!--#include virtual="/shared/navigation.inc" -->"""
        )

        val reference = includeReference(source)

        assertEquals("/shared/navigation.inc", reference.canonicalText)
        assertEquals(target.virtualFile, reference.resolve()?.containingFile?.virtualFile)
    }

    fun testMissingIncludeHasUnresolvedReference() {
        val source = myFixture.addFileToProject(
            "site/default.asp",
            """<!-- #INCLUDE FILE='missing.inc' -->"""
        )

        val reference = includeReference(source)

        assertEquals("missing.inc", reference.canonicalText)
        assertNull(reference.resolve())
    }

    fun testOrdinaryHtmlCommentHasNoIncludeReference() {
        val source = myFixture.addFileToProject("site/default.asp", "<!-- ordinary comment -->")
        val comment = htmlComment(source)

        assertEmpty(comment.references)
    }

    fun testIncludeSyntaxInPlainHtmlIsIgnored() {
        val source = myFixture.addFileToProject(
            "site/default.html",
            """<!-- #include file="common.inc" -->"""
        )
        val comment = htmlComment(source)

        assertEmpty(comment.references)
    }

    private fun includeReference(file: PsiFile): PsiReference {
        val references = htmlComment(file).references
        assertEquals("Expected exactly one include reference", 1, references.size)
        return references.single()
    }

    private fun htmlComment(file: PsiFile): XmlComment {
        val htmlFile = file.viewProvider.getPsi(HTMLLanguage.INSTANCE)
        assertNotNull("HTML PSI should exist for ASP file", htmlFile)
        return PsiTreeUtil.findChildOfType(htmlFile, XmlComment::class.java)
            ?: fail("Expected an HTML comment in ASP PSI") as XmlComment
    }
}
