package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Exercises the real editor Reformat Code action against a clean Git revision. */
class AspEditorReformatSafetyTest : BasePlatformTestCase() {
    fun testBugsIndexWithViewerBlockOutsideViewport() {
        assertEditorReformatPreservesHealthyBugsIndex(targetVisible = false)
    }

    fun testBugsIndexWithViewerBlockInsideViewport() {
        assertEditorReformatPreservesHealthyBugsIndex(targetVisible = true)
    }

    fun testBugsIndexAfterUndoWithViewerBlockOutsideViewport() {
        val ttsRoot = System.getProperty(TTS_PROJECT_DIR_PROPERTY) ?: return
        val repository = Path.of(ttsRoot)
        val healthySource = readGitHead(repository, "Bugs/index.asp")
        val viewerSourceStart = healthySource.indexOf("rsViewers.Source =")
        val viewerOpen = healthySource.indexOf("rsViewers.Open()", viewerSourceStart)
        assertTrue("Expected rsViewers source and open statements", viewerSourceStart >= 0 && viewerOpen > viewerSourceStart)
        val corruptedSource = healthySource.replaceRange(
            viewerSourceStart,
            viewerOpen,
            "rsViewers.Source = \"BROKEN\"\n"
        )

        val file = myFixture.configureByText("index.asp", healthySource)
        myFixture.editor.caretModel.moveToOffset(0)
        myFixture.editor.scrollingModel.scrollVertically(0)
        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.replaceString(0, myFixture.editor.document.textLength, corruptedSource)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        myFixture.performEditorAction(IdeActions.ACTION_UNDO)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        assertEquals("Undo should restore the healthy source", healthySource, file.text)

        val before = healthySource.filterNot(Char::isWhitespace)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val after = file.text.filterNot(Char::isWhitespace)
        assertEquals("Reformat after Undo changed non-whitespace characters", before, after)
    }

    private fun assertEditorReformatPreservesHealthyBugsIndex(targetVisible: Boolean) {
        val ttsRoot = System.getProperty(TTS_PROJECT_DIR_PROPERTY) ?: return
        val source = readGitHead(Path.of(ttsRoot), "Bugs/index.asp")
        val file = myFixture.configureByText("index.asp", source)
        val targetOffset = source.indexOf("Set rsViewers = Server.CreateObject")
        assertTrue("Expected rsViewers block in healthy Bugs/index.asp", targetOffset >= 0)

        if (targetVisible) {
            myFixture.editor.caretModel.moveToOffset(targetOffset)
            myFixture.editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        } else {
            myFixture.editor.caretModel.moveToOffset(0)
            myFixture.editor.scrollingModel.scrollVertically(0)
        }

        val before = source.filterNot(Char::isWhitespace)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val after = file.text.filterNot(Char::isWhitespace)

        assertFalse("Editor Reformat Code should change whitespace in the healthy fixture", source == file.text)

        assertEquals(
            "Editor Reformat Code changed non-whitespace characters when targetVisible=$targetVisible",
            before,
            after
        )
    }

    private fun readGitHead(repository: Path, relativePath: String): String {
        val process = ProcessBuilder("git", "-C", repository.toString(), "show", "HEAD:$relativePath")
            .redirectErrorStream(true)
            .start()
        val bytes = process.inputStream.readAllBytes()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "git show failed ($exitCode): ${String(bytes, StandardCharsets.UTF_8)}" }
        return String(bytes, StandardCharsets.UTF_8)
    }

    companion object {
        private const val TTS_PROJECT_DIR_PROPERTY = "tts.project.dir"
    }
}
