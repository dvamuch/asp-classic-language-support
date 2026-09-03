package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AspFormatterIntegrationTest : BasePlatformTestCase() {
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

    fun testExpandsInlineIfAroundHtmlAndKeepsFourSpaceNesting() {
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
            <%
            if isAuthor = 0 then
                %>
                <script type="text/javascript">
                    alert('Message' + '\n' +
                        'Second line')
                </script>

                <%
            end if
            %>
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

    fun testExpandsInlineElseBranchAroundHtml() {
        assertReformatted(
            """
            <% If ready Then %>
            <p>Ready</p>
            <% Else %>
            <p>Not ready</p>
            <% End If %>
            """.trimIndent(),
            """
            <%
            If ready Then
                %>
                <p>Ready</p>
                <%
            Else
                %>
                <p>Not ready</p>
                <%
            End If
            %>
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
            class DeviceDal
                public function getById(deviceId)
                    set cmd = Server.CreateObject("ADODB.Command")
                    set getById = cmd.Execute()
                end function
            end class
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
