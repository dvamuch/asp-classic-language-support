package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbEditorFeaturesIntegrationTest : BasePlatformTestCase() {
    fun testCommentsLineInVbScript() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Sub Save()
                <caret>value = 1
            End Sub
            """.trimIndent()
        )

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("Sub Save()\n'    value = 1\nEnd Sub", myFixture.editor.document.text)
    }

    fun testUncommentsLineInVbScript() {
        myFixture.configureByText(
            VbScriptFileType,
            "Sub Save()\n'    <caret>value = 1\nEnd Sub"
        )
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("Sub Save()\n    value = 1\nEnd Sub", myFixture.editor.document.text)
    }

    fun testTogglesLineCommentInsideAspScriptlet() {
        val hostFile = myFixture.configureByText(
            AspFileType,
            """
            <div>before</div>
            <%
                <caret>value = 1
            %>
            <div>after</div>
            """.trimIndent()
        )

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

        val topLevelFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(hostFile)
        val hostDocument = PsiDocumentManager.getInstance(project).getDocument(topLevelFile)
        assertNotNull("ASP host document should exist", hostDocument)
        assertEquals(
            "<div>before</div>\n<%\n'    value = 1\n%>\n<div>after</div>",
            hostDocument!!.text
        )
    }

    fun testTogglesHtmlLineInsideAspWithHtmlCommentSyntax() {
        myFixture.configureByText(AspFileType, "<caret><div class=\"order\">Ready</div>")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("<!--<div class=\"order\">Ready</div>-->", myFixture.editor.document.text)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("<div class=\"order\">Ready</div>", myFixture.editor.document.text)
    }

    fun testTogglesMixedHtmlAndAspSelectionPerPhysicalLine() {
        val original = """
            <div>Before</div>
            <%
                SaveOrder
            %>
            <div>After</div>
        """.trimIndent()
        myFixture.configureByText(AspFileType, original)
        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(
            """
            <!--<div>Before</div>-->
            <%
            '    SaveOrder
            %>
            <!--<div>After</div>-->
            """.trimIndent(),
            myFixture.editor.document.text
        )

        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testTogglesHtmlLineContainingInlineAspExpressionAsHtml() {
        val original = "<td><%= rsOrder(\"OrderName\") %></td>"
        myFixture.configureByText(AspFileType, "<caret>$original")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("<!--$original-->", myFixture.editor.document.text)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testTogglesSingleLineAspStatementInsideItsOpeningDelimiter() {
        val original = "<% SaveOrder() %>"
        myFixture.configureByText(AspFileType, "<caret>$original")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("<% ' SaveOrder() %>", myFixture.editor.document.text)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testTogglesScriptletWhoseOpeningDelimiterSharesTheFirstCodeLine() {
        val original = """
            <% SaveOrder()
                UpdateOrderStatus()
            %>
        """.trimIndent()
        myFixture.configureByText(AspFileType, original)
        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(
            """
            <% ' SaveOrder()
            '    UpdateOrderStatus()
            %>
            """.trimIndent(),
            myFixture.editor.document.text
        )

        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testTogglesScriptletWhoseClosingDelimiterSharesTheLastCodeLine() {
        val original = """
            <%
            SaveOrder()
                UpdateOrderStatus() %>
        """.trimIndent()
        myFixture.configureByText(AspFileType, original)
        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(
            """
            <%
            'SaveOrder()
            '    UpdateOrderStatus() %>
            """.trimIndent(),
            myFixture.editor.document.text
        )

        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testTogglesAspIncludeDirectiveWithoutLeavingItActive() {
        val original = "<!-- #include file=\"shared.inc\" -->"
        myFixture.configureByText(AspFileType, "<caret>$original")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(
            "<!--&lt;!&ndash; #include file=\"shared.inc\" &ndash;&gt;-->",
            myFixture.editor.document.text
        )

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testTogglesHtmlContainingExistingCommentWithoutNestingIt() {
        val original = "<div><!-- existing note --></div>"
        myFixture.configureByText(AspFileType, "<caret>$original")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(
            "<!--<div>&lt;!&ndash; existing note &ndash;&gt;</div>-->",
            myFixture.editor.document.text
        )

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testNestedHtmlCommentEscapingMatchesNativeHtmlCommenter() {
        val original = "<TD><!-- #BeginEditable \"main\" -->"
        myFixture.configureByText("native.html", "<caret>$original")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        val nativeHtmlResult = myFixture.editor.document.text

        myFixture.configureByText(AspFileType, "<caret>$original")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

        assertEquals(nativeHtmlResult, myFixture.editor.document.text)
    }

    fun testTogglesDreamweaverEditableBlockWithMixedAsp() {
        val original = """
            <TR>
                <TD><!-- #BeginEditable "main" -->
                    <% If rsUsers.EOF And rsUsers.BOF Then %>
                        No data was found.
                    <% End If ' end rsUsers.EOF And rsUsers.BOF %>
        """.trimIndent()
        myFixture.configureByText(AspFileType, original)
        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(
            """
            <!--<TR>-->
                <!--<TD>&lt;!&ndash; #BeginEditable "main" &ndash;&gt;-->
                    <% ' If rsUsers.EOF And rsUsers.BOF Then %>
                        <!--No data was found.-->
                    <% ' End If ' end rsUsers.EOF And rsUsers.BOF %>
            """.trimIndent(),
            myFixture.editor.document.text
        )

        myFixture.editor.selectionModel.setSelection(0, myFixture.editor.document.textLength)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testTogglesIndentedHtmlWithoutMovingIndentation() {
        val original = "    <span>Ready</span>"
        myFixture.configureByText(AspFileType, "<caret>$original")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("    <!--<span>Ready</span>-->", myFixture.editor.document.text)

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(original, myFixture.editor.document.text)
    }

    fun testBuildsNestedVbScriptFoldRegions() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Function LoadValue(id)
                If id > 0 Then
                    For i = 1 To id
                        value = value + i
                    Next
                End If
                LoadValue = value
            End Function
            """.trimIndent()
        )

        CodeFoldingManager.getInstance(project).updateFoldRegions(myFixture.editor)
        val regions = myFixture.editor.foldingModel.allFoldRegions

        assertEquals("Function, If and For should be foldable", 3, regions.size)
        assertTrue(regions.all { region -> region.placeholderText == " ... " })
        assertTrue(regions.all { region -> region.isExpanded })
    }

    fun testBuildsFoldRegionsForAllSupportedBlockKinds() {
        myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Property Get Name()
                    Name = "worker"
                End Property

                Sub Run(items)
                    Select Case items.Count
                        Case 0
                            Exit Sub
                    End Select
                    For Each item In items
                        item.Process
                    Next
                    Do While pending
                        pending = LoadNext()
                    Loop
                    While active
                        active = CheckState()
                    Wend
                    With current
                        .Save
                    End With
                    If active Then current.Save
                End Sub
            End Class
            """.trimIndent()
        )

        CodeFoldingManager.getInstance(project).updateFoldRegions(myFixture.editor)
        val regions = myFixture.editor.foldingModel.allFoldRegions

        assertEquals("Class, Property, Sub, Select, For Each, Do, While and With should fold", 8, regions.size)
    }

    fun testBuildsFoldRegionInsideAspScriptlet() {
        myFixture.configureByText(
            AspFileType,
            """
            <%
            Function LoadValue(id)
                LoadValue = id
            End Function
            %>
            """.trimIndent()
        )

        CodeFoldingManager.getInstance(project).updateFoldRegions(myFixture.editor)
        val regions = myFixture.editor.foldingModel.allFoldRegions

        assertTrue("Injected VBScript function should be foldable in ASP", regions.isNotEmpty())
    }

    fun testBuildsAspFoldAcrossScriptletsAndHtml() {
        myFixture.configureByText(
            AspFileType,
            """
            <%
            If showDetails Then
            %>
            <section>
                <p>Details</p>
            </section>
            <%
            End If
            %>
            """.trimIndent()
        )

        CodeFoldingManager.getInstance(project).updateFoldRegions(myFixture.editor)
        val document = myFixture.editor.document
        val foldedTexts = myFixture.editor.foldingModel.allFoldRegions.map { region ->
            document.getText(TextRange(region.startOffset, region.endOffset))
        }

        assertTrue(
            "The semantic If fold should include HTML between its scriptlets: $foldedTexts",
            foldedTexts.any { text -> text.contains("<section>") && text.contains("</section>") }
        )
    }
}
