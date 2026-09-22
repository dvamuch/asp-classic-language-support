package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.nio.file.Files

class VbResolveIntegrationTest : BasePlatformTestCase() {
    fun testIdeFindsReferenceAtCaretInsideAspScriptlet() {
        myFixture.configureByText(AspFileType, AspTestData.read("smoke/resolve_local.asp"))

        assertResolvesCaretReferenceTo("total")
    }

    fun testIdeFindsReferenceAtCaretInsideVbScriptFile() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Dim total
            total = 10
            Response.Write <caret>total
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("total")
    }

    fun testResolvesFunctionDeclaredAfterUsage() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Response.Write <caret>FormatName("guest")

            Function FormatName(value)
                FormatName = UCase(value)
            End Function
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("FormatName")
    }

    fun testResolvesFunctionDeclaredInLaterAspScriptlet() {
        myFixture.configureByText(
            AspFileType,
            """
            <%
            Response.Write <caret>FormatName()
            %>
            <p>Template gap</p>
            <%
            Function FormatName()
                FormatName = "guest"
            End Function
            %>
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("FormatName")
    }

    fun testAssignmentTargetResolvesToDimDeclaration() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Dim total
            <caret>total = 10
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("total")
    }

    fun testDreamweaverEditActionAssignmentResolvesToDimDeclaration() {
        myFixture.configureByText(
            AspFileType,
            """
            <%@LANGUAGE="VBSCRIPT" CODEPAGE="65001"%>
            <!--#include file="../Connections/connection.asp" -->
            <!--#include file="../inc/sendmailwithlog.asp" -->
            <!--#include file="../core/adovbs.inc" -->
            <%
            ' *** Edit Operations: declare variables

            Dim MM_editAction
            Dim MM_abortEdit
            Dim MM_editQuery
            Dim MM_editCmd

            MM_editAction = CStr(Request.ServerVariables("SCRIPT_NAME"))
            If (Request.QueryString <> "") Then
              <caret>MM_editAction = MM_editAction & "?" & Request.QueryString
            End If
            %>
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("MM_editAction")
    }

    fun testGoToDeclarationWorksInPhysicalAspFile() {
        val source = """
            <%@LANGUAGE="VBSCRIPT" CODEPAGE="65001"%>
            <!--#include file="connection.asp" -->
            <%
            ' *** Edit Operations: declare variables

            Dim MM_editAction
            Dim MM_abortEdit
            Dim MM_editRedirectUrl
            Dim MM_fields
            Dim MM_i

            MM_editAction = CStr(Request.ServerVariables("SCRIPT_NAME"))
            MM_fields(MM_i+1) = CStr(Request.Form(MM_fields(MM_i)))
            If (MM_editRedirectUrl <> "" And Request.QueryString <> "") Then
              MM_editRedirectUrl = MM_editRedirectUrl & "?" & Request.QueryString
            End If
            %>
        """.trimIndent()
        val target = "MM_editRedirectUrl <> \"\""
        val tempDirectory = Files.createTempDirectory("asp-classic-navigation-")
        VfsRootAccess.allowRootAccess(
            testRootDisposable,
            tempDirectory.toString(),
            tempDirectory.toRealPath().toString()
        )
        val path = tempDirectory.resolve("BugAdd.asp")
        Files.writeString(path, source)

        try {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path.toString())
            assertNotNull("Temporary BugAdd.asp should exist", virtualFile)
            assertEquals("BugAdd.asp should use the plugin ASP file type", AspFileType, virtualFile!!.fileType)
            myFixture.configureFromExistingVirtualFile(virtualFile)
            myFixture.editor.caretModel.moveToOffset(source.indexOf(target))

            myFixture.performEditorAction(IdeActions.ACTION_GOTO_DECLARATION)
            val lineNumber = myFixture.editor.document.getLineNumber(myFixture.caretOffset)
            val lineStart = myFixture.editor.document.getLineStartOffset(lineNumber)
            val lineEnd = myFixture.editor.document.getLineEndOffset(lineNumber)
            assertEquals(
                "Go to Declaration should move the editor caret to the Dim statement",
                "Dim MM_editRedirectUrl",
                myFixture.editor.document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd)).trim()
            )
        } finally {
            Files.deleteIfExists(path)
            Files.deleteIfExists(tempDirectory)
        }
    }

    fun testFunctionResultAssignmentResolvesToFunctionDeclaration() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Function FormatName()
                <caret>FormatName = "guest"
            End Function
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("FormatName")
    }

    fun testResolveIsCaseInsensitive() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Dim CustomerName
            Response.Write <caret>customername
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("CustomerName")
    }

    fun testResolvesParameterInsideFunction() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Function FormatName(value)
                FormatName = UCase(<caret>value)
            End Function
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("value")
    }

    fun testPrefersLocalVariableOverGlobalVariable() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Dim value

            Function FormatName()
                Dim value
                value = "local"
                FormatName = <caret>value
            End Function
            """.trimIndent()
        )

        val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = reference.resolve()
        val ancestry = generateSequence(myFixture.file.findElementAt(myFixture.caretOffset)) { it.parent }
            .joinToString(" -> ") { "${it.javaClass.simpleName}:${it.text.take(80)}" }
        assertNotNull("Caret reference should resolve; PSI: $ancestry", resolved)
        val function = PsiTreeUtil.getParentOfType(
            resolved,
            dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt::class.java,
            false
        )
        assertNotNull("Usage should resolve to the variable inside FormatName", function)
    }

    fun testDoesNotResolveProcedureLocalFromAnotherProcedure() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Sub First()
                Dim value
                value = 10
            End Sub

            Sub Second()
                Response.Write <caret>value
            End Sub
            """.trimIndent()
        )

        val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNull("A local from another procedure must not resolve", reference.resolve())
    }

    fun testResolvesMemberOfUserClassInstance() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Public Sub Run()
                End Sub
            End Class

            Dim worker
            Set worker = New Worker
            worker.<caret>Run
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("Run")
    }

    fun testResolvesUserClassMemberInsideWithBlock() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Public Sub Run()
                End Sub
            End Class

            Dim worker
            Set worker = New Worker
            With worker
                .<caret>Run
            End With
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("Run")
    }

    fun testOptionExplicitDisablesImplicitAssignmentDeclaration() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Option Explicit
            missing = 1
            Response.Write <caret>missing
            """.trimIndent()
        )

        val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNull("Option Explicit must not turn a bare assignment into a declaration", reference.resolve())
    }

    fun testUnknownObjectMemberDoesNotFallBackToUnrelatedGlobalSymbol() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Sub Run()
            End Sub

            unknown.<caret>Run
            """.trimIndent()
        )

        val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNull("A dotted member must not resolve by its bare name", reference.resolve())
    }

    fun testResolvesMemberOnDirectNewExpression() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Public Sub Run()
                End Sub
            End Class

            (New Worker).<caret>Run
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("Run")
    }

    fun testResolvesMemberThroughUserClassReturnValue() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Public Sub Run()
                End Sub
            End Class

            Class Factory
                Public Function CreateWorker()
                    Set CreateWorker = New Worker
                End Function
            End Class

            Dim factory
            Set factory = New Factory
            factory.CreateWorker().<caret>Run
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("Run")
    }

    fun testResolvesPrivateMemberThroughMeInsideClass() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Private Sub Reset()
                End Sub

                Public Sub Run()
                    Me.<caret>Reset
                End Sub
            End Class
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("Reset")
    }

    fun testResolvesMemberInsideNestedWith() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Public Sub Run()
                End Sub
            End Class

            Class Factory
                Public Function Current()
                    Set Current = New Worker
                End Function
            End Class

            Dim factory
            Set factory = New Factory
            With factory
                With .Current()
                    .<caret>Run
                End With
            End With
            """.trimIndent()
        )

        assertResolvesCaretReferenceTo("Run")
    }

    private fun assertResolvesCaretReferenceTo(expectedName: String) {
        val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = reference.resolve()
        val ancestry = generateSequence(myFixture.file.findElementAt(myFixture.caretOffset)) { it.parent }
            .joinToString(" -> ") { "${it.javaClass.simpleName}:${it.text.take(80)}" }
        assertNotNull("Caret reference should resolve; PSI: $ancestry", resolved)
        assertTrue(
            "Expected '$expectedName', got '${resolved?.text}'",
            resolved!!.text.equals(expectedName, ignoreCase = true)
        )
    }
}
