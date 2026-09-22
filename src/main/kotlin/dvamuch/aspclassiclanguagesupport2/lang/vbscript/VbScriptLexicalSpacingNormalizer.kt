package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

/**
 * Applies the unambiguous part of VBScript spacing directly to lexer tokens.
 *
 * The regular formatting model remains the primary formatter. This fallback is
 * needed for legacy ASP pages where fragments from the whole page form an
 * incomplete temporary VBScript file and the parser cannot build expression
 * nodes around every operator.
 */
internal object VbScriptLexicalSpacingNormalizer {
    fun normalizeText(text: String): String {
        val tokens = lex(text)
        if (tokens.size < 2) return text

        val replacements = mutableListOf<Replacement>()
        for (index in 0 until tokens.lastIndex) {
            val left = tokens[index]
            val right = tokens[index + 1]
            val gap = text.substring(left.end, right.start)
            if (gap.any { it == '\r' || it == '\n' } || gap.any { !it.isWhitespace() }) continue

            val spaces = spacesBetween(tokens, index) ?: continue
            val replacement = " ".repeat(spaces)
            if (gap != replacement) replacements += Replacement(left.end, right.start, replacement)
        }
        if (replacements.isEmpty()) return text

        return buildString(text.length) {
            var offset = 0
            replacements.forEach { replacement ->
                append(text, offset, replacement.start)
                append(replacement.text)
                offset = replacement.end
            }
            append(text, offset, text.length)
        }
    }

    private fun spacesBetween(tokens: List<Token>, leftIndex: Int): Int? {
        val left = tokens[leftIndex]
        val right = tokens[leftIndex + 1]
        if (left.type in IGNORED_BOUNDARY_TOKENS || right.type in IGNORED_BOUNDARY_TOKENS) return null

        if (left.type == VbTypes.DOT || right.type == VbTypes.DOT) return 0
        if (right.type == VbTypes.COMMA) return 0
        if (left.type == VbTypes.COMMA) return 1
        if (left.type == VbTypes.LPAREN || right.type == VbTypes.RPAREN) return 0
        if (left.type == VbTypes.NOT) return 1

        if (isBinaryOperator(tokens, leftIndex)) return 1
        if (isBinaryOperator(tokens, leftIndex + 1)) return 1
        return null
    }

    private fun isBinaryOperator(tokens: List<Token>, index: Int): Boolean {
        val type = tokens[index].type
        if (type in ALWAYS_BINARY_OPERATORS) return true
        if (type != VbTypes.PLUS && type != VbTypes.MINUS) return false
        val previous = tokens.getOrNull(index - 1)?.type ?: return false
        return previous in EXPRESSION_END_TOKENS
    }

    private fun lex(text: String): List<Token> {
        val lexer = VbScriptLexerAdapter()
        lexer.start(text)
        return buildList {
            while (lexer.tokenType != null) {
                val type = lexer.tokenType!!
                if (type != TokenType.WHITE_SPACE && type != VbTypes.EOL) {
                    add(Token(type, lexer.tokenStart, lexer.tokenEnd))
                }
                lexer.advance()
            }
        }
    }

    private data class Token(val type: IElementType, val start: Int, val end: Int)
    private data class Replacement(val start: Int, val end: Int, val text: String)

    private val IGNORED_BOUNDARY_TOKENS = setOf(VbTypes.COMMENT, TokenType.BAD_CHARACTER)
    private val ALWAYS_BINARY_OPERATORS = setOf(
        VbTypes.EQ,
        VbTypes.NEQ,
        VbTypes.LT,
        VbTypes.GT,
        VbTypes.LE,
        VbTypes.GE,
        VbTypes.IS,
        VbTypes.AMP,
        VbTypes.STAR,
        VbTypes.SLASH,
        VbTypes.IDIV,
        VbTypes.POW,
        VbTypes.AND,
        VbTypes.OR,
        VbTypes.XOR,
        VbTypes.EQV,
        VbTypes.IMP,
        VbTypes.MOD
    )
    private val EXPRESSION_END_TOKENS = setOf(
        VbTypes.IDENTIFIER,
        VbTypes.NUMBER,
        VbTypes.FLOAT,
        VbTypes.HEX_NUMBER,
        VbTypes.OCT_NUMBER,
        VbTypes.STRING,
        VbTypes.DATE,
        VbTypes.TRUE,
        VbTypes.FALSE,
        VbTypes.NULL,
        VbTypes.EMPTY,
        VbTypes.NOTHING,
        VbTypes.RPAREN
    )
}
