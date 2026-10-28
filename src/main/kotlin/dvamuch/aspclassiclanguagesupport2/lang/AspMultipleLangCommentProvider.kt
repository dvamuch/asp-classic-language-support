package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.codeInsight.generation.SelfManagingCommenter
import com.intellij.lang.Commenter
import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.MultipleLangCommentProvider

/** Selects comment syntax per physical line in mixed ASP/HTML documents. */
class AspMultipleLangCommentProvider : MultipleLangCommentProvider {
    override fun canProcess(file: PsiFile, viewProvider: FileViewProvider): Boolean =
        viewProvider.baseLanguage == AspLanguage

    override fun getLineCommenter(
        file: PsiFile,
        editor: Editor,
        lineStartLanguage: Language,
        lineEndLanguage: Language
    ): Commenter = AspLineCommenter
}

private object AspLineCommenter : Commenter, SelfManagingCommenter<AspCommenterState> {
    override fun getLineCommentPrefix(): String = "'"

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null

    override fun createLineCommentingState(
        startLine: Int,
        endLine: Int,
        document: Document,
        file: PsiFile
    ): AspCommenterState = AspCommenterState(analyzeDocument(document))

    override fun createBlockCommentingState(
        startLine: Int,
        endLine: Int,
        document: Document,
        file: PsiFile
    ): AspCommenterState = AspCommenterState(analyzeDocument(document))

    override fun commentLine(
        line: Int,
        offset: Int,
        document: Document,
        state: AspCommenterState
    ) {
        val plan = state.plans.getOrNull(line) ?: return
        if (isCommented(plan, line, document)) return

        val lineStart = document.getLineStartOffset(line)
        when (plan.kind) {
            AspLineKind.HTML -> {
                val contentStart = lineStart + plan.contentStartColumn
                val contentEnd = lineStart + plan.contentEndColumn
                val content = document.charsSequence.subSequence(contentStart, contentEnd).toString()
                document.replaceString(
                    contentStart,
                    contentEnd,
                    HTML_COMMENT_PREFIX + escapeHtmlCommentContent(content) + HTML_COMMENT_SUFFIX
                )
            }

            AspLineKind.SCRIPT -> {
                val insertionColumn = plan.scriptCodeColumn ?: 0
                val prefix = if (plan.scriptCodeColumn != null) "' " else "'"
                document.insertString(lineStart + insertionColumn, prefix)
            }

            AspLineKind.NEUTRAL,
            AspLineKind.PROTECTED -> Unit
        }
    }

    override fun uncommentLine(
        line: Int,
        offset: Int,
        document: Document,
        state: AspCommenterState
    ) {
        val plan = state.plans.getOrNull(line) ?: return
        val lineStart = document.getLineStartOffset(line)
        when (plan.kind) {
            AspLineKind.HTML -> {
                val prefixStart = lineStart + plan.contentStartColumn
                val contentEnd = lineStart + plan.contentEndColumn
                if (matches(document, prefixStart, HTML_COMMENT_PREFIX) &&
                    matches(document, contentEnd - HTML_COMMENT_SUFFIX.length, HTML_COMMENT_SUFFIX)) {
                    val escapedContent = document.charsSequence.subSequence(
                        prefixStart + HTML_COMMENT_PREFIX.length,
                        contentEnd - HTML_COMMENT_SUFFIX.length
                    ).toString()
                    document.replaceString(prefixStart, contentEnd, unescapeHtmlCommentContent(escapedContent))
                }
            }

            AspLineKind.SCRIPT -> {
                val commentColumn = plan.scriptCodeColumn ?: plan.firstNonWhitespaceColumn
                val commentStart = lineStart + commentColumn
                if (plan.scriptCodeColumn != null && matches(document, commentStart, "' ")) {
                    document.deleteString(commentStart, commentStart + 2)
                } else if (matches(document, commentStart, "'")) {
                    document.deleteString(commentStart, commentStart + 1)
                }
            }

            AspLineKind.NEUTRAL,
            AspLineKind.PROTECTED -> Unit
        }
    }

    override fun isLineCommented(
        line: Int,
        offset: Int,
        document: Document,
        state: AspCommenterState
    ): Boolean {
        val plan = state.plans.getOrNull(line) ?: return true
        return isCommented(plan, line, document)
    }

    override fun getCommentPrefix(
        line: Int,
        document: Document,
        state: AspCommenterState
    ): String? = when (state.plans.getOrNull(line)?.kind) {
        AspLineKind.HTML -> HTML_COMMENT_PREFIX
        AspLineKind.SCRIPT -> "'"
        else -> null
    }

    // ASP has no single safe block-comment syntax spanning both template HTML
    // and executable VBScript. Keep the separate block-comment action inert;
    // Command+/ is handled line by line above.
    override fun getBlockCommentRange(
        selectionStart: Int,
        selectionEnd: Int,
        document: Document,
        state: AspCommenterState
    ): TextRange? = null

    override fun getBlockCommentPrefix(
        selectionStart: Int,
        document: Document,
        state: AspCommenterState
    ): String? = null

    override fun getBlockCommentSuffix(
        selectionEnd: Int,
        document: Document,
        state: AspCommenterState
    ): String? = null

    override fun uncommentBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        state: AspCommenterState
    ) = Unit

    override fun insertBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        state: AspCommenterState
    ): TextRange? = null

    private fun isCommented(plan: AspLinePlan, line: Int, document: Document): Boolean {
        if (plan.kind == AspLineKind.NEUTRAL || plan.kind == AspLineKind.PROTECTED) return true

        val lineStart = document.getLineStartOffset(line)
        return when (plan.kind) {
            AspLineKind.HTML -> {
                !plan.activeHtmlDirective &&
                    matches(document, lineStart + plan.contentStartColumn, HTML_COMMENT_PREFIX) &&
                    matches(
                        document,
                        lineStart + plan.contentEndColumn - HTML_COMMENT_SUFFIX.length,
                        HTML_COMMENT_SUFFIX
                    )
            }

            AspLineKind.SCRIPT -> {
                val commentColumn = plan.scriptCodeColumn ?: plan.firstNonWhitespaceColumn
                matches(document, lineStart + commentColumn, "'")
            }

            AspLineKind.NEUTRAL,
            AspLineKind.PROTECTED -> true
        }
    }
}

private class AspCommenterState(val plans: List<AspLinePlan>) : CommenterDataHolder()

private enum class AspLineKind {
    HTML,
    SCRIPT,
    NEUTRAL,
    PROTECTED
}

private data class AspLinePlan(
    val kind: AspLineKind,
    val contentStartColumn: Int,
    val contentEndColumn: Int,
    val firstNonWhitespaceColumn: Int,
    val scriptCodeColumn: Int? = null,
    val activeHtmlDirective: Boolean = false
)

private enum class AspSegmentMode {
    HTML,
    STATEMENT,
    EXPRESSION,
    DIRECTIVE,
    SERVER_COMMENT
}

private fun analyzeDocument(document: Document): List<AspLinePlan> {
    val text = document.charsSequence
    var mode = AspSegmentMode.HTML
    return (0 until document.lineCount).map { line ->
        val lineStart = document.getLineStartOffset(line)
        val lineEnd = document.getLineEndOffset(line)
        val lineText = text.subSequence(lineStart, lineEnd).toString()
        val firstNonWhitespace = lineText.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) lineText.length else it }
        val contentEnd = lineText.indexOfLast { !it.isWhitespace() }.let { if (it < 0) firstNonWhitespace else it + 1 }

        if (firstNonWhitespace == lineText.length) {
            return@map AspLinePlan(AspLineKind.NEUTRAL, firstNonWhitespace, contentEnd, firstNonWhitespace)
        }

        val trimmed = lineText.substring(firstNonWhitespace, contentEnd)
        if (trimmed.startsWith("<%@") || trimmed.startsWith("<%--")) {
            mode = modeAfterProtectedLine(trimmed, mode)
            return@map AspLinePlan(AspLineKind.PROTECTED, firstNonWhitespace, contentEnd, firstNonWhitespace)
        }
        val activeHtmlDirective = HTML_DIRECTIVE.matches(trimmed)

        var index = 0
        var hasHtml = false
        var hasStatementCode = false
        var hasExpressionCode = false
        var statementOpenColumn: Int? = null
        var statementCodeColumn: Int? = null

        while (index < lineText.length) {
            when (mode) {
                AspSegmentMode.HTML -> {
                    val open = lineText.indexOf("<%", index)
                    val htmlEnd = if (open < 0) lineText.length else open
                    if (lineText.substring(index, htmlEnd).any { !it.isWhitespace() }) hasHtml = true
                    if (open < 0) {
                        index = lineText.length
                    } else {
                        mode = when {
                            lineText.startsWith("<%--", open) -> AspSegmentMode.SERVER_COMMENT
                            lineText.startsWith("<%@", open) -> AspSegmentMode.DIRECTIVE
                            lineText.startsWith("<%=", open) -> AspSegmentMode.EXPRESSION
                            else -> AspSegmentMode.STATEMENT
                        }
                        val delimiterLength = when (mode) {
                            AspSegmentMode.SERVER_COMMENT -> 4
                            AspSegmentMode.DIRECTIVE,
                            AspSegmentMode.EXPRESSION -> 3
                            AspSegmentMode.STATEMENT -> 2
                            AspSegmentMode.HTML -> error("unreachable")
                        }
                        if (mode == AspSegmentMode.STATEMENT) statementOpenColumn = open
                        index = open + delimiterLength
                    }
                }

                AspSegmentMode.STATEMENT,
                AspSegmentMode.EXPRESSION,
                AspSegmentMode.DIRECTIVE -> {
                    val close = lineText.indexOf("%>", index)
                    val codeEnd = if (close < 0) lineText.length else close
                    val codeStart = firstNonWhitespaceIn(lineText, index, codeEnd)
                    if (codeStart != null) {
                        when (mode) {
                            AspSegmentMode.STATEMENT -> {
                                hasStatementCode = true
                                if (statementOpenColumn != null && statementCodeColumn == null) {
                                    statementCodeColumn = codeStart
                                }
                            }
                            AspSegmentMode.EXPRESSION -> hasExpressionCode = true
                            AspSegmentMode.DIRECTIVE -> Unit
                            AspSegmentMode.HTML,
                            AspSegmentMode.SERVER_COMMENT -> error("unreachable")
                        }
                    }
                    if (close < 0) {
                        index = lineText.length
                    } else {
                        mode = AspSegmentMode.HTML
                        index = close + 2
                    }
                }

                AspSegmentMode.SERVER_COMMENT -> {
                    val close = lineText.indexOf("--%>", index)
                    if (close < 0) {
                        index = lineText.length
                    } else {
                        mode = AspSegmentMode.HTML
                        index = close + 4
                    }
                }
            }
        }

        val kind = when {
            hasHtml || hasExpressionCode -> AspLineKind.HTML
            hasStatementCode -> AspLineKind.SCRIPT
            else -> AspLineKind.NEUTRAL
        }
        AspLinePlan(
            kind = kind,
            contentStartColumn = firstNonWhitespace,
            contentEndColumn = contentEnd,
            firstNonWhitespaceColumn = firstNonWhitespace,
            scriptCodeColumn = statementCodeColumn,
            activeHtmlDirective = activeHtmlDirective
        )
    }
}

private fun firstNonWhitespaceIn(text: String, start: Int, end: Int): Int? {
    for (index in start until end) {
        if (!text[index].isWhitespace()) return index
    }
    return null
}

private fun modeAfterProtectedLine(trimmed: String, current: AspSegmentMode): AspSegmentMode = when {
    trimmed.startsWith("<%--") && !trimmed.contains("--%>") -> AspSegmentMode.SERVER_COMMENT
    trimmed.startsWith("<%@") && !trimmed.contains("%>") -> AspSegmentMode.DIRECTIVE
    else -> if (current == AspSegmentMode.HTML) AspSegmentMode.HTML else current
}

private fun matches(document: Document, offset: Int, value: String): Boolean {
    if (offset < 0 || offset + value.length > document.textLength) return false
    val chars = document.charsSequence
    return value.indices.all { chars[offset + it] == value[it] }
}

private fun escapeHtmlCommentContent(content: String): String = content
    .replace(HTML_COMMENT_PREFIX, ESCAPED_HTML_COMMENT_PREFIX)
    .replace(HTML_COMMENT_SUFFIX, ESCAPED_HTML_COMMENT_SUFFIX)
    .replace(DOUBLE_DASH, ESCAPED_DOUBLE_DASH)

private fun unescapeHtmlCommentContent(content: String): String = content
    .replace(ESCAPED_HTML_COMMENT_PREFIX, HTML_COMMENT_PREFIX)
    .replace(ESCAPED_HTML_COMMENT_SUFFIX, HTML_COMMENT_SUFFIX)
    .replace(ESCAPED_DOUBLE_DASH, DOUBLE_DASH)

private val HTML_DIRECTIVE = Regex("(?is)<!--\\s*#[a-z][a-z0-9_]*\\b.*-->")
private const val HTML_COMMENT_PREFIX = "<!--"
private const val HTML_COMMENT_SUFFIX = "-->"
private const val ESCAPED_HTML_COMMENT_PREFIX = "&lt;!&ndash;"
private const val ESCAPED_HTML_COMMENT_SUFFIX = "&ndash;&gt;"
private const val DOUBLE_DASH = "--"
private const val ESCAPED_DOUBLE_DASH = "&#45;&#45;"
