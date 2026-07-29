package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbIncludeResolveIntegrationTest : BasePlatformTestCase() {
    fun testResolvesFunctionFromRelativeInclude() {
        val target = myFixture.addFileToProject(
            "site/includes/helpers.inc",
            """
            <%
            Function FormatName(value)
                FormatName = UCase(value)
            End Function
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/helpers.inc" -->
            <%
            Response.Write FormatName("guest")
            %>
            """.trimIndent()
        )

        assertUsageResolvesTo(source, "FormatName(\"guest\")", target, "FormatName")
    }

    fun testResolvesVariableFromNestedInclude() {
        val target = myFixture.addFileToProject(
            "site/shared/constants.inc",
            """
            <%
            Dim SharedTitle
            SharedTitle = "ASP Classic"
            %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/includes/bootstrap.inc",
            """
            <!--#include file="../shared/constants.inc" -->
            <%
            Dim BootstrapLoaded
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/bootstrap.inc" -->
            <%
            Response.Write SharedTitle
            %>
            """.trimIndent()
        )

        assertUsageResolvesTo(source, "SharedTitle", target, "SharedTitle")
    }

    fun testResolvesFunctionFromVirtualInclude() {
        val target = myFixture.addFileToProject(
            "shared/navigation.inc",
            """
            <%
            Function RenderNavigation()
                RenderNavigation = "nav"
            End Function
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include virtual="/shared/navigation.inc" -->
            <%
            Response.Write RenderNavigation()
            %>
            """.trimIndent()
        )

        assertUsageResolvesTo(source, "RenderNavigation()", target, "RenderNavigation")
    }

    fun testIncludeCycleDoesNotPreventResolve() {
        myFixture.addFileToProject(
            "site/includes/a.inc",
            """
            <!--#include file="b.inc" -->
            <%
            Dim FromA
            %>
            """.trimIndent()
        )
        val target = myFixture.addFileToProject(
            "site/includes/b.inc",
            """
            <!--#include file="a.inc" -->
            <%
            Function FromCycle()
                FromCycle = "ok"
            End Function
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/a.inc" -->
            <%
            Response.Write FromCycle()
            %>
            """.trimIndent()
        )

        assertUsageResolvesTo(source, "FromCycle()", target, "FromCycle")
    }

    fun testDoesNotResolveSymbolFromFileThatIsNotIncluded() {
        myFixture.addFileToProject(
            "site/includes/unrelated.inc",
            """
            <%
            Dim HiddenValue
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <%
            Response.Write HiddenValue
            %>
            """.trimIndent()
        )

        assertUsageIsUnresolved(source, "HiddenValue")
    }

    fun testLocalDeclarationWinsOverIncludedDeclaration() {
        myFixture.addFileToProject(
            "site/includes/defaults.inc",
            """
            <%
            Dim PageTitle
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/defaults.inc" -->
            <%
            Dim PageTitle
            Response.Write pageTitle
            %>
            """.trimIndent()
        )

        assertUsageResolvesTo(source, "pageTitle", source, "PageTitle")
    }

    fun testDoesNotExposeProcedureLocalFromIncludedFile() {
        myFixture.addFileToProject(
            "site/includes/helpers.inc",
            """
            <%
            Sub PreparePage()
                Dim InternalValue
                InternalValue = "private"
            End Sub
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/helpers.inc" -->
            <%
            Response.Write InternalValue
            %>
            """.trimIndent()
        )

        assertUsageIsUnresolved(source, "InternalValue")
    }

    fun testGoToDeclarationNavigatesToIncludedFile() {
        val target = myFixture.addFileToProject(
            "site/includes/helpers.inc",
            """
            <%
            Function IncludedFunction()
                IncludedFunction = "ok"
            End Function
            %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/helpers.inc" -->
            <%
            Response.Write IncludedFunction()
            %>
            """.trimIndent()
        )
        val usageOffset = source.text.indexOf("IncludedFunction()")
        myFixture.configureFromExistingVirtualFile(source.virtualFile)
        myFixture.editor.caretModel.moveToOffset(usageOffset)

        myFixture.performEditorAction(IdeActions.ACTION_GOTO_DECLARATION)

        val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor
        assertNotNull("Go to Declaration should open an editor", selectedEditor)
        val selectedFile = PsiDocumentManager.getInstance(project).getPsiFile(selectedEditor!!.document)
        assertEquals(target.virtualFile, selectedFile?.virtualFile)
    }

    fun testResolveCachesAreInvalidatedWhenIncludedFileChanges() {
        val target = myFixture.addFileToProject(
            "site/includes/dynamic.inc",
            "<% Dim ExistingValue %>"
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/dynamic.inc" -->
            <%
            Response.Write DynamicValue
            %>
            """.trimIndent()
        )
        val usage = usageId(source, "DynamicValue")
        assertNull("DynamicValue should initially be unresolved", usage.reference?.resolve())

        val documentManager = PsiDocumentManager.getInstance(project)
        val targetDocument = documentManager.getDocument(target)
        assertNotNull("Included file should have a document", targetDocument)
        WriteCommandAction.runWriteCommandAction(project) {
            targetDocument!!.setText("<% Dim ExistingValue, DynamicValue %>")
        }
        documentManager.commitAllDocuments()

        val resolved = usage.reference?.resolve()
        assertNotNull("Resolve should see declarations added after cache creation", resolved)
        assertEquals("DynamicValue", resolved!!.text)
        val topLevelFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(resolved)
        assertEquals(target.virtualFile, topLevelFile.virtualFile)
    }

    private fun assertUsageResolvesTo(
        source: PsiFile,
        usageText: String,
        expectedFile: PsiFile,
        expectedName: String
    ) {
        val usage = usageId(source, usageText)
        val resolved = usage.reference?.resolve()
        assertNotNull("Usage '$usageText' should resolve", resolved)
        assertEquals(expectedName, resolved!!.text)

        val topLevelFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(resolved)
        assertEquals(expectedFile.virtualFile, topLevelFile.virtualFile)
    }

    private fun assertUsageIsUnresolved(source: PsiFile, usageText: String) {
        val usage = usageId(source, usageText)
        assertNull("Usage '$usageText' should stay unresolved", usage.reference?.resolve())
    }

    private fun usageId(source: PsiFile, usageText: String): VbId {
        val usageOffset = source.text.indexOf(usageText)
        assertTrue("Usage '$usageText' should exist", usageOffset >= 0)

        val injected = InjectedLanguageManager.getInstance(project)
            .findInjectedElementAt(source, usageOffset)
        assertNotNull("Usage '$usageText' should be inside injected VBScript", injected)

        return PsiTreeUtil.getParentOfType(injected, VbId::class.java, false)
            ?: failWithElement(injected!!, usageText)
    }

    private fun failWithElement(element: PsiElement, usageText: String): Nothing {
        fail("Usage '$usageText' should point to VbId, got ${element.javaClass.simpleName}: '${element.text}'")
        error("unreachable")
    }
}
