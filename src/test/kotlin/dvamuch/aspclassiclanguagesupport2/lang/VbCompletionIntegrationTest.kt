package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbCompletionIntegrationTest : BasePlatformTestCase() {
    fun testCompletesKeywordsAndBuiltInGlobals() {
        myFixture.configureByText(VbScriptFileType, "<caret>")

        val variants = completionVariants()

        assertContainsElements(variants, "Dim", "Function", "If", "Response", "Server", "CStr")
    }

    fun testCompletesBuiltInObjectMembersAfterDot() {
        myFixture.configureByText(VbScriptFileType, "Response.<caret>")

        val variants = completionVariants()

        assertContainsElements(variants, "Write", "Redirect", "ContentType", "AddHeader")
        assertFalse("Keywords must not be offered as object members", variants.contains("Dim"))
    }

    fun testDoesNotOfferGlobalsAsUnknownObjectMembers() {
        myFixture.configureByText(VbScriptFileType, "customer.<caret>")

        val variants = completionVariants()

        assertFalse("Globals must not be offered after an object dot", variants.contains("Dim"))
        assertFalse("ASP globals must not be offered after an object dot", variants.contains("Response"))
    }

    fun testDoesNotCompleteInsideCommentsOrStrings() {
        myFixture.configureByText(VbScriptFileType, "' Res<caret>")
        assertEmpty(completionVariants())

        myFixture.configureByText(VbScriptFileType, "value = \"Res<caret>\"")
        assertEmpty(completionVariants())
    }

    fun testCompletesVisibleSymbolsAndRespectsProcedureScope() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Const GlobalStatus = 1

            Function LoadCustomer(customerId)
                Dim localCustomer
                implicitBeforeCaret = customerId
                <caret>
                implicitAfterCaret = 2
            End Function

            Function OtherProcedure()
                Dim hiddenLocal
            End Function
            """.trimIndent()
        )

        val variants = completionVariants()

        assertContainsElements(
            variants,
            "GlobalStatus",
            "LoadCustomer",
            "customerId",
            "localCustomer",
            "implicitBeforeCaret"
        )
        assertFalse("A local from another procedure must stay hidden", variants.contains("hiddenLocal"))
        assertFalse("A later implicit assignment is not yet visible", variants.contains("implicitAfterCaret"))
    }

    fun testCompletesSymbolsFromTransitiveRelativeIncludes() {
        myFixture.addFileToProject(
            "site/shared/constants.inc",
            """
            <%
            Const adInteger = 3
            Function SharedFormatter(value)
                SharedFormatter = CStr(value)
            End Function
            %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/includes/bootstrap.inc",
            """
            <!--#include file="../shared/constants.inc" -->
            <% Dim BootstrapLoaded %>
            """.trimIndent()
        )
        val source = myFixture.addFileToProject(
            "site/pages/index.asp",
            """
            <!--#include file="../includes/bootstrap.inc" -->
            <%
            Dim LocalValue
            %>
            """.trimIndent()
        )

        val variants = completionVariantsAt(source, "<%\n")

        assertContainsElements(
            variants,
            "LocalValue",
            "BootstrapLoaded",
            "adInteger",
            "SharedFormatter"
        )
    }

    fun testCompletesBuiltInMembersInsideAspScriptlet() {
        val source = myFixture.addFileToProject(
            "site/default.asp",
            """
            <%
            Response.
            %>
            """.trimIndent()
        )

        val variants = completionVariantsAt(source, "Response.")

        assertContainsElements(variants, "Write", "Redirect", "ContentType")
    }

    fun testAcceptsInjectedAspCompletionWithTab() {
        myFixture.configureByText(AspFileType, "<% Response.<caret> %>")
        val variants = myFixture.completeBasic().orEmpty()
        val write = variants.firstOrNull { it.lookupString == "Write" }
        assertNotNull("Response.Write completion should be available", write)
        myFixture.lookup.currentItem = write

        myFixture.type('\t')

        assertEquals("Response.Write", myFixture.editor.document.text.trim())
    }

    fun testCompletionIsCaseInsensitive() {
        myFixture.configureByText(VbScriptFileType, "res<caret>")

        val variants = completionVariants()

        assertContainsElements(variants, "Response")
    }

    private fun completionVariants(): List<String> {
        return myFixture.completeBasic()?.map { it.lookupString }.orEmpty()
    }

    private fun completionVariantsAt(file: PsiFile, marker: String): List<String> {
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val offset = file.text.indexOf(marker) + marker.length
        assertTrue("Completion marker should exist", offset >= marker.length)
        myFixture.editor.caretModel.moveToOffset(offset)
        return completionVariants()
    }
}
