package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

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
        assertNotNull("Caret reference should resolve", resolved)
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

    private fun assertResolvesCaretReferenceTo(expectedName: String) {
        val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = reference.resolve()
        assertNotNull("Caret reference should resolve", resolved)
        assertTrue(
            "Expected '$expectedName', got '${resolved?.text}'",
            resolved!!.text.equals(expectedName, ignoreCase = true)
        )
    }
}
