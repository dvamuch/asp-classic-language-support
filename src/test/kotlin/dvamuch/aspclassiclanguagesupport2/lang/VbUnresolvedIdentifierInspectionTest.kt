package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbUnresolvedIdentifierInspection

class VbUnresolvedIdentifierInspectionTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(VbUnresolvedIdentifierInspection::class.java)
    }

    fun testHighlightsOnlyUndeclaredRootIdentifiersWithOptionExplicit() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Option Explicit
            Dim known, component
            known = Abs(1)
            component.UnknownMember = vbCrLf
            Response.Write known
            <warning descr="Undeclared identifier: missing">missing</warning> = known
            <warning descr="Undeclared identifier: VendorApi">VendorApi</warning>.Send known
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testDoesNotInspectFilesWithoutOptionExplicit() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            missing = 1
            Response.Write missing
            VendorApi.Send missing
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testUnderstandsUserClassesMembersAndMe() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Option Explicit
            Class Worker
                Private value

                Private Sub Reset()
                    value = 0
                End Sub

                Public Sub Run()
                    value = 1
                    Me.Reset
                End Sub
            End Class

            Dim worker
            Set worker = New Worker
            worker.Run
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testUnderstandsProcedureParametersReturnNamesAndDynamicMembers() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Option Explicit

            Function FormatValue(ByVal value)
                Dim formatter
                Set formatter = CreateObject("Vendor.Formatter")
                FormatValue = formatter.Apply(value)
            End Function

            Sub WriteValue(ByRef value)
                WScript.Echo FormatValue(value)
            End Sub

            Dim item
            Randomize
            For Each item In Array(1, 2)
                WriteValue item
            Next

            Err.Raise vbObjectError + 1

            For <warning descr="Undeclared identifier: index">index</warning> = 0 To 1
                WScript.Echo <warning descr="Undeclared identifier: index">index</warning>
            Next
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testHighlightsUndeclaredIdentifierInsideAspScriptlet() {
        myFixture.configureByText(
            AspFileType,
            """
            <%
            Option Explicit
            Dim known
            Response.Write known & <warning descr="Undeclared identifier: missing">missing</warning>
            %>
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testResolvesDeclarationsFromAspIncludes() {
        myFixture.addFileToProject(
            "site/shared.inc",
            """
            <%
            Dim SharedValue
            Class SharedWorker
                Public Sub Run()
                End Sub
            End Class
            %>
            """.trimIndent()
        )
        val page = myFixture.addFileToProject(
            "site/page.asp",
            """
            <!-- #include file="shared.inc" -->
            <%
            Option Explicit
            Dim worker
            SharedValue = 1
            Set worker = New SharedWorker
            worker.Run
            %>
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(page.virtualFile)

        myFixture.checkHighlighting()
    }
}
