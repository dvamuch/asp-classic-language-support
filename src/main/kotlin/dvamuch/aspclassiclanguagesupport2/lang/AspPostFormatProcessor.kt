package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptControlFlowTracker
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptIndentNormalizer
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLexerAdapter
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

class AspPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        if (source !is PsiFile) return source
        val file = source.containingFile ?: return source
        if (file.language != AspLanguage || isProcessing.get()) return source
        formatVbScriptFragments(file, settings)
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source.language != AspLanguage || isProcessing.get()) return rangeToReformat
        if (rangeToReformat.startOffset != 0 || rangeToReformat.endOffset < source.textLength) {
            return rangeToReformat
        }
        val oldLength = source.textLength
        formatVbScriptFragments(source, settings)
        val delta = source.textLength - oldLength
        return TextRange(rangeToReformat.startOffset, (rangeToReformat.endOffset + delta).coerceAtMost(source.textLength))
    }

    override fun isWhitespaceOnly(): Boolean = true

    internal fun prepareCodeSpacing(file: PsiFile, settings: CodeStyleSettings) {
        if (!isProcessing.get()) formatVbScriptFragments(file, settings, formatSpacing = true)
    }

    private fun formatVbScriptFragments(file: PsiFile, settings: CodeStyleSettings, formatSpacing: Boolean = false) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        val originalDocumentText = document.text
        isProcessing.set(true)
        try {
            val indentSize = settings.getCommonSettings(
                dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage
            ).indentOptions?.INDENT_SIZE ?: 4
            val initialSnapshot = document.text
            val insertions = multilineScriptletInsertions(initialSnapshot)
            if (insertions.isNotEmpty()) {
                val insertedLineOffsets = insertions.map { (offset, text) ->
                    offset + text.length + insertions
                        .filter { (otherOffset, _) -> otherOffset < offset }
                        .sumOf { (_, otherText) -> otherText.length }
                }
                insertions.sortedByDescending { (offset, _) -> offset }
                    .forEach { (offset, text) -> document.insertString(offset, text) }
                documentManager.commitDocument(document)
                if (!formatSpacing) adjustInsertedLineIndents(file, document, insertedLineOffsets, indentSize)
                documentManager.commitDocument(document)
            }

            val fragmentSnapshot = document.text
            val fragments = collectFragments(fragmentSnapshot)
            if (fragments.isEmpty()) return
            val controlProfiles = analyzeControlFlow(fragments)

            val markedSource = buildMarkedSource(fragments)
            // Spacing must settle BEFORE the HTML formatter calculates line wraps.
            // Afterwards only recompute indentation against the final HTML layout.
            val spacedSource = if (formatSpacing) {
                val temporaryFile = PsiFileFactory.getInstance(file.project).createFileFromText(
                    "asp-format.vbs", VbScriptFileType, markedSource
                )
                CodeStyleManager.getInstance(file.project).reformat(temporaryFile)
                temporaryFile.text
            } else {
                markedSource
            }
            val formattedSource = VbScriptIndentNormalizer.normalizeText(spacedSource, indentSize)

            val replacements = fragments.mapIndexed { index, fragment ->
                val formatted = extractFragment(formattedSource, index)
                    ?: return@mapIndexed fragment.originalContent
                prepareForHost(fragment, formatted, controlProfiles[index], indentSize)
            }
            if (!replacementsAreSafe(fragmentSnapshot, fragments, replacements)) {
                LOG.warn("ASP formatting cancelled because scriptlet ranges or tokens changed")
                restoreDocument(documentManager, document, originalDocumentText)
                return
            }
            fragments.indices.reversed().forEach { index ->
                val range = fragments[index].contentRange
                document.replaceString(range.startOffset, range.endOffset, replacements[index])
            }
            documentManager.commitDocument(document)
            if (!formatSpacing) applySemanticLayout(document, controlProfiles, indentSize)
            documentManager.commitDocument(document)
            if (nonWhitespaceSkeleton(document.text) != nonWhitespaceSkeleton(originalDocumentText)) {
                LOG.warn("ASP formatting changed non-whitespace document content; rolling back scriptlet pass")
                restoreDocument(documentManager, document, originalDocumentText)
            }
        } catch (error: Throwable) {
            restoreDocument(documentManager, document, originalDocumentText)
            throw error
        } finally {
            isProcessing.remove()
        }
    }

    private fun multilineScriptletInsertions(text: String): List<Pair<Int, String>> {
        return buildList {
            collectFragments(text).forEach { fragment ->
                if (fragment.originalContent.none { char -> char == '\n' || char == '\r' }) return@forEach

                val start = fragment.outerRange.startOffset
                val lineStart = findLineStart(text, start)
                val before = text.subSequence(lineStart, start)
                if (before.any { char -> char != ' ' && char != '\t' } &&
                    before.lastOrNull { char -> char != ' ' && char != '\t' } == '>'
                ) {
                    add(start to "\n")
                }

                val end = fragment.outerRange.endOffset
                if (end < text.length && text[end] == '<') add(end to "\n")
            }
        }.distinct()
    }

    private fun adjustInsertedLineIndents(
        file: PsiFile,
        document: com.intellij.openapi.editor.Document,
        insertedLineOffsets: List<Int>,
        indentSize: Int
    ) {
        val snapshot = document.text
        val fragmentsBeforeAdjustment = collectFragments(snapshot)
        val insertedHostIndexes = fragmentsBeforeAdjustment.mapIndexedNotNull { index, fragment ->
            index.takeIf { fragment.outerRange.startOffset in insertedLineOffsets }
        }.toSet()
        val codeStyleManager = CodeStyleManager.getInstance(file.project)
        insertedLineOffsets.distinct().sortedDescending().forEach { offset ->
            codeStyleManager.adjustLineIndent(file, offset)
        }

        val adjustedSnapshot = document.text
        val replacements = collectFragments(adjustedSnapshot).mapIndexedNotNull { index, fragment ->
            if (index !in insertedHostIndexes) return@mapIndexedNotNull null
            val lineStart = findLineStart(adjustedSnapshot, fragment.outerRange.startOffset)
            val currentIndent = adjustedSnapshot.substring(lineStart, fragment.outerRange.startOffset)
            if (currentIndent.any { char -> char != ' ' && char != '\t' }) return@mapIndexedNotNull null

            val nextLine = nextNonBlankLine(adjustedSnapshot, fragment.outerRange.endOffset)
                ?: return@mapIndexedNotNull null
            val nextIndentEnd = nextLine.indexOfFirst { char -> char != ' ' && char != '\t' }
            val nextIndent = nextLine.substring(0, nextIndentEnd)
            val desiredIndent = if (nextLine.substring(nextIndentEnd).startsWith("</")) {
                nextIndent + " ".repeat(indentSize)
            } else {
                nextIndent
            }
            TextRange(lineStart, fragment.outerRange.startOffset) to desiredIndent
        }
        replacements.sortedByDescending { (range, _) -> range.startOffset }
            .forEach { (range, replacement) ->
                document.replaceString(range.startOffset, range.endOffset, replacement)
            }
    }

    private fun nextNonBlankLine(text: String, offset: Int): String? {
        var lineStart = nextLineStart(text, offset, text.length)
        while (lineStart < text.length) {
            var lineEnd = lineStart
            while (lineEnd < text.length && text[lineEnd] != '\n' && text[lineEnd] != '\r') lineEnd++
            val line = text.substring(lineStart, lineEnd)
            if (line.isNotBlank()) return line
            lineStart = nextLineStart(text, lineEnd, text.length)
        }
        return null
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

    private fun prepareForHost(
        fragment: Fragment,
        formattedContent: String,
        profile: ControlProfile,
        indentSize: Int
    ): String {
        var content = formattedContent
        if (fragment.expressionPrefix != null) {
            val prefixOffset = content.indexOf(fragment.expressionPrefix, ignoreCase = true)
            if (prefixOffset >= 0) {
                content = content.removeRange(prefixOffset, prefixOffset + fragment.expressionPrefix.length)
            }
        }

        val originalHasLineBreak = fragment.originalContent.any { char -> char == '\n' || char == '\r' }
        if (!originalHasLineBreak && profile.isBoundary && fragment.standalone) {
            val closingIndent = fragment.baseIndent + " ".repeat(profile.endDepth * indentSize)
            return "\n${fragment.baseIndent}${content.trim()}\n$closingIndent"
        }
        if (!originalHasLineBreak) return " ${content.trim()} "

        val startsOnNextLine = fragment.originalContent.startsWith('\n') || fragment.originalContent.startsWith("\r\n")
        val endsOnOwnLine = fragment.originalContent.endsWith('\n') || fragment.originalContent.endsWith('\r')
        if (startsOnNextLine && !content.startsWith('\n') && !content.startsWith('\r')) content = "\n$content"
        if (endsOnOwnLine && !content.endsWith('\n') && !content.endsWith('\r')) content += "\n"
        return addBaseIndent(content, fragment.baseIndent, profile.endDepth * indentSize)
    }

    private fun addBaseIndent(content: String, baseIndent: String, closingExtraIndent: Int): String {
        val lines = content.split('\n')
        return lines.mapIndexed { index, rawLine ->
            val line = rawLine.removeSuffix("\r")
            if (index == 0) {
                line
            } else if (index == lines.lastIndex && line.all { it == ' ' || it == '\t' }) {
                baseIndent + " ".repeat(closingExtraIndent)
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

    private fun applySemanticLayout(
        document: com.intellij.openapi.editor.Document,
        profiles: List<ControlProfile>,
        indentSize: Int
    ) {
        val snapshot = document.text
        val hosts = collectFragments(snapshot)
        if (hosts.size != profiles.size) return

        val replacements = linkedMapOf<TextRange, String>()
        hosts.forEachIndexed { index, host ->
            val profile = profiles[index]
            val openOffset = host.outerRange.startOffset
            val openLineStart = document.getLineStartOffset(document.getLineNumber(openOffset))
            val beforeOpen = snapshot.subSequence(openLineStart, openOffset)
            if (beforeOpen.all { char -> char == ' ' || char == '\t' } && profile.startDepth > 0) {
                replacements[TextRange(openLineStart, openOffset)] =
                    beforeOpen.toString() + " ".repeat(profile.startDepth * indentSize)
            }

            val segmentStart = host.outerRange.endOffset
            val segmentEnd = hosts.getOrNull(index + 1)?.outerRange?.startOffset ?: snapshot.length
            if (profile.endDepth > 0) {
                collectHtmlLineIndentReplacements(
                    snapshot,
                    segmentStart,
                    segmentEnd,
                    profile.endDepth * indentSize,
                    replacements
                )
            }
        }
        replacements.entries.sortedByDescending { (range, _) -> range.startOffset }
            .forEach { (range, replacement) ->
                document.replaceString(range.startOffset, range.endOffset, replacement)
            }
    }

    private fun collectFragments(text: String): List<Fragment> {
        val lexer = AspLexer()
        lexer.start(text)
        return buildList {
            while (lexer.tokenType != null) {
                val openingType = lexer.tokenType
                if (openingType != VbTypes.ASP_OPEN && openingType != VbTypes.ASP_EXPR_OPEN) {
                    lexer.advance()
                    continue
                }

                val outerStart = lexer.tokenStart
                val contentStart = lexer.tokenEnd
                lexer.advance()
                while (lexer.tokenType != null && lexer.tokenType != VbTypes.ASP_CLOSE) lexer.advance()
                if (lexer.tokenType != VbTypes.ASP_CLOSE) continue

                val contentRange = TextRange(contentStart, lexer.tokenStart)
                val outerRange = TextRange(outerStart, lexer.tokenEnd)
                if (!contentRange.isEmpty) {
                    add(
                        Fragment(
                            outerRange = outerRange,
                            contentRange = contentRange,
                            originalContent = contentRange.substring(text),
                            expressionPrefix = if (openingType == VbTypes.ASP_EXPR_OPEN) "Response.Write " else null,
                            baseIndent = lineIndentBefore(text, outerStart),
                            standalone = isStandaloneScriptlet(text, outerRange)
                        )
                    )
                }
                lexer.advance()
            }
        }
    }

    private fun isStandaloneScriptlet(text: String, range: TextRange): Boolean {
        val lineEnd = text.indexOfAny(charArrayOf('\r', '\n'), range.endOffset)
            .takeIf { it >= 0 } ?: text.length
        return text.subSequence(findLineStart(text, range.startOffset), range.startOffset)
            .all { it == ' ' || it == '\t' } &&
            text.subSequence(range.endOffset, lineEnd).all { it == ' ' || it == '\t' }
    }

    private fun replacementsAreSafe(
        snapshot: String,
        fragments: List<Fragment>,
        replacements: List<String>
    ): Boolean {
        if (fragments.size != replacements.size) return false
        return fragments.indices.all { index ->
            val fragment = fragments[index]
            val range = fragment.contentRange
            range.startOffset >= 0 && range.endOffset <= snapshot.length &&
                range.substring(snapshot) == fragment.originalContent &&
                preservesVbScriptTokens(fragment.originalContent, replacements[index])
        }
    }

    private fun preservesVbScriptTokens(before: String, after: String): Boolean {
        return nonWhitespaceSkeleton(before) == nonWhitespaceSkeleton(after) &&
            significantVbScriptTokens(before) == significantVbScriptTokens(after)
    }

    private fun significantVbScriptTokens(text: String): List<Pair<String, String>> {
        val lexer = VbScriptLexerAdapter()
        lexer.start(text)
        return buildList {
            while (lexer.tokenType != null) {
                val type = lexer.tokenType
                if (type != TokenType.WHITE_SPACE && type != VbTypes.EOL) {
                    val rawText = text.substring(lexer.tokenStart, lexer.tokenEnd)
                    val tokenText = if (type == VbTypes.COMMENT) {
                        rawText.filterNot(Char::isWhitespace)
                    } else {
                        rawText
                    }
                    add(type.toString() to tokenText)
                }
                lexer.advance()
            }
        }
    }

    private fun nonWhitespaceSkeleton(text: String): String = text.filterNot(Char::isWhitespace)

    private fun restoreDocument(
        documentManager: PsiDocumentManager,
        document: com.intellij.openapi.editor.Document,
        originalText: String
    ) {
        if (document.text != originalText) {
            document.replaceString(0, document.textLength, originalText)
            documentManager.commitDocument(document)
        }
    }

    private fun collectHtmlLineIndentReplacements(
        text: CharSequence,
        startOffset: Int,
        endOffset: Int,
        extraIndent: Int,
        replacements: MutableMap<TextRange, String>
    ) {
        var lineStart = if (startOffset == 0 || text[startOffset - 1] == '\n' || text[startOffset - 1] == '\r') {
            startOffset
        } else {
            nextLineStart(text, startOffset, endOffset)
        }
        while (lineStart < endOffset) {
            var firstCode = lineStart
            while (firstCode < endOffset && (text[firstCode] == ' ' || text[firstCode] == '\t')) firstCode++
            if (firstCode < endOffset && text[firstCode] != '\n' && text[firstCode] != '\r') {
                val currentIndent = text.subSequence(lineStart, firstCode).toString()
                replacements[TextRange(lineStart, firstCode)] = currentIndent + " ".repeat(extraIndent)
            }
            lineStart = nextLineStart(text, firstCode, endOffset)
        }
    }

    private fun nextLineStart(text: CharSequence, offset: Int, limit: Int): Int {
        var current = offset
        while (current < limit && text[current] != '\n' && text[current] != '\r') current++
        while (current < limit && (text[current] == '\n' || text[current] == '\r')) current++
        return current
    }

    private fun analyzeControlFlow(fragments: List<Fragment>): List<ControlProfile> {
        val tracker = VbScriptControlFlowTracker()
        return fragments.map { fragment ->
            val startDepth = tracker.depth
            var hasBoundary = false
            if (fragment.expressionPrefix == null) {
                fragment.originalContent.lineSequence().forEach { rawLine ->
                    if (tracker.consume(rawLine).isBoundary) hasBoundary = true
                }
            }
            ControlProfile(
                startDepth = startDepth,
                endDepth = tracker.depth,
                isBoundary = hasBoundary && fragment.originalContent.none { char -> char == '\n' || char == '\r' }
            )
        }
    }

    private fun marker(index: Int, boundary: String): String = "'__ASP_FORMAT_${index}_${boundary}__"

    private data class Fragment(
        val outerRange: TextRange,
        val contentRange: TextRange,
        val originalContent: String,
        val expressionPrefix: String?,
        val baseIndent: String,
        val standalone: Boolean
    )

    private data class ControlProfile(
        val startDepth: Int,
        val endDepth: Int,
        val isBoundary: Boolean
    )

    companion object {
        private val LOG = Logger.getInstance(AspPostFormatProcessor::class.java)
        private val isProcessing = ThreadLocal.withInitial { false }
    }
}
