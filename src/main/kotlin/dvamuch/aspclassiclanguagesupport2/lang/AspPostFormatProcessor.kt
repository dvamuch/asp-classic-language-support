package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class AspPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        val file = source.containingFile ?: return source
        if (file.language != AspLanguage || isProcessing.get()) return source
        formatVbScriptFragments(file)
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source.language != AspLanguage || isProcessing.get()) return rangeToReformat
        val oldLength = source.textLength
        formatVbScriptFragments(source)
        val delta = source.textLength - oldLength
        return TextRange(rangeToReformat.startOffset, (rangeToReformat.endOffset + delta).coerceAtMost(source.textLength))
    }

    override fun isWhitespaceOnly(): Boolean = true

    private fun formatVbScriptFragments(file: PsiFile) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        isProcessing.set(true)
        try {
            if (separateMultilineScriptlets(file, document.charsSequence) { offset, text ->
                    document.insertString(offset, text)
                }
            ) {
                documentManager.commitDocument(document)
                CodeStyleManager.getInstance(file.project).reformat(file)
                documentManager.commitDocument(document)
            }

            val fragments = SyntaxTraverser.psiTraverser(file)
                .filter(AspOuterPsiElement::class.java)
                .mapNotNull { host ->
                    val info = aspScriptletInfo(host) ?: return@mapNotNull null
                    Fragment(
                        contentRange = info.range.shiftRight(host.textRange.startOffset),
                        originalContent = info.range.substring(host.text),
                        expressionPrefix = info.prefix,
                        baseIndent = lineIndentBefore(document.charsSequence, host.textRange.startOffset)
                    )
                }
                .toList()
                .sortedBy { fragment -> fragment.contentRange.startOffset }
            if (fragments.isEmpty()) return

            val markedSource = buildMarkedSource(fragments)
            val temporaryFile = PsiFileFactory.getInstance(file.project).createFileFromText(
                "asp-format.vbs",
                VbScriptFileType,
                markedSource
            )
            CodeStyleManager.getInstance(file.project).reformat(temporaryFile)
            val formattedSource = temporaryFile.text

            val replacements = fragments.mapIndexed { index, fragment ->
                val formatted = extractFragment(formattedSource, index)
                    ?: return@mapIndexed fragment.originalContent
                prepareForHost(fragment, formatted)
            }
            fragments.indices.reversed().forEach { index ->
                val range = fragments[index].contentRange
                document.replaceString(range.startOffset, range.endOffset, replacements[index])
            }
            documentManager.commitDocument(document)
            alignClosingDelimiters(file)
            documentManager.commitDocument(document)
        } finally {
            isProcessing.remove()
        }
    }

    private fun separateMultilineScriptlets(
        file: PsiFile,
        text: CharSequence,
        insert: (Int, String) -> Unit
    ): Boolean {
        val insertions = buildList {
            SyntaxTraverser.psiTraverser(file)
                .filter(AspOuterPsiElement::class.java)
                .forEach { host ->
                    val info = aspScriptletInfo(host) ?: return@forEach
                    val content = info.range.substring(host.text)
                    if (content.none { char -> char == '\n' || char == '\r' }) return@forEach

                    val start = host.textRange.startOffset
                    val lineStart = findLineStart(text, start)
                    val before = text.subSequence(lineStart, start)
                    if (before.any { char -> char != ' ' && char != '\t' } &&
                        before.lastOrNull { char -> char != ' ' && char != '\t' } == '>'
                    ) {
                        add(start to "\n")
                    }

                    val end = host.textRange.endOffset
                    if (end < text.length && text[end] == '<') add(end to "\n")
                }
        }
        insertions.sortedByDescending { (offset, _) -> offset }
            .forEach { (offset, value) -> insert(offset, value) }
        return insertions.isNotEmpty()
    }

    private fun buildMarkedSource(fragments: List<Fragment>): String = buildString {
        fragments.forEachIndexed { index, fragment ->
            append(marker(index, "START"))
            append('\n')
            append(fragment.expressionPrefix.orEmpty())
            append(removeHostIndent(fragment.originalContent, fragment.baseIndent))
            append('\n')
            append(marker(index, "END"))
            append('\n')
        }
    }

    private fun removeHostIndent(content: String, baseIndent: String): String {
        if (baseIndent.isEmpty()) return content
        return content.split('\n').mapIndexed { index, line ->
            if (index == 0) line else line.removePrefix(baseIndent)
        }.joinToString("\n")
    }

    private fun extractFragment(source: String, index: Int): String? {
        val startMarker = marker(index, "START")
        val endMarker = marker(index, "END")
        val markerOffset = source.indexOf(startMarker)
        if (markerOffset < 0) return null
        val contentStart = source.indexOf('\n', markerOffset + startMarker.length)
        if (contentStart < 0) return null
        val endMarkerOffset = source.indexOf(endMarker, contentStart + 1)
        if (endMarkerOffset < 0) return null
        val contentEnd = source.lastIndexOf('\n', endMarkerOffset - 1)
        if (contentEnd < contentStart) return null

        return source.substring(contentStart + 1, contentEnd + 1)
            .removeSuffix("\n")
            .removeSuffix("\r")
    }

    private fun prepareForHost(fragment: Fragment, formattedContent: String): String {
        var content = formattedContent
        if (fragment.expressionPrefix != null) {
            val prefixOffset = content.indexOf(fragment.expressionPrefix, ignoreCase = true)
            if (prefixOffset >= 0) {
                content = content.removeRange(prefixOffset, prefixOffset + fragment.expressionPrefix.length)
            }
        }

        val originalHasLineBreak = fragment.originalContent.any { char -> char == '\n' || char == '\r' }
        if (!originalHasLineBreak) return " ${content.trim()} "

        val startsOnNextLine = fragment.originalContent.startsWith('\n') || fragment.originalContent.startsWith("\r\n")
        val endsOnOwnLine = fragment.originalContent.endsWith('\n') || fragment.originalContent.endsWith('\r')
        if (startsOnNextLine && !content.startsWith('\n') && !content.startsWith('\r')) content = "\n$content"
        if (endsOnOwnLine && !content.endsWith('\n') && !content.endsWith('\r')) content += "\n"
        return addBaseIndent(content, fragment.baseIndent)
    }

    private fun addBaseIndent(content: String, baseIndent: String): String {
        val lines = content.split('\n')
        return lines.mapIndexed { index, rawLine ->
            val line = rawLine.removeSuffix("\r")
            if (index == 0) {
                line
            } else if (line.isNotEmpty() || index == lines.lastIndex) {
                baseIndent + line
            } else {
                line
            }
        }.joinToString("\n")
    }

    private fun lineIndentBefore(text: CharSequence, offset: Int): String {
        val lineStart = findLineStart(text, offset)
        val beforeHost = text.subSequence(lineStart, offset)
        return if (beforeHost.all { char -> char == ' ' || char == '\t' }) beforeHost.toString() else ""
    }

    private fun findLineStart(text: CharSequence, offset: Int): Int {
        var lineStart = offset
        while (lineStart > 0 && text[lineStart - 1] != '\n' && text[lineStart - 1] != '\r') lineStart--
        return lineStart
    }

    private fun alignClosingDelimiters(file: PsiFile) {
        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return
        val hosts = SyntaxTraverser.psiTraverser(file)
            .filter(AspOuterPsiElement::class.java)
            .filter { host -> aspScriptletInfo(host) != null }
            .toList()
            .sortedByDescending { host -> host.textRange.startOffset }

        for (host in hosts) {
            val closeOffset = host.textRange.endOffset - 2
            if (closeOffset < 0 || closeOffset > document.textLength) continue
            val closeLine = document.getLineNumber(closeOffset)
            val closeLineStart = document.getLineStartOffset(closeLine)
            val beforeClose = document.charsSequence.subSequence(closeLineStart, closeOffset)
            if (beforeClose.any { char -> char != ' ' && char != '\t' }) continue

            val openOffset = host.textRange.startOffset
            val openLine = document.getLineNumber(openOffset)
            val openLineStart = document.getLineStartOffset(openLine)
            val beforeOpen = document.charsSequence.subSequence(openLineStart, openOffset)
            val openIndent = if (beforeOpen.all { char -> char == ' ' || char == '\t' }) beforeOpen.toString() else ""
            document.replaceString(closeLineStart, closeOffset, openIndent)
        }
    }

    private fun marker(index: Int, boundary: String): String = "'__ASP_FORMAT_${index}_${boundary}__"

    private data class Fragment(
        val contentRange: TextRange,
        val originalContent: String,
        val expressionPrefix: String?,
        val baseIndent: String
    )

    companion object {
        private val isProcessing = ThreadLocal.withInitial { false }
    }
}
