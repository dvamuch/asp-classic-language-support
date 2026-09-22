package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.lang.html.HTMLLanguage
import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.formatter.xml.HtmlCodeStyleSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptCodeStyleSettings
import java.nio.file.Files
import java.nio.file.Path

class AspFormatterIntegrationTest : BasePlatformTestCase() {
    fun testAppliesKeywordCaseAcrossAspScriptletsOnly() {
        val source = """
            <div data-label="if else end if">
            <%
            iF ready tHeN
            Response.Write "else end if"
            %>
            <span>else end if</span>
            <%
            eLsE
            Response.Write "if then"
            EnD iF
            %>
            </div>
        """.trimIndent()
        val file = myFixture.configureByText(AspFileType, source)
        val customSettings = CodeStyle.getSettings(file)
            .getCustomSettings(VbScriptCodeStyleSettings::class.java)
        val previousMode = customSettings.KEYWORD_CASE
        try {
            customSettings.KEYWORD_CASE = VbScriptCodeStyleSettings.KEYWORD_CASE_TITLE
            reformat(file)
            assertTrue(file.text.contains("If ready Then"))
            assertTrue(file.text, Regex("(?m)^[ \\t]*Else[ \\t]*$").containsMatchIn(file.text))
            assertTrue(file.text.contains("End If"))
            assertTrue(file.text.contains("data-label=\"if else end if\""))
            assertTrue(file.text.contains("<span>else end if</span>"))
            assertTrue(file.text.contains("\"else end if\""))
            assertTrue(file.text.contains("\"if then\""))
        } finally {
            customSettings.KEYWORD_CASE = previousMode
        }
    }

    fun testHtmlWrappingUsesFinalAspExpressionWidth() {
        for (padding in 40..60) {
            val source = "<div><input title=\"${"x".repeat(padding)}\" value=\"<%=Session(\"UserID\")%>\" size=\"32\"></div>"
            val file = myFixture.configureByText(AspFileType, source)
            reformat(file)
            val onceFormatted = file.text
            assertEquals(source.filterNot(Char::isWhitespace), onceFormatted.filterNot(Char::isWhitespace))
            assertTrue(onceFormatted.contains("<%= Session(\"UserID\") %>"))
            reformat(file)
            assertEquals("HTML wraps must settle on the first pass (padding=$padding)", onceFormatted, file.text)
        }
    }

    fun testKeepsInlineControlFlowInsideHtmlTag() {
        val source = "<input <% If ready Then %> checked <% End If %> value=\"<%=value%>\">"
        val file = myFixture.configureByText(AspFileType, source)
        reformat(file)
        assertTrue(file.text.contains("<% If ready Then %>"))
        assertTrue(file.text.contains("<% End If %>"))
        val onceFormatted = file.text
        reformat(file)
        assertEquals(onceFormatted, file.text)
    }

    fun testKeepsInlineAspDelimiterPairsInline() {
        assertReformatted(
            """
            <div>
                <%if ready then%>
                <span><%=value+1%></span>
                <%end if%>
            </div>
            """.trimIndent(),
            """
            <div>
                <% If ready Then %>
                    <span><%= value + 1 %></span>
                <% End If %>
            </div>
            """.trimIndent()
        )
    }

    fun testMatchesMultilineAspDelimiterPlacementAndAlignment() {
        assertReformatted(
            """
            <div>
                <%
                If ready Then %>
                <span>Ready</span>
                <% End If
                %>
            </div>
            """.trimIndent(),
            """
            <div>
                <%
                If ready Then
                    %>
                    <span>Ready</span>
                    <%
                End If
                %>
            </div>
            """.trimIndent()
        )
    }

    fun testIndentsBothAspBoundariesAroundHtmlInsideIfBlock() {
        assertReformatted(
            """
            <title>
                <%
                If Not rsBug.EOF Or Not rsBug.BOF Then
                %>
                    Bug:<%= rsBug("BugID") %> - <%= BugTitle %>
                <%
                End If
                %>
            </title>
            """.trimIndent(),
            """
            <title>
                <%
                If Not rsBug.EOF Or Not rsBug.BOF Then
                    %>
                    Bug:<%= rsBug("BugID") %> - <%= BugTitle %>
                    <%
                End If
                %>
            </title>
            """.trimIndent()
        )
    }

    fun testMarketplaceFormattingDemoMatchesExpectedOutput() {
        val demoRoot = Path.of("examples/marketplace-demo")
        val before = Files.readString(demoRoot.resolve("formatting-before.asp"))
        val expected = Files.readString(demoRoot.resolve("formatting-after.asp"))
        val file = myFixture.configureByText("marketplace-formatting-demo.asp", before)

        reformat(file)

        assertEquals("Marketplace before/after files must match the real formatter", expected, file.text)
        val onceFormatted = file.text
        reformat(file)
        assertEquals("Marketplace formatting demo must settle in one pass", onceFormatted, file.text)
    }

    fun testKeepsMultilineAspDelimitersOnLinesWithoutTrailingHtml() {
        val source = """
            <div>
                <%
                If ready Then
                %> <img src="icon.png" width="16" height="16" border="0" align="absmiddle"
                       class="noprint" style="cursor:pointer" title="Send message">
                <%
                End If
                %>
            </div>
        """.trimIndent()
        val file = myFixture.configureByText("delimiter-with-html.asp", source)

        reformat(file)

        assertFalse(file.text, Regex("(?m)^[ \\t]*%>[ \\t]+\\S").containsMatchIn(file.text))
        val onceFormatted = file.text
        reformat(file)
        assertEquals("Multiline delimiter and following HTML must settle in one pass", onceFormatted, file.text)
    }

    fun testCanDisableSpacesInsideAspDelimiters() {
        val source = "<div><%=value+1%></div>"
        val file = myFixture.configureByText("delimiter-spaces.asp", source)
        val customSettings = CodeStyle.getSettings(file)
            .getCustomSettings(VbScriptCodeStyleSettings::class.java)
        val previous = customSettings.SPACE_INSIDE_ASP_DELIMITERS
        try {
            customSettings.SPACE_INSIDE_ASP_DELIMITERS = false
            reformat(file)
            assertEquals("<div><%=value + 1%></div>", file.text)
        } finally {
            customSettings.SPACE_INSIDE_ASP_DELIMITERS = previous
        }
    }

    fun testCanDisableMatchingAspDelimiterPlacement() {
        val source = """
            <%
            value = 1 %>
        """.trimIndent()
        val file = myFixture.configureByText("legacy-delimiter-placement.asp", source)
        val customSettings = CodeStyle.getSettings(file)
            .getCustomSettings(VbScriptCodeStyleSettings::class.java)
        val previous = customSettings.MATCH_ASP_DELIMITER_PLACEMENT
        try {
            customSettings.MATCH_ASP_DELIMITER_PLACEMENT = false
            reformat(file)
            assertTrue(file.text, file.text.startsWith("<%\n"))
            assertTrue(file.text, file.text.contains("value = 1 %>"))
            val onceFormatted = file.text
            reformat(file)
            assertEquals(onceFormatted, file.text)
        } finally {
            customSettings.MATCH_ASP_DELIMITER_PLACEMENT = previous
        }
    }

    fun testDoesNotAccumulateWhitespaceBeforeClosingDelimiter() {
        assertReformatted(
            "<div>\n    <%\n    If ready Then\n        value = 1\n    End If\n  %>\n</div>",
            "<div>\n    <%\n    If ready Then\n        value = 1\n    End If\n    %>\n</div>"
        )
    }

    fun testFormatsAspExpressionInsideJavaScriptString() {
        val source = "<script>const name='<%=value+1%>';if(name){alert(name);}</script>"
        val file = myFixture.configureByText(AspFileType, source)

        // PhpStorm 2026.1's JS formatter calls the template builder with null indent.
        reformat(file)

        assertEquals(source.filterNot(Char::isWhitespace), file.text.filterNot(Char::isWhitespace))
        assertTrue("VBScript should be formatted even inside a JS string", file.text.contains("'<%= value + 1 %>'"))
        val onceFormatted = file.text
        reformat(file)
        assertEquals("Embedded JS/ASP formatting must be stable", onceFormatted, file.text)
    }

    fun testFormatsPureHtmlWithBuiltInHtmlFormatter() {
        val source = """
            <html><body><main><section><span>Text</span></section></main></body></html>
        """.trimIndent()

        val htmlFile = myFixture.configureByText(HtmlFileType.INSTANCE, source)
        reformat(htmlFile)
        val expectedHtml = htmlFile.text

        val aspFile = myFixture.configureByText(AspFileType, source)
        reformat(aspFile)

        assertEquals("ASP template data should use PhpStorm's HTML formatter", expectedHtml, aspFile.text)
    }

    fun testFormatsMultilineScriptletInsideHtmlTree() {
        assertReformatted(
            """
            <div><section>
            <%
            If enabled Then
            value=value+1
            End If
            %>
            <span><%=value%></span>
            </section></div>
            """.trimIndent(),
            """
            <div>
                <section>
                    <%
                    If enabled Then
                        value = value + 1
                    End If
                    %>
                    <span><%= value %></span>
                </section>
            </div>
            """.trimIndent()
        )
    }

    fun testKeepsInlineExpressionInsideHtmlAttribute() {
        assertReformatted(
            "<a class=\"link\" href=\"<%=url%>\"><%=title%></a>",
            "<a class=\"link\" href=\"<%= url %>\"><%= title %></a>"
        )
    }

    fun testAlignsAspExpressionOnContinuationLineInsideHtmlAttribute() {
        for (alignText in listOf(false, true)) {
            val source = """
                <table>
                    <tr>
                        <td valign="top"
                            title="Телефон: <%= rsOrder("ManagerPhone") %>
              <%= CHR(13) & CHR(13) & FormatCrLfStrAsText(rsOrder("Comments")) %>">
                            Manager
                        </td>
                    </tr>
                </table>
            """.trimIndent()
            val file = myFixture.configureByText("attribute-$alignText.asp", source)
            CodeStyle.getSettings(file)
                .getCustomSettings(HtmlCodeStyleSettings::class.java)
                .HTML_ALIGN_TEXT = alignText

            reformat(file)

            val lines = file.text.lines()
            val attributeLine = lines.single { it.trimStart().startsWith("title=") }
            val expressionLine = lines.single { it.trimStart().startsWith("<%= CHR(13)") }
            val attributeIndent = attributeLine.length - attributeLine.trimStart().length
            val expressionIndent = expressionLine.length - expressionLine.trimStart().length
            val continuationIndent = CodeStyle.getSettings(file)
                .getCommonSettings(HTMLLanguage.INSTANCE)
                .indentOptions?.CONTINUATION_INDENT_SIZE ?: 4
            assertEquals(
                "ASP continuation in an attribute should use one continuation indent (alignText=$alignText)",
                attributeIndent + continuationIndent,
                expressionIndent
            )
            assertEquals(source.filterNot(Char::isWhitespace), file.text.filterNot(Char::isWhitespace))
            val onceFormatted = file.text
            reformat(file)
            assertEquals("Attribute formatting must settle in one pass", onceFormatted, file.text)
        }
    }

    fun testSpacesOperatorsWhenOtherAspFragmentsMakeTemporaryVbScriptIncomplete() {
        assertReformatted(
            """
            <% If broken Then %>
            <span><%= unknown( %></span>
            <% If rsOrder("PServiceID")<>"" Then %>
            <%=left+right*2%>
            <% End If %>
            """.trimIndent(),
            """
            <% If broken Then %>
                <span><%= unknown( %></span>
                <% If rsOrder("PServiceID") <> "" Then %>
                    <%= left + right * 2 %>
                <% End If %>
            """.trimIndent()
        )
    }

    fun testNormalizesVbScriptContinuationIndentInsideAsp() {
        assertReformatted(
            """
            <div>
                <%
                Set result = connection.Execute( _
            "Select Field " & _
              "From Table " & _
             "Where ID = " & id _
          )
                %>
            </div>
            """.trimIndent(),
            """
            <div>
                <%
                Set result = connection.Execute( _
                        "Select Field " & _
                        "From Table " & _
                        "Where ID = " & id _
                )
                %>
            </div>
            """.trimIndent()
        )
    }

    fun testPreservesQuotedExpressionInsideLongHtmlAttribute() {
        val source = """
            <img style="cursor:pointer;" title="Скопировать адрес в буфер обмена" alt="Скопировать адрес в буфер обмена" src="/images/copy2buff.gif" width="16" height="16" onClick="copylinkToClipboard(this)" link="http://tts.naukanet.ru/files/filedownload.asp?<%="FileID=" & rsFiles("FileID") %>">
        """.trimIndent()
        val file = myFixture.configureByText("buginfo-regression.asp", source)

        reformat(file)

        assertEquals(
            "Formatting an ASP expression inside an HTML attribute must preserve every non-whitespace character",
            source.filterNot(Char::isWhitespace),
            file.text.filterNot(Char::isWhitespace)
        )
        assertTrue(file.text, file.text.contains("<%= \"FileID=\" & rsFiles(\"FileID\") %>"))
    }

    fun testKeepsVbScriptIndentationAcrossHtmlBetweenScriptlets() {
        assertReformatted(
            """
            <div>
            <%
            If enabled Then
            %>
            <section>Text</section>
            <%
            value=value+1
            End If
            %>
            </div>
            """.trimIndent(),
            """
            <div>
                <%
                If enabled Then
                    %>
                    <section>Text</section>
                    <%
                    value = value + 1
                End If
                %>
            </div>
            """.trimIndent()
        )
    }

    fun testSemanticLayoutPreAndPostPassesDoNotAccumulateIndent() {
        val source = """
            <%
            If enabled Then
                %>
                <table>
                    <tr><td>Text</td></tr>
                </table>
                <%
            End If
            %>
        """.trimIndent()
        val file = myFixture.configureByText("semantic-layout-round-trip.asp", source)
        reformat(file)
        val onceFormatted = file.text
        val processor = AspPostFormatProcessor()
        val settings = CodeStyle.getSettings(file)

        WriteCommandAction.runWriteCommandAction(project) {
            processor.prepareCodeSpacing(file, settings)
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            processor.processText(file, TextRange(0, file.textLength), settings)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals(
            "Semantic indentation removed before the HTML pass must be restored exactly once",
            onceFormatted,
            file.text
        )
    }

    fun testNestedBranchesWithBlankLinesAreIdempotent() {
        val source = """
            <table>
            <%
            If firstCondition Then

              Dim firstValue

              If nestedCondition Then
                %>
                <tr><td><%=firstValue%></td></tr>
                <%
              End If
            End If

            ' A second branch after HTML exposed blank-line indent feedback.
            If secondCondition Then
              Dim secondValue

              secondValue=1
            End If
            %>
            </table>
        """.trimIndent()
        val file = myFixture.configureByText("nested-blank-lines.asp", source)

        reformat(file)
        val afterFirstPass = file.text
        reformat(file)

        assertEquals("Blank lines in nested ASP branches must settle in one pass", afterFirstPass, file.text)
        assertEquals(source.filterNot(Char::isWhitespace), file.text.filterNot(Char::isWhitespace))
    }

    fun testFormatsIncAsMixedAspTemplate() {
        val file = myFixture.configureByText(
            "fragment.inc",
            """
            <div><%
            value=value+1
            %><span><%=value%></span></div>
            """.trimIndent()
        )

        reformat(file)

        assertEquals(
            """
            <div>
                <%
                value = value + 1
                %>
                <span><%= value %></span></div>
            """.trimIndent(),
            file.text
        )
        val onceFormatted = file.text
        reformat(file)
        assertEquals("Formatting an INC file twice should be stable", onceFormatted, file.text)
    }

    fun testKeepsInlineIfAroundHtmlAndKeepsFourSpaceNesting() {
        assertReformatted(
            """
            <% if isAuthor = 0 then %>
            <script type="text/javascript">
                alert('Message' + '\n' +
                    'Second line')
            </script>

            <% end if %>
            """.trimIndent(),
            """
            <% If isAuthor = 0 Then %>
                <script type="text/javascript">
                    alert('Message' + '\n' +
                        'Second line')
                </script>

            <% End If %>
            """.trimIndent()
        )
    }

    fun testNormalizesFirstStatementInsideMultilineScriptlet() {
        assertReformatted(
            """
            <%
            If condition Then
              first=value+1
              second=value+2
            End If
            %>
            """.trimIndent(),
            """
            <%
            If condition Then
                first = value + 1
                second = value + 2
            End If
            %>
            """.trimIndent()
        )
    }

    fun testKeepsInlineElseBranchAroundHtml() {
        assertReformatted(
            """
            <% If ready Then %>
            <p>Ready</p>
            <% Else %>
            <p>Not ready</p>
            <% End If %>
            """.trimIndent(),
            """
            <% If ready Then %>
                <p>Ready</p>
            <% Else %>
                <p>Not ready</p>
            <% End If %>
            """.trimIndent()
        )
    }

    fun testIndentsPublicFunctionBodyInsideClassInIncFile() {
        val file = myFixture.configureByText(
            "deviceDal.inc",
            """
            <%
            class DeviceDal
            public function getById(deviceId)
            set cmd=Server.CreateObject("ADODB.Command")
            set getById=cmd.Execute()
            end function
            end class
            %>
            """.trimIndent()
        )

        reformat(file)

        assertEquals(
            """
            <%
            Class DeviceDal
                Public Function getById(deviceId)
                    Set cmd = Server.CreateObject("ADODB.Command")
                    Set getById = cmd.Execute()
                End Function
            End Class
            %>
            """.trimIndent(),
            file.text
        )
        val onceFormatted = file.text
        reformat(file)
        assertEquals("Formatting a DAL class twice should be stable", onceFormatted, file.text)
    }

    fun testPreservesLargeLeadingScriptletWithManyInlineHosts() {
        val source = buildString {
            appendLine("<%")
            appendLine("Set rsViewers = Server.CreateObject(\"ADODB.Recordset\")")
            appendLine(
                "rsViewers.Source = \"SELECT DISTINCT BV.UserID FROM dbo.BugViewers BV " +
                    "WHERE BV.UserID = 42 ORDER BY BV.UserID\""
            )
            repeat(300) { index ->
                appendLine("viewerValue$index=viewerValue$index+1")
            }
            appendLine("rsViewers.Open()")
            appendLine("rsViewers.Close()")
            appendLine("Set rsViewers = Nothing")
            appendLine("%>")
            appendLine("<html><body><select>")
            repeat(160) { index ->
                appendLine(
                    "<option value=\"$index\" <% If selectedViewer = $index Then " +
                        "Response.Write \" selected\" %>>Viewer $index</option>"
                )
            }
            append("</select></body></html>")
        }
        val file = myFixture.configureByText("large-index.asp", source)
        val originalSkeleton = source.filterNot(Char::isWhitespace)

        reformat(file)

        assertEquals(
            "Formatting must not move or remove code between ASP hosts",
            originalSkeleton,
            file.text.filterNot(Char::isWhitespace)
        )
        assertTrue(file.text.indexOf("rsViewers.Source") < file.text.indexOf("rsViewers.Open"))
        assertTrue(file.text.indexOf("rsViewers.Open") < file.text.indexOf("Set rsViewers = Nothing"))
        assertEquals(160, Regex("selectedViewer").findAll(file.text).count())

        val onceFormatted = file.text
        reformat(file)
        assertEquals("Formatting a large mixed ASP file twice should be stable", onceFormatted, file.text)
    }

    fun testEditorReformatActionFormatsVbScriptContent() {
        val file = myFixture.configureByText(
            "editor-action.asp",
            "<%\nvalue=value+1\n%>"
        )

        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals("<%\nvalue = value + 1\n%>", file.text)
    }

    fun testUserTextCannotCollideWithInternalFragmentMarkers() {
        val source = """
            <%
            markerText = "'__ASP_FORMAT_0_END__"
            ' '__ASP_FORMAT_1_START__ must remain an ordinary user comment
            If ready Then
            value=1
            End If
            %>
            <p><%=markerText%></p>
        """.trimIndent()
        val file = myFixture.configureByText("marker-collision.asp", source)

        reformat(file)

        assertTrue(file.text, file.text.contains("markerText = \"'__ASP_FORMAT_0_END__\""))
        assertTrue(file.text, file.text.contains("' '__ASP_FORMAT_1_START__ must remain an ordinary user comment"))
        assertTrue(file.text, file.text.contains("value = 1"))
        assertEquals(source.filterNot(Char::isWhitespace), file.text.filterNot(Char::isWhitespace))
        val onceFormatted = file.text
        reformat(file)
        assertEquals("Marker-like user text must remain idempotent", onceFormatted, file.text)
    }

    private fun assertReformatted(before: String, after: String) {
        val file = myFixture.configureByText(AspFileType, before)
        reformat(file)
        assertEquals(after, file.text)
        reformat(file)
        assertEquals("Formatting an ASP file twice should be stable", after, file.text)
    }

    private fun reformat(file: PsiFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(file)
        }
    }
}
