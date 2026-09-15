package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.daemon.quickFix.FileReferenceQuickFixProvider
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.IncorrectOperationException

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

    fun testPlatformContentChangePreservesAspExpressionAfterHttpUrl() {
        val original =
            "<img link=\"http://tts.naukanet.ru/files/filedownload.asp?<%= \"FileID=\" & rsFiles(\"FileID\") %>\">"
        val expected = original.replaceFirst("http://", "https://")
        val file = myFixture.configureByText("BugInfo.asp", original)
        val htmlFile = file.viewProvider.getPsi(HTMLLanguage.INSTANCE)!!
        val httpOffset = file.text.indexOf("http://")
        val leaf = htmlFile.findElementAt(httpOffset)!!
        val attributeValue = PsiTreeUtil.getParentOfType(leaf, XmlAttributeValue::class.java, false)!!
        assertEquals(
            AspXmlAttributeValueManipulator::class.java,
            ElementManipulators.getManipulator(attributeValue).javaClass
        )
        val httpRange = TextRange.from(httpOffset - attributeValue.textRange.startOffset, "http://".length)

        WriteCommandAction.runWriteCommandAction(project) {
            ElementManipulators.handleContentChange(attributeValue, httpRange, "https://")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals(expected, file.text)
    }

    fun testHttpToHttpsInspectionPreservesAspExpression() {
        val inspection = Class.forName("com.intellij.httpClient.http.security.HttpUrlsUsageInspection")
            .getDeclaredConstructor()
            .newInstance() as LocalInspectionTool
        myFixture.enableInspections(inspection)
        val original =
            "<img link=\"http://tts.naukanet.ru/files/filedownload.asp?<%= \"FileID=\" & rsFiles(\"FileID\") %>\">"
        val file = myFixture.configureByText(
            "BugInfo.asp",
            original.replace("http://", "<caret>http://")
        )

        myFixture.doHighlighting()
        val action = myFixture.availableIntentions.singleOrNull {
            it.familyName == "Change prefix to https://"
        }
        assertNotNull("Expected the platform HTTP-to-HTTPS quick-fix", action)
        myFixture.launchAction(action!!)

        assertEquals(original.replaceFirst("http://", "https://"), file.text)
    }

    fun testCompoundAttributeManipulatorRejectsChangesInsideAspCode() {
        val original = "<a href=\"item.asp?id=<%= rs(\"ID\") %>\">item</a>"
        val file = myFixture.configureByText("Item.asp", original)
        val htmlFile = file.viewProvider.getPsi(HTMLLanguage.INSTANCE)!!
        val aspOffset = file.text.indexOf("rs(")
        val leaf = htmlFile.findElementAt(file.text.indexOf("item.asp"))!!
        val attributeValue = PsiTreeUtil.getParentOfType(leaf, XmlAttributeValue::class.java, false)!!
        val aspRange = TextRange.from(aspOffset - attributeValue.textRange.startOffset, 2)

        try {
            WriteCommandAction.runWriteCommandAction(project) {
                ElementManipulators.handleContentChange(attributeValue, aspRange, "other")
            }
            fail("A generic HTML quick-fix must not edit inside embedded ASP code")
        } catch (_: IncorrectOperationException) {
            // Refusing an unsafe generic edit is preferable to losing the scriptlet.
        }

        assertEquals(original, file.text)
    }

    fun testPlainHtmlAttributeKeepsStandardManipulatorBehavior() {
        val file = myFixture.configureByText("index.html", "<a href=\"http://example.com\">item</a>")
        val httpOffset = file.text.indexOf("http://")
        val leaf = file.findElementAt(httpOffset)!!
        val attributeValue = PsiTreeUtil.getParentOfType(leaf, XmlAttributeValue::class.java, false)!!
        val httpRange = TextRange.from(httpOffset - attributeValue.textRange.startOffset, "http://".length)

        WriteCommandAction.runWriteCommandAction(project) {
            ElementManipulators.handleContentChange(attributeValue, httpRange, "https://")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals("<a href=\"https://example.com\">item</a>", file.text)
    }
}
