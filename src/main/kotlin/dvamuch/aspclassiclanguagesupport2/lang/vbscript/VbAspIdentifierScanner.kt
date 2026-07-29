package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.AspOuterPsiElement
import dvamuch.aspclassiclanguagesupport2.lang.aspScriptletInfo
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

/** Finds VB identifiers without building the injected parser tree. */
internal object VbAspIdentifierScanner {
    data class Result(
        val ranges: List<TextRange>,
        val requiresPsiResolution: Boolean
    )

    private data class Token(
        val type: IElementType,
        val text: String,
        val range: TextRange,
        val hostIndex: Int
    )

    fun scan(aspFile: PsiFile, name: String): Result {
        val tokens = mutableListOf<Token>()
        val hosts = PsiTreeUtil.collectElementsOfType(aspFile, AspOuterPsiElement::class.java)
            .sortedBy { it.textOffset }
        hosts.forEachIndexed { hostIndex, host ->
            val info = aspScriptletInfo(host) ?: return@forEachIndexed
            val text = info.range.substring(host.text)
            val lexer = VbScriptLexerAdapter()
            lexer.start(text)
            while (lexer.tokenType != null) {
                val type = lexer.tokenType!!
                if (type != TokenType.WHITE_SPACE && type != VbTypes.COMMENT) {
                    val start = host.textRange.startOffset + info.range.startOffset + lexer.tokenStart
                    tokens += Token(
                        type,
                        lexer.tokenText,
                        TextRange(start, start + lexer.tokenText.length),
                        hostIndex
                    )
                }
                lexer.advance()
            }
        }

        val matches = tokens.indices.filter { index ->
            tokens[index].type == VbTypes.IDENTIFIER && tokens[index].text.equals(name, ignoreCase = true)
        }
        if (matches.any { isPotentialLocalDeclaration(tokens, it) }) {
            return Result(emptyList(), requiresPsiResolution = true)
        }

        return Result(
            matches
                .filterNot { index -> tokens.getOrNull(index - 1)?.type == VbTypes.DOT }
                .map { tokens[it].range },
            requiresPsiResolution = false
        )
    }

    private fun isPotentialLocalDeclaration(tokens: List<Token>, index: Int): Boolean {
        val token = tokens[index]
        val statementStart = (index - 1 downTo 0).firstOrNull { candidate ->
            val previous = tokens[candidate]
            previous.hostIndex != token.hostIndex ||
                previous.type == VbTypes.EOL || previous.type == VbTypes.COLON
        }?.plus(1) ?: 0
        val prefix = tokens.subList(statementStart, index).map { it.type }

        if (prefix.any {
                it == VbTypes.DIM || it == VbTypes.CONST || it == VbTypes.FUNCTION ||
                    it == VbTypes.SUB || it == VbTypes.PROPERTY || it == VbTypes.CLASS ||
                    it == VbTypes.FOR
            }
        ) {
            return true
        }

        val first = tokens.getOrNull(statementStart)?.type
        val previous = tokens.getOrNull(index - 1)?.type
        val startsAssignment = statementStart == index ||
            ((first == VbTypes.SET || first == VbTypes.LET) && statementStart + 1 == index) ||
            previous == VbTypes.THEN || previous == VbTypes.ELSE ||
            ((previous == VbTypes.SET || previous == VbTypes.LET) &&
                tokens.getOrNull(index - 2)?.type in setOf(VbTypes.THEN, VbTypes.ELSE, VbTypes.COLON))
        return startsAssignment && tokens.getOrNull(index + 1)?.type == VbTypes.EQ
    }
}
