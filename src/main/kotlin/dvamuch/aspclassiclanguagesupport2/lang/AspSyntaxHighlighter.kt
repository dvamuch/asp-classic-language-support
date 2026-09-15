package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

class AspSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer(): Lexer = AspLexer()

    private val vbHighlighter = dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptSyntaxHighlighter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            AspTokenTypes.TEMPLATE_DATA -> emptyArray()
            VbTypes.ASP_OPEN, VbTypes.ASP_EXPR_OPEN, VbTypes.ASP_CLOSE -> SCRIPTLET_DELIMITER_KEYS
            else -> vbHighlighter.getTokenHighlights(tokenType)
        }
    }

    private companion object {
        val SCRIPTLET_DELIMITER_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("PHP_TAG", DefaultLanguageHighlighterColors.KEYWORD)
        )
    }
}

class AspSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(
        project: Project?,
        virtualFile: VirtualFile?
    ): SyntaxHighlighter = AspSyntaxHighlighter()
}
