package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.lookup.LookupElementPresentation
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

    fun testCompletesAdoCommandMembersFromServerCreateObject() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Dim cmd
            Set cmd = Server.CreateObject("ADODB.Command")
            cmd.<caret>
            """.trimIndent()
        )

        val variants = completionVariants()

        assertContainsElements(variants, "ActiveConnection", "CommandText", "Parameters", "CreateParameter", "Execute")
        assertFalse("Recordset members must not leak into Command", variants.contains("EOF"))
    }

    fun testCompletesNestedDocumentedMemberChains() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set cmd = Server.CreateObject("ADODB.Command")
            cmd.Parameters.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "Append", "Delete", "Item", "Refresh", "Count")

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set rs = Server.CreateObject("ADODB.Recordset")
            rs.Fields.Item(0).<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "Name", "Type", "Value", "ActualSize", "GetChunk")

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set document = Server.CreateObject("MSXML2.DOMDocument")
            document.documentElement.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "nodeName", "text", "selectNodes", "selectSingleNode")

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set regex = New RegExp
            regex.Execute("value").<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "Count", "Item")
    }

    fun testShowsDocumentedMethodSignaturesInCompletion() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set cmd = Server.CreateObject("ADODB.Command")
            cmd.<caret>
            """.trimIndent()
        )
        assertEquals(
            " ([Name As String], [Type As DataTypeEnum], [Direction As ParameterDirectionEnum], " +
                "[Size As Long], [Value As Variant])",
            completionTailText("CreateParameter")
        )

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set cmd = Server.CreateObject("ADODB.Command")
            cmd.Parameters.<caret>
            """.trimIndent()
        )
        assertEquals(" (Object As Parameter)", completionTailText("Append"))

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set fso = CreateObject("Scripting.FileSystemObject")
            fso.<caret>
            """.trimIndent()
        )
        assertEquals(
            " (FileName As String, [IOMode As IOMode], [Create As Boolean], [Format As Tristate])",
            completionTailText("OpenTextFile")
        )
    }

    fun testShowsParameterInfoAndHighlightsCurrentCreateParameterArgument() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set cmd = Server.CreateObject("ADODB.Command")
            Set parameter = cmd.CreateParameter("user_id", adInteger, <caret>adParamInput, 4, value)
            """.trimIndent()
        )

        assertEquals(
            "CreateParameter([Name As String], [Type As DataTypeEnum], " +
                "<b>[Direction As ParameterDirectionEnum]</b>, [Size As Long], [Value As Variant])",
            myFixture.parameterInfoAtCaret
        )
    }

    fun testParameterInfoCountsOnlyTopLevelCommas() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set cmd = Server.CreateObject("ADODB.Command")
            Set parameter = cmd.CreateParameter(BuildName("last, first", value), adInteger, adParamInput, <caret>4, value)
            """.trimIndent()
        )

        assertEquals(
            "CreateParameter([Name As String], [Type As DataTypeEnum], " +
                "[Direction As ParameterDirectionEnum], <b>[Size As Long]</b>, [Value As Variant])",
            myFixture.parameterInfoAtCaret
        )
    }

    fun testShowsInnerCreateParameterInfoInsideInjectedAsp() {
        myFixture.configureByText(
            AspFileType,
            """
            <%
            Set MM_editCmd = Server.CreateObject("ADODB.Command")
            MM_editCmd.Parameters.Append(MM_editCmd.CreateParameter("complexity", adInteger, <caret>adParamInput, 0, complexity))
            %>
            """.trimIndent()
        )

        assertEquals(
            "CreateParameter([Name As String], [Type As DataTypeEnum], " +
                "<b>[Direction As ParameterDirectionEnum]</b>, [Size As Long], [Value As Variant])",
            myFixture.parameterInfoAtCaret
        )
    }

    fun testRanksVisibleAdoConstantsForExpectedCreateParameterEnum() {
        val declarations = """
            Const adParamInput = 1
            Const adParamOutput = 2
            Const adInteger = 3
            Const adVarChar = 200
            Set cmd = Server.CreateObject("ADODB.Command")
        """.trimIndent()

        myFixture.configureByText(
            VbScriptFileType,
            "$declarations\nSet parameter = cmd.CreateParameter(\"id\", ad<caret>)"
        )
        val typeVariants = completionVariants()
        assertOrderedBefore(typeVariants, "adInteger", "adParamInput")
        assertOrderedBefore(typeVariants, "adVarChar", "adParamOutput")

        myFixture.configureByText(
            VbScriptFileType,
            "$declarations\nSet parameter = cmd.CreateParameter(\"id\", adInteger, ad<caret>)"
        )
        val directionVariants = completionVariants()
        assertOrderedBefore(directionVariants, "adParamInput", "adInteger")
        assertOrderedBefore(directionVariants, "adParamOutput", "adVarChar")
    }

    fun testCompletesAdoRecordsetMembersInsideAsp() {
        myFixture.configureByText(
            AspFileType,
            """
            <%
            Set rs = Server.CreateObject ("adodb.recordset")
            rs.<caret>
            %>
            """.trimIndent()
        )

        val variants = completionVariants()

        assertContainsElements(variants, "EOF", "BOF", "Fields", "MoveNext", "RecordCount")
    }

    fun testPropagatesAdoExecuteReturnType() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set cmd = Server.CreateObject("ADODB.Command")
            Set records = cmd.Execute
            records.<caret>
            """.trimIndent()
        )

        val variants = completionVariants()

        assertContainsElements(variants, "EOF", "Fields", "MoveNext", "Close")
        assertFalse("Command-only members must not leak into returned Recordset", variants.contains("CommandText"))
    }

    fun testCompletesFileSystemObjectAndReturnedTextStream() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set fso = CreateObject("Scripting.FileSystemObject")
            Set input = fso.OpenTextFile("input.txt")
            input.<caret>
            """.trimIndent()
        )

        val variants = completionVariants()

        assertContainsElements(variants, "AtEndOfStream", "ReadLine", "ReadAll", "Close")
        assertFalse("FileSystemObject members must not leak into TextStream", variants.contains("FileExists"))
    }

    fun testResolvesCreateObjectProgIdFromStringAssignment() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            MM_flag = "ADODB.Recordset"
            Set MM_rsUser = Server.CreateObject(MM_flag)
            MM_rsUser.<caret>
            """.trimIndent()
        )

        val variants = completionVariants()

        assertContainsElements(variants, "EOF", "Open", "CursorType")
    }

    fun testCompletesDocumentedXmlHttpAndRegExpObjects() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set http = Server.CreateObject("MSXML2.ServerXMLHTTP")
            http.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "open", "send", "responseText", "responseXML", "setTimeouts")

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set regex = New RegExp
            regex.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "Pattern", "IgnoreCase", "Execute", "Replace", "Test")
    }

    fun testCompletesOtherDocumentedComRootsUsedByTts() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set connection = Server.CreateObject("ADODB.Connection")
            connection.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "ConnectionString", "Open", "Execute", "BeginTrans")

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set values = CreateObject("Scripting.Dictionary")
            values.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "Add", "Exists", "Keys", "Items", "Count")

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set document = Server.CreateObject("Microsoft.XMLDOM")
            document.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "load", "loadXML", "documentElement", "selectSingleNode")

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set http = Server.CreateObject("WinHttp.WinHttpRequest.5.1")
            http.<caret>
            """.trimIndent()
        )
        assertContainsElements(completionVariants(), "Open", "Send", "SetRequestHeader", "ResponseText")
    }

    fun testDoesNotGuessMembersForCustomOrReassignedComObject() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Set component = Server.CreateObject("SMTPRus.SMTPRus.1")
            component.<caret>
            """.trimIndent()
        )
        assertEmpty(completionVariants())

        myFixture.configureByText(
            VbScriptFileType,
            """
            Set component = Server.CreateObject("ADODB.Recordset")
            Set component = Server.CreateObject("Vendor.CustomComponent")
            component.<caret>
            """.trimIndent()
        )
        assertFalse("The latest unknown assignment must clear an older known type", completionVariants().contains("EOF"))
    }

    private fun completionVariants(): List<String> {
        return myFixture.completeBasic()?.map { it.lookupString }.orEmpty()
    }

    private fun completionTailText(name: String): String? {
        val element = myFixture.completeBasic().orEmpty().firstOrNull { it.lookupString == name }
        assertNotNull("Completion item '$name' should be available", element)
        val presentation = LookupElementPresentation()
        element!!.renderElement(presentation)
        return presentation.tailText
    }

    private fun completionVariantsAt(file: PsiFile, marker: String): List<String> {
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val offset = file.text.indexOf(marker) + marker.length
        assertTrue("Completion marker should exist", offset >= marker.length)
        myFixture.editor.caretModel.moveToOffset(offset)
        return completionVariants()
    }

    private fun assertOrderedBefore(variants: List<String>, expectedFirst: String, expectedLater: String) {
        val firstIndex = variants.indexOf(expectedFirst)
        val laterIndex = variants.indexOf(expectedLater)
        assertTrue("Completion must contain '$expectedFirst': $variants", firstIndex >= 0)
        assertTrue("Completion must contain '$expectedLater': $variants", laterIndex >= 0)
        assertTrue(
            "'$expectedFirst' must be ranked before '$expectedLater': $variants",
            firstIndex < laterIndex
        )
    }
}
