package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AspFormatterIntegrationTest : BasePlatformTestCase() {
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
