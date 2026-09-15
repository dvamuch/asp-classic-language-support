package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor

class VbScriptIndentPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        val file = source.containingFile ?: return source
        if (file.language != VbScriptLanguage || isProcessing.get()) return source
        runGuarded { VbScriptIndentNormalizer.normalize(file, settings) }
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source.language != VbScriptLanguage || isProcessing.get()) return rangeToReformat
        val oldLength = source.textLength
        runGuarded { VbScriptIndentNormalizer.normalize(source, settings) }
        val delta = source.textLength - oldLength
        return TextRange(rangeToReformat.startOffset, (rangeToReformat.endOffset + delta).coerceAtMost(source.textLength))
    }

    override fun isWhitespaceOnly(): Boolean = false

    private fun runGuarded(action: () -> Unit) {
        isProcessing.set(true)
        try {
            action()
        } finally {
            isProcessing.remove()
        }
    }

    companion object {
        private val isProcessing = ThreadLocal.withInitial { false }
    }
}

internal object VbScriptIndentNormalizer {
    fun normalize(file: PsiFile, settings: CodeStyleSettings) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        val indentSize = settings.getCommonSettings(VbScriptLanguage).indentOptions?.INDENT_SIZE ?: 4
        val indented = normalizeText(document.text, indentSize)
        val normalized = VbScriptKeywordCaseSupport.normalizeText(
            indented,
            VbScriptKeywordCaseSupport.mode(settings)
        )
        if (normalized == document.text) return
        document.replaceString(0, document.textLength, normalized)
        documentManager.commitDocument(document)
    }

    fun normalizeText(text: String, indentSize: Int): String {
        val tracker = VbScriptControlFlowTracker()
        var previousLineContinues = false
        var offset = 0
        return buildString(text.length) {
            while (offset < text.length) {
                val lineEnd = text.indexOfAny(charArrayOf('\r', '\n'), offset).let { found ->
                    if (found < 0) text.length else found
                }
                val line = text.substring(offset, lineEnd)
                val code = line.trimStart(' ', '\t')
                if (code.isEmpty() || previousLineContinues) {
                    append(line)
                } else {
                    val level = tracker.consume(code).indentLevel
                    append(" ".repeat(level * indentSize))
                    append(code)
                }
                previousLineContinues = hasLineContinuation(line)

                if (lineEnd < text.length) {
                    if (text[lineEnd] == '\r' && lineEnd + 1 < text.length && text[lineEnd + 1] == '\n') {
                        append("\r\n")
                        offset = lineEnd + 2
                    } else {
                        append(text[lineEnd])
                        offset = lineEnd + 1
                    }
                } else {
                    offset = lineEnd
                }
            }
        }
    }

    private fun hasLineContinuation(line: String): Boolean {
        if (!line.trimEnd().endsWith('_')) return false
        // An underscore in a comment (including our ASP fragment markers) is
        // not a continuation and must not suppress tracking of the next If/End If.
        val source = "$line\n"
        val lexer = VbScriptLexerAdapter()
        lexer.start(source)
        while (lexer.tokenType != null) {
            if (lexer.tokenType == com.intellij.psi.TokenType.WHITE_SPACE &&
                VbScriptFormattingSupport.isLineContinuation(source.subSequence(lexer.tokenStart, lexer.tokenEnd))
            ) return true
            lexer.advance()
        }
        return false
    }

}
