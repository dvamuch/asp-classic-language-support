package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.daemon.quickFix.FileReferenceQuickFixProvider
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AspHtmlFileReferenceIntegrationTest : BasePlatformTestCase() {
    fun testCaseCorrectionPreservesAspExpressionInHref() {
        val targetFile = myFixture.addFileToProject("BugAssignManager.asp", "<html></html>")
        val file = myFixture.configureByText(
            "BugInfo.asp",
            "<a href=\"BugAssignmanager.asp?BugID=<%= rsBug(\"BugID\") %>\"><caret>Assign</a>"
        )
        val fileNameOffset = file.text.indexOf("BugAssignmanager.asp") + 2
        myFixture.editor.caretModel.moveToOffset(fileNameOffset)

        val htmlFile = file.viewProvider.getPsi(HTMLLanguage.INSTANCE)
        assertNotNull("ASP file must expose HTML PSI", htmlFile)
        val reference = htmlFile!!.findReferenceAt(fileNameOffset)
        assertNotNull("Expected an HTML file reference at the href filename", reference)
        val references = if (reference is PsiMultiReference) reference.references.toList() else listOf(reference!!)
        val fileReferences = references.filterIsInstance<FileReference>()
        assertEquals(
            "Expected one ASP-safe file reference; found ${references.map { it.javaClass.name to it.rangeInElement }}",
            1,
            fileReferences.size
        )
        val safeReference = fileReferences.single()
        assertFalse(
            "The unsafe platform FileReference must be shadowed for a compound ASP attribute",
            safeReference.javaClass == FileReference::class.java
        )
        val caseInsensitiveTarget = safeReference.innerSingleResolve(false, htmlFile)
        assertEquals(
            "The safe reference must retain the platform case-correction target",
            targetFile.virtualFile,
            caseInsensitiveTarget?.virtualFile
        )
        assertTrue(
            "The platform must offer its case-correction action for the safe reference",
            FileReferenceQuickFixProvider.registerQuickFix(safeReference).any {
                it.name == "Rename file reference to BugAssignManager.asp"
            }
        )
        WriteCommandAction.runWriteCommandAction(project) {
            safeReference.handleElementRename(targetFile.name)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals(
            "<a href=\"BugAssignManager.asp?BugID=<%= rsBug(\"BugID\") %>\">Assign</a>",
            file.text
        )
        assertNotNull("ASP file must retain its HTML PSI after the quick-fix", htmlFile)
    }

    fun testPlainHrefKeepsTheStandardHtmlFileReference() {
        myFixture.addFileToProject("Target.asp", "<html></html>")
        val file = myFixture.configureByText("Plain.asp", "<a href=\"Target.asp\">Target</a>")
        val offset = file.text.indexOf("Target.asp") + 2
        val htmlFile = file.viewProvider.getPsi(HTMLLanguage.INSTANCE)!!
        val reference = htmlFile.findReferenceAt(offset)
        val references = if (reference is PsiMultiReference) reference.references.toList() else listOfNotNull(reference)

        assertTrue(
            "A plain href should keep the standard HTML file reference: $references",
            references.any { it.javaClass == FileReference::class.java }
        )
        assertFalse(
            "The ASP-safe reference is only needed for compound values: $references",
            references.any { it.javaClass.name == "dvamuch.aspclassiclanguagesupport2.lang.include.SafeAspFileReference" }
        )
    }
}
