package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import dvamuch.aspclassiclanguagesupport2.lang.FormattingRangeSupport

class VbScriptIndentPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        val file = source.containingFile ?: return source
        if (file.language != VbScriptLanguage || isProcessing.get()) return source
        runGuarded { VbScriptIndentNormalizer.normalize(file, settings, null) }
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source.language != VbScriptLanguage || isProcessing.get()) return rangeToReformat
        return runGuarded {
            VbScriptIndentNormalizer.normalize(source, settings, rangeToReformat) ?: rangeToReformat
        }
    }

    override fun isWhitespaceOnly(): Boolean = false

    private fun <T> runGuarded(action: () -> T): T {
        isProcessing.set(true)
        try {
            return action()
        } finally {
            isProcessing.remove()
        }
    }

    companion object {
        private val isProcessing = ThreadLocal.withInitial { false }
    }
}

internal object VbScriptIndentNormalizer {
    fun normalize(file: PsiFile, settings: CodeStyleSettings, range: TextRange? = null): TextRange? {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return range
        val original = document.text
        val selectedRange = range?.let { FormattingRangeSupport.expandToWholeLines(original, it) }
        val indentOptions = settings.getCommonSettings(VbScriptLanguage).indentOptions
        val indentSize = indentOptions?.INDENT_SIZE ?: 4
        val continuationIndentSize = indentOptions?.CONTINUATION_INDENT_SIZE ?: 8
        val spaced = VbScriptLexicalSpacingNormalizer.normalizeText(original)
        val indented = normalizeText(spaced, indentSize, continuationIndentSize)
        val normalized = VbScriptKeywordCaseSupport.normalizeText(
            indented,
            VbScriptKeywordCaseSupport.mode(settings)
        )
        val replacement = if (selectedRange == null) {
            normalized
        } else {
            FormattingRangeSupport.mergeSelectedLines(original, normalized, 0, selectedRange) ?: original
        }
        if (replacement == original) return selectedRange ?: TextRange(0, original.length)
        document.replaceString(0, document.textLength, replacement)
        documentManager.commitDocument(document)
        return selectedRange?.let {
            TextRange(
                it.startOffset,
                (it.endOffset + replacement.length - original.length).coerceAtMost(replacement.length)
            )
        } ?: TextRange(0, replacement.length)
    }

    fun normalizeText(text: String, indentSize: Int, continuationIndentSize: Int = indentSize): String {
        val tracker = VbScriptControlFlowTracker()
        var previousLineContinues = false
        var continuationBaseIndent = 0
        var offset = 0
        return buildString(text.length) {
            while (offset < text.length) {
                val lineEnd = text.indexOfAny(charArrayOf('\r', '\n'), offset).let { found ->
                    if (found < 0) text.length else found
                }
                val line = text.substring(offset, lineEnd)
                val code = line.trimStart(' ', '\t')
                val currentLineContinues = isVbScriptLineContinuation(line)
                val flowResult = code.takeIf(String::isNotEmpty)?.let(tracker::consume)
                if (code.isEmpty()) {
                    append(line)
                } else if (previousLineContinues) {
                    val continuationIndent = if (startsWithClosingDelimiter(code)) {
                        continuationBaseIndent
                    } else {
                        continuationBaseIndent + continuationIndentSize
                    }
                    append(" ".repeat(continuationIndent))
                    append(code)
                } else {
                    val level = flowResult!!.indentLevel
                    continuationBaseIndent = level * indentSize
                    append(" ".repeat(continuationBaseIndent))
                    append(code)
                }
                previousLineContinues = currentLineContinues

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

    private fun startsWithClosingDelimiter(code: String): Boolean =
        code.startsWith(")") || code.startsWith("]")
}
