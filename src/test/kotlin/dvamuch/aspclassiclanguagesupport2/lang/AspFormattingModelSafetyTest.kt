package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.formatting.Block
import com.intellij.formatting.FormattingContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.application.options.CodeStyle
import com.intellij.psi.formatter.xml.HtmlCodeStyleSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptCodeStyleSettings
import java.nio.file.Files
import java.nio.file.Path

/** Diagnostic regressions: every meaningful character must belong to a leaf block. */
class AspFormattingModelSafetyTest : BasePlatformTestCase() {
    fun testWholeOperationGuardRestoresChangesMadeBeforePostProcessor() {
        val source = """
            <div>
                <% If ready Then %>
                <span><%= value %></span>
                <% End If %>
            </div>
        """.trimIndent()
        val file = myFixture.configureByText("guard.asp", source)
        val document = myFixture.editor.document
        val documentManager = PsiDocumentManager.getInstance(project)

        WriteCommandAction.runWriteCommandAction(project) {
            AspFormatOperationGuard.capture(file, TextRange(0, file.textLength))
            val destructiveStart = document.text.indexOf("value")
            document.deleteString(destructiveStart, destructiveStart + "value".length)
            documentManager.commitDocument(document)
            AspPostFormatProcessor().processText(
                file,
                TextRange(0, file.textLength),
                CodeStyle.getSettings(file)
            )
        }
        documentManager.commitAllDocuments()

        assertEquals("The whole-operation guard must restore pre-format content", source, file.text)
        myFixture.performEditorAction(IdeActions.ACTION_UNDO)
        documentManager.commitAllDocuments()
        assertEquals("Undo after a guarded reformat must keep the original content", source, file.text)
    }

    fun testCancelledOperationSnapshotCannotRestoreLaterCommand() {
        val source = "<div><%= value %></div>"
        val file = myFixture.configureByText("cancelled-guard.asp", source)
        val document = myFixture.editor.document
        val documentManager = PsiDocumentManager.getInstance(project)

        WriteCommandAction.runWriteCommandAction(project) {
            AspFormatOperationGuard.capture(file, TextRange(0, file.textLength))
            // Simulate cancellation before the post-processor consumes this snapshot.
        }
        WriteCommandAction.runWriteCommandAction(project) {
            val start = document.text.indexOf("value")
            document.replaceString(start, start + "value".length, "other")
            documentManager.commitDocument(document)
            AspPostFormatProcessor().processText(
                file,
                TextRange(0, 1),
                CodeStyle.getSettings(file)
            )
        }
        documentManager.commitAllDocuments()

        assertEquals("A stale guard must not overwrite a later command", "<div><%= other %></div>", file.text)
    }

    fun testGuardUsesCurrentUnsavedDocumentAsRollbackBaseline() {
        val file = myFixture.configureByText("unsaved-guard.asp", "<div><%= value %></div>")
        val document = myFixture.editor.document
        val documentManager = PsiDocumentManager.getInstance(project)

        WriteCommandAction.runWriteCommandAction(project) {
            val valueOffset = document.text.indexOf("value")
            document.replaceString(valueOffset, valueOffset + "value".length, "unsavedValue")
            // capture() must commit and snapshot the current editor text, not stale PSI.
            AspFormatOperationGuard.capture(file, TextRange(0, document.textLength))
            val destructiveOffset = document.text.indexOf("unsavedValue")
            document.deleteString(destructiveOffset, destructiveOffset + "unsavedValue".length)
            documentManager.commitDocument(document)
            AspPostFormatProcessor().processText(
                file,
                TextRange(0, file.textLength),
                CodeStyle.getSettings(file)
            )
        }
        documentManager.commitAllDocuments()

        assertEquals("<div><%= unsavedValue %></div>", file.text)
    }

    fun testPartialReformatAroundScriptletBoundaries() {
        val source = """
            <div>
            <%
            If ready Then
            value = "SELECT DISTINCT field FROM records WHERE field = 1"
            other = 2
            %>
            <script>const value = '<%= value %>'; if (value) { alert(value); }</script>
            <p><%= other %></p>
            <% End If %>
            </div>
        """.trimIndent()
        val offsets = listOf(0, source.indexOf("If ready"), source.indexOf("WHERE"), source.indexOf("other ="),
            source.indexOf("%>"), source.indexOf("<script>"), source.indexOf("<%= value"), source.length)
        val failures = mutableListOf<String>()
        for (start in offsets) for (end in offsets.filter { it > start }) {
            val file = myFixture.configureByText("selected.asp", source)
            myFixture.editor.selectionModel.setSelection(start, end)
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            if (source.filterNot(Char::isWhitespace) != file.text.filterNot(Char::isWhitespace)) {
                failures += "selection $start..$end changed code: ${file.text}"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    fun testReformatAfterIncrementalEditsAndUndo() {
        val source = buildString {
            appendLine("<div><%")
            appendLine("If ready Then")
            repeat(200) { appendLine("value$it = \"SELECT field FROM records WHERE id = $it\"") }
            appendLine("%>")
            repeat(40) { appendLine("<p><%= value$it %></p>") }
            appendLine("<% End If %></div>")
        }
        val file = myFixture.configureByText("edited.asp", source)
        val document = myFixture.editor.document
        val documentManager = PsiDocumentManager.getInstance(project)
        uncoveredCode(file) // Materialize both PSI trees before changing their offsets.
        repeat(4) { pass ->
            val offset = document.text.indexOf("WHERE")
            WriteCommandAction.runWriteCommandAction(project) { document.insertString(offset, "MORE ") }
            documentManager.commitAllDocuments()
            myFixture.doHighlighting()
            myFixture.performEditorAction(IdeActions.ACTION_UNDO)
            documentManager.commitAllDocuments()
            val before = document.text
            myFixture.editor.caretModel.moveToOffset(if (pass % 2 == 0) 0 else offset)
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
            documentManager.commitAllDocuments()
            assertEquals("Incremental edit/undo pass $pass", before.filterNot(Char::isWhitespace), file.text.filterNot(Char::isWhitespace))
            assertEquals(document.text, file.text)
            assertEquals(document.text, file.viewProvider.getPsi(com.intellij.lang.html.HTMLLanguage.INSTANCE)?.text)
        }
    }

    fun testMixedTemplateBlockCoverageAndReformat() {
        val cases = listOf(
            "<% If ready Then %><div><%= value %></div><% End If %>",
            "<script>const name = '<%= value %>'; if (name) { alert(name); }</script>",
            "<style>.item { color: <%= color %>; }</style>",
            "<input <% If ready Then %> disabled <% End If %> value=\"<%= value %>\">",
            "<% If ready Then %><div><% Else %><section><% End If %>Text</div>",
            "<div><% ' comment\nvalue = \"long string with spaces\"\n%><span>Text</span></div>",
            "<textarea><%= value %> plain text</textarea>",
            "<!-- comment <% value = 1 %> more comment -->",
            "<% Dim =\n%><p>Broken ASP must still be preserved</p>",
            "<div><%\nvalue = 1\n",
            "<%@ Language=VBScript %><% value=1 %>"
        )
        val failures = mutableListOf<String>()
        cases.forEachIndexed { index, source ->
            val file = myFixture.configureByText("case$index.asp", source)
            val gaps = uncoveredCode(file)
            if (gaps.isNotEmpty()) failures += "case $index uncovered: $gaps"
            WriteCommandAction.runWriteCommandAction(project) {
                CodeStyleManager.getInstance(project).reformat(file)
            }
            if (source.filterNot(Char::isWhitespace) != file.text.filterNot(Char::isWhitespace)) {
                failures += "case $index content changed: ${file.text}"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    fun testImportantTtsFilesWithRepeatedEditorReformat() {
        val root = System.getProperty("tts.project.dir") ?: return
        val failures = mutableListOf<String>()
        val filter = System.getProperty("tts.editor.path.filter").orEmpty()
        val paths = listOf("Bugs/index.asp", "Bugs/BugAdd.asp", "customers/orderinfo.asp").filter { it.contains(filter) }
        require(paths.isNotEmpty()) { "No editor fixtures matched $filter" }
        for (relativePath in paths) {
            val source = Files.readString(Path.of(root, relativePath)).replace("\r\n", "\n").removePrefix("\uFEFF")
            val file = myFixture.configureByText("safety.asp", source)
            // This diagnostic verifies content preservation and idempotence.
            // Keyword casing is an intentional transformation covered by the
            // formatter integration tests, so keep it out of this safety oracle.
            CodeStyle.getSettings(file)
                .getCustomSettings(VbScriptCodeStyleSettings::class.java)
                .KEYWORD_CASE = VbScriptCodeStyleSettings.KEYWORD_CASE_PRESERVE
            System.getProperty("tts.html.align.text")?.toBooleanStrictOrNull()?.let { alignText ->
                CodeStyle.getSettings(file)
                    .getCustomSettings(HtmlCodeStyleSettings::class.java)
                    .HTML_ALIGN_TEXT = alignText
            }
            val gaps = uncoveredCode(file)
            println("FORMAT MODEL $relativePath uncovered=$gaps")
            if (gaps.isNotEmpty()) failures += "$relativePath uncovered: $gaps"
            repeat(2) { pass ->
                val beforePass = file.text
                val started = System.nanoTime()
                println("EDITOR REFORMAT START $relativePath pass=${pass + 1}")
                System.out.flush()
                myFixture.editor.caretModel.moveToOffset(0)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
                PsiDocumentManager.getInstance(project).commitAllDocuments()
                val diagnosticsPath = System.getProperty("tts.format.diagnostics.dir")
                if (diagnosticsPath != null && relativePath == "customers/orderinfo.asp" && pass == 0) {
                    val diagnostics = Path.of(diagnosticsPath)
                    Files.createDirectories(diagnostics)
                    Files.writeString(diagnostics.resolve("formatted-current.txt"), file.text)
                }
                if (source.filterNot(Char::isWhitespace) != file.text.filterNot(Char::isWhitespace)) {
                    failures += "$relativePath pass $pass changed content"
                }
                if (pass == 1 && beforePass != file.text) {
                    val artifacts = Path.of("build/reports/tts-format-idempotence", relativePath)
                    Files.createDirectories(artifacts)
                    Files.writeString(artifacts.resolve("once.txt"), beforePass)
                    Files.writeString(artifacts.resolve("twice.txt"), file.text)
                    failures += "$relativePath second reformat was not idempotent; artifacts=$artifacts"
                }
                val elapsedMillis = (System.nanoTime() - started) / 1_000_000
                println("EDITOR REFORMAT END $relativePath pass=${pass + 1} ${elapsedMillis}ms")
                if (relativePath == "customers/orderinfo.asp") {
                    val limitMillis = if (pass == 0) 6_000 else 4_000
                    assertTrue(
                        "$relativePath pass ${pass + 1} took ${elapsedMillis}ms; limit=${limitMillis}ms",
                        elapsedMillis < limitMillis
                    )
                }
                System.out.flush()
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun uncoveredCode(file: com.intellij.psi.PsiFile): List<String> {
        val source = file.text
        val settings = CodeStyle.getSettings(file)
        val model = AspFormattingModelBuilder().createModel(FormattingContext.create(file, settings))
        val covered = BooleanArray(source.length)
        fun visit(block: Block) {
            val children = block.subBlocks
            if (children.isEmpty()) {
                val range = block.textRange
                for (offset in range.startOffset until range.endOffset.coerceAtMost(source.length)) {
                    covered[offset] = true
                }
            } else {
                children.forEach(::visit)
            }
        }
        visit(model.rootBlock)
        return source.indices.asSequence()
            .filter { !covered[it] && !source[it].isWhitespace() }
            .take(10)
            .map { "offset=$it ${source.substring(it, (it + 60).coerceAtMost(source.length)).replace("\n", "\\n")}" }
            .toList()
    }

}
