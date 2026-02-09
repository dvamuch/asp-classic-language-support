package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.util.LayerDescriptor
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AspEditorHighlighter(
    project: Project?,
    virtualFile: VirtualFile?,
    colors: EditorColorsScheme
) : LayeredLexerEditorHighlighter(AspSyntaxHighlighter(), colors) {
    init {
        val htmlHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(
            HTMLLanguage.INSTANCE,
            project,
            virtualFile
        )
        registerLayer(
            AspTokenTypes.TEMPLATE_DATA,
            LayerDescriptor(htmlHighlighter, "")
        )
    }
}
