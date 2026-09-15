package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

class VbScriptSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer(): Lexer = VbScriptLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when {
            VbScriptKeywordCaseSupport.tokenSet.contains(tokenType) -> KEYWORD_KEYS
            OPERATORS.contains(tokenType) -> OPERATOR_KEYS
            COMMENTS.contains(tokenType) -> COMMENT_KEYS
            LITERALS.contains(tokenType) -> LITERAL_KEYS
            STRINGS.contains(tokenType) -> STRING_KEYS
            NUMBERS.contains(tokenType) -> NUMBER_KEYS
            IDENTIFIERS.contains(tokenType) -> VARIABLE_KEYS
            else -> emptyArray()
        }
    }

    companion object {
        private val OPERATORS = TokenSet.create(
            VbTypes.PLUS, VbTypes.MINUS, VbTypes.STAR, VbTypes.SLASH, VbTypes.IDIV, VbTypes.POW,
            VbTypes.AMP, VbTypes.EQ, VbTypes.NEQ, VbTypes.LT, VbTypes.GT, VbTypes.LE, VbTypes.GE
        )

        private val STRINGS = TokenSet.create(VbTypes.STRING)
        private val COMMENTS = TokenSet.create(VbTypes.COMMENT)
        private val IDENTIFIERS = TokenSet.create(VbTypes.IDENTIFIER)

        private val NUMBERS = TokenSet.create(
            VbTypes.NUMBER, VbTypes.FLOAT, VbTypes.HEX_NUMBER, VbTypes.OCT_NUMBER
        )

        private val LITERALS = TokenSet.create(
            VbTypes.TRUE, VbTypes.FALSE, VbTypes.NULL, VbTypes.EMPTY, VbTypes.NOTHING, VbTypes.DATE
        )

        private val KEYWORD_KEYS = arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
        private val OPERATOR_KEYS = arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)
        private val STRING_KEYS = arrayOf(DefaultLanguageHighlighterColors.STRING)
        private val NUMBER_KEYS = arrayOf(DefaultLanguageHighlighterColors.NUMBER)
        private val LITERAL_KEYS = arrayOf(DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
        private val COMMENT_KEYS = arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
        private val VARIABLE_KEYS = arrayOf(
            TextAttributesKey.find("PHP_VARIABLE")
                ?: TextAttributesKey.find("PHP_LOCAL_VARIABLE")
                ?: TextAttributesKey.find("PHP_GLOBAL_VARIABLE")
                ?: DefaultLanguageHighlighterColors.LOCAL_VARIABLE
        )
    }
}

class VbScriptSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(
        project: Project?,
        virtualFile: VirtualFile?
    ): SyntaxHighlighter = VbScriptSyntaxHighlighter()
}
