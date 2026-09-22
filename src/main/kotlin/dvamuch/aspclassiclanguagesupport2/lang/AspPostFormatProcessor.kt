package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.lang.html.HTMLLanguage
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.DocumentUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptCodeStyleSettings
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptControlFlowTracker
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptIndentNormalizer
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptKeywordCaseSupport
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLexicalSpacingNormalizer
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLexerAdapter
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import java.util.WeakHashMap

class AspPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        if (source !is PsiFile) return source
        val file = source.containingFile ?: return source
        if (file.language != AspLanguage || isProcessing.get()) return source
        val startedAt = System.nanoTime()
        val operationSnapshot = AspFormatOperationGuard.take(file)
        try {
            formatVbScriptFragments(file, settings, placementSource = operationSnapshot?.text)
        } finally {
            validateWholeOperation(file, settings, operationSnapshot)
            AspFormatPerformanceTrace.record("post-total", file, startedAt)
        }
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source.language != AspLanguage || isProcessing.get()) return rangeToReformat
        val startedAt = System.nanoTime()
        val operationSnapshot = AspFormatOperationGuard.take(source)
        try {
            if (rangeToReformat.startOffset != 0 || rangeToReformat.endOffset < source.textLength) {
                return rangeToReformat
            }
            val oldLength = source.textLength
            formatVbScriptFragments(source, settings, placementSource = operationSnapshot?.text)
            val delta = source.textLength - oldLength
            return TextRange(rangeToReformat.startOffset, (rangeToReformat.endOffset + delta).coerceAtMost(source.textLength))
        } finally {
            validateWholeOperation(source, settings, operationSnapshot)
            AspFormatPerformanceTrace.record("post-total", source, startedAt)
        }
    }

    override fun isWhitespaceOnly(): Boolean = false

    internal fun prepareCodeSpacing(file: PsiFile, settings: CodeStyleSettings) {
        if (!isProcessing.get()) formatVbScriptFragments(file, settings, formatSpacing = true)
    }

    private fun formatVbScriptFragments(
        file: PsiFile,
        settings: CodeStyleSettings,
        formatSpacing: Boolean = false,
        placementSource: String? = null
    ) {
        val startedAt = System.nanoTime()
        var stageStartedAt = startedAt
        val passName = if (formatSpacing) "spacing" else "layout"
        fun traceStage(stage: String) {
            AspFormatPerformanceTrace.record("$passName/$stage", file, stageStartedAt)
            stageStartedAt = System.nanoTime()
        }
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        val originalDocumentText = document.text
        isProcessing.set(true)
        try {
            val keywordCase = VbScriptKeywordCaseSupport.mode(settings)
            val customSettings = settings.getCustomSettings(VbScriptCodeStyleSettings::class.java)
            val indentOptions = settings.getCommonSettings(
                dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage
            ).indentOptions
            val indentSize = indentOptions?.INDENT_SIZE ?: 4
            val continuationIndentSize = indentOptions?.CONTINUATION_INDENT_SIZE ?: 8
            val initialSnapshot = document.text
            val insertions = multilineScriptletInsertions(
                initialSnapshot,
                customSettings.MATCH_ASP_DELIMITER_PLACEMENT
            )
            if (insertions.isNotEmpty()) {
                val insertedLineOffsets = insertedLineOffsets(insertions)
                applyDocumentReplacements(
                    document,
                    insertions.map { (offset, text) -> TextRange(offset, offset) to text }
                )
                if (!formatSpacing) {
                    documentManager.commitDocument(document)
                    adjustInsertedLineIndents(file, document, insertedLineOffsets, indentSize)
                    documentManager.commitDocument(document)
                }
            }
            traceStage("delimiter-insertions")

            val fragmentSnapshot = document.text
            val fragments = collectFragments(fragmentSnapshot)
            if (fragments.isEmpty()) return
            val placementFragments = placementSource?.let(::collectFragments)
                ?.takeIf { it.size == fragments.size }
            val controlProfiles = analyzeControlFlow(fragments)
            traceStage("collect-and-control-flow")

            val markedSource = buildMarkedSource(fragments)
            // Spacing must settle BEFORE the HTML formatter calculates line wraps.
            // The lexer normalizer implements the same unambiguous operator,
            // comma, parenthesis, and dot rules as the VBScript formatting
            // model. Running the full PSI formatter on this temporary combined
            // source duplicated that work and dominated large ASP files.
            // Afterwards only recompute indentation against the final HTML layout.
            val spacedSource = if (formatSpacing) {
                VbScriptLexicalSpacingNormalizer.normalizeText(markedSource.text)
            } else {
                markedSource.text
            }
            val indentedSource = VbScriptIndentNormalizer.normalizeText(
                spacedSource,
                indentSize,
                continuationIndentSize
            )
            val formattedSource = VbScriptKeywordCaseSupport.normalizeText(
                indentedSource,
                keywordCase
            )
            traceStage("normalize")

            val extractedFragments = extractFragments(
                formattedSource,
                markedSource.namespace,
                fragments.size
            )
            val replacements = fragments.mapIndexed { index, fragment ->
                ProgressManager.checkCanceled()
                val formatted = extractedFragments?.getOrNull(index) ?: fragment.originalContent
                prepareForHost(
                    fragment = fragment,
                    formattedContent = formatted,
                    profile = controlProfiles[index],
                    indentSize = indentSize,
                    placementContent = placementFragments?.get(index)?.originalContent,
                    spaceInsideDelimiters = customSettings.SPACE_INSIDE_ASP_DELIMITERS,
                    matchDelimiterPlacement = customSettings.MATCH_ASP_DELIMITER_PLACEMENT
                )
            }
            traceStage("extract-and-prepare")
            if (!replacementsAreSafe(fragmentSnapshot, fragments, replacements, keywordCase)) {
                LOG.warn("ASP formatting cancelled because scriptlet ranges or tokens changed")
                restoreDocument(documentManager, document, originalDocumentText)
                return
            }
            traceStage("fragment-safety")
            val fragmentReplacements = fragments.indices.mapNotNull { index ->
                ProgressManager.checkCanceled()
                val range = fragments[index].contentRange
                if (fragments[index].originalContent != replacements[index]) {
                    range to replacements[index]
                } else null
            }
            applyDocumentReplacements(document, fragmentReplacements)
            traceStage("fragment-write")
            if (formatSpacing) {
                removeSemanticLayout(document, controlProfiles, indentSize)
            } else {
                applySemanticLayout(document, controlProfiles, indentSize)
                alignMultilineAttributeScriptlets(document, settings)
            }
            documentManager.commitDocument(document)
            traceStage("semantic-layout")
            val expectedDocumentText = normalizeAspKeywordCase(originalDocumentText, keywordCase)
            if (nonWhitespaceSkeleton(document.text) != nonWhitespaceSkeleton(expectedDocumentText)) {
                LOG.warn("ASP formatting changed non-whitespace document content; rolling back scriptlet pass")
                restoreDocument(documentManager, document, originalDocumentText)
            }
            traceStage("pass-safety")
        } catch (error: Throwable) {
            restoreDocument(documentManager, document, originalDocumentText)
            throw error
        } finally {
            isProcessing.remove()
            AspFormatPerformanceTrace.record(
                if (formatSpacing) "vb-spacing-pass" else "vb-layout-pass",
                file,
                startedAt
            )
        }
    }

    private fun validateWholeOperation(
        file: PsiFile,
        settings: CodeStyleSettings,
        snapshot: AspFormatOperationGuard.Snapshot?
    ) {
        if (snapshot == null) return
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        documentManager.commitDocument(document)

        val expected = if (snapshot.fullFile) {
            normalizeAspKeywordCase(snapshot.text, VbScriptKeywordCaseSupport.mode(settings))
        } else {
            snapshot.text
        }
        val actual = document.text
        val contentChanged = nonWhitespaceSkeleton(expected) != nonWhitespaceSkeleton(actual)
        val tokensChanged = aspTokenFingerprint(expected) != aspTokenFingerprint(actual)
        val parserErrorsIncreased = aspParseErrorCount(file) > snapshot.parseErrorCount
        if (contentChanged || tokensChanged || parserErrorsIncreased) {
            LOG.warn(
                "ASP Reformat Code safety guard restored the original document: " +
                    "contentChanged=$contentChanged, tokensChanged=$tokensChanged, " +
                    "parserErrorsIncreased=$parserErrorsIncreased"
            )
            restoreDocument(documentManager, document, snapshot.text)
        }
    }

    private fun aspTokenFingerprint(text: String): List<Pair<String, String>> {
        val lexer = AspLexer()
        lexer.start(text)
        return buildList {
            while (lexer.tokenType != null) {
                val type = lexer.tokenType
                if (type != AspTokenTypes.TEMPLATE_DATA && type != TokenType.WHITE_SPACE && type != VbTypes.EOL) {
                    val rawText = text.substring(lexer.tokenStart, lexer.tokenEnd)
                    val tokenText = if (type == VbTypes.COMMENT) rawText.filterNot(Char::isWhitespace) else rawText
                    add(type.toString() to tokenText)
                }
                lexer.advance()
            }
        }
    }

    private fun multilineScriptletInsertions(
        text: String,
        matchDelimiterPlacement: Boolean
    ): List<Pair<Int, String>> {
        return buildList {
            collectFragments(text).forEach { fragment ->
                if (fragment.originalContent.none { char -> char == '\n' || char == '\r' }) return@forEach

                val start = fragment.outerRange.startOffset
                val lineStart = findLineStart(text, start)
                val before = text.subSequence(lineStart, start)
                val insertBeforeOpening = before.any { char -> char != ' ' && char != '\t' } &&
                    before.lastOrNull { char -> char != ' ' && char != '\t' } == '>'
                if (insertBeforeOpening) {
                    add(start to "\n")
                }

                val end = fragment.outerRange.endOffset
                val openingIsOnOwnLine = before.all { char -> char == ' ' || char == '\t' } || insertBeforeOpening
                if (matchDelimiterPlacement && openingIsOnOwnLine) {
                    val lineEnd = text.indexOfAny(charArrayOf('\r', '\n'), end)
                        .takeIf { it >= 0 } ?: text.length
                    val after = text.subSequence(end, lineEnd)
                    if (after.any { char -> char != ' ' && char != '\t' }) add(end to "\n")
                } else if (!matchDelimiterPlacement && end < text.length && text[end] == '<') {
                    add(end to "\n")
                }
            }
        }.distinct()
    }

    private fun insertedLineOffsets(insertions: List<Pair<Int, String>>): List<Int> {
        var addedBeforeOffset = 0
        return insertions.groupBy { (offset, _) -> offset }
            .toSortedMap()
            .flatMap { (offset, atOffset) ->
                val adjusted = atOffset.map { (_, text) -> offset + text.length + addedBeforeOffset }
                addedBeforeOffset += atOffset.sumOf { (_, text) -> text.length }
                adjusted
            }
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
        applyDocumentReplacements(document, replacements)
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

    private fun buildMarkedSource(fragments: List<Fragment>): MarkedSource {
        val namespace = markerNamespace(fragments)
        return MarkedSource(
            text = buildString {
                fragments.forEachIndexed { index, fragment ->
                    append(marker(namespace, index, "START"))
                    append('\n')
                    append(fragment.expressionPrefix.orEmpty())
                    append(removeHostIndent(fragment.originalContent, fragment.baseIndent))
                    append('\n')
                    append(marker(namespace, index, "END"))
                    append('\n')
                }
            },
            namespace = namespace
        )
    }

    private fun markerNamespace(fragments: List<Fragment>): String {
        var namespace = "__ASP_FORMAT_"
        while (fragments.any { fragment -> fragment.originalContent.contains("'$namespace") }) {
            namespace += "X_"
        }
        return namespace
    }

    private fun removeHostIndent(content: String, baseIndent: String): String {
        if (baseIndent.isEmpty()) return content
        return content.split('\n').mapIndexed { index, line ->
            if (index == 0) line else line.removePrefix(baseIndent)
        }.joinToString("\n")
    }

    private fun extractFragments(source: String, namespace: String, count: Int): List<String>? {
        val result = ArrayList<String>(count)
        var searchOffset = 0
        repeat(count) { index ->
            ProgressManager.checkCanceled()
            val startMarker = marker(namespace, index, "START")
            val endMarker = marker(namespace, index, "END")
            val markerOffset = source.indexOf(startMarker, searchOffset)
            if (markerOffset < 0) return null
            val contentStart = source.indexOf('\n', markerOffset + startMarker.length)
            if (contentStart < 0) return null
            val endMarkerOffset = source.indexOf(endMarker, contentStart + 1)
            if (endMarkerOffset < 0) return null
            val contentEnd = source.lastIndexOf('\n', endMarkerOffset - 1)
            if (contentEnd < contentStart) return null

            result += source.substring(contentStart + 1, contentEnd + 1)
                .removeSuffix("\n")
                .removeSuffix("\r")
            searchOffset = endMarkerOffset + endMarker.length
        }
        return result
    }

    private fun prepareForHost(
        fragment: Fragment,
        formattedContent: String,
        profile: ControlProfile,
        indentSize: Int,
        placementContent: String?,
        spaceInsideDelimiters: Boolean,
        matchDelimiterPlacement: Boolean
    ): String {
        var content = formattedContent
        if (fragment.expressionPrefix != null) {
            val prefixOffset = content.indexOf(fragment.expressionPrefix, ignoreCase = true)
            if (prefixOffset >= 0) {
                content = content.removeRange(prefixOffset, prefixOffset + fragment.expressionPrefix.length)
            }
        }

        val placement = placementContent ?: fragment.originalContent
        val originalHasLineBreak = placement.any { char -> char == '\n' || char == '\r' }
        if (!matchDelimiterPlacement && !originalHasLineBreak && profile.isBoundary && fragment.standalone) {
            val closingIndent = fragment.baseIndent + " ".repeat(profile.endDepth * indentSize)
            return "\n${fragment.baseIndent}${content.trim()}\n$closingIndent"
        }
        if (!originalHasLineBreak) return padInlineContent(content, spaceInsideDelimiters)

        val openingIsSeparate = startsWithLineBreakAfterHorizontalWhitespace(placement)
        val closingIsSeparate = endsWithLineBreakBeforeHorizontalWhitespace(placement)
        if (matchDelimiterPlacement && (openingIsSeparate || closingIsSeparate)) {
            content = putOpeningDelimiterOnOwnLine(content)
            content = putClosingDelimiterOnOwnLine(content)
        } else {
            content = formatOpeningBoundary(content, openingIsSeparate, spaceInsideDelimiters)
            content = formatClosingBoundary(content, closingIsSeparate, spaceInsideDelimiters)
        }
        // A standalone closing delimiter belongs to the control-flow state
        // after the fragment. This deliberately differs from the opening
        // delimiter, which belongs to startDepth: a fragment containing
        // `If ... Then` opens a nested template-data region, while a fragment
        // containing `End If` closes one.
        val closingExtraIndent = profile.endDepth * indentSize
        return addBaseIndent(content, fragment.baseIndent, closingExtraIndent)
    }

    private fun padInlineContent(content: String, enabled: Boolean): String {
        val body = content.trim()
        if (body.isEmpty() || !enabled) return body
        return " $body "
    }

    private fun startsWithLineBreakAfterHorizontalWhitespace(content: String): Boolean {
        var offset = 0
        while (offset < content.length && (content[offset] == ' ' || content[offset] == '\t')) offset++
        return offset < content.length && (content[offset] == '\n' || content[offset] == '\r')
    }

    private fun endsWithLineBreakBeforeHorizontalWhitespace(content: String): Boolean {
        var offset = content.length - 1
        while (offset >= 0 && (content[offset] == ' ' || content[offset] == '\t')) offset--
        return offset >= 0 && (content[offset] == '\n' || content[offset] == '\r')
    }

    private fun putOpeningDelimiterOnOwnLine(content: String): String {
        var boundaryEnd = 0
        while (boundaryEnd < content.length && (content[boundaryEnd] == ' ' || content[boundaryEnd] == '\t')) boundaryEnd++
        if (boundaryEnd < content.length && content[boundaryEnd] == '\r') boundaryEnd++
        if (boundaryEnd < content.length && content[boundaryEnd] == '\n') boundaryEnd++
        return "\n" + content.substring(boundaryEnd)
    }

    private fun putClosingDelimiterOnOwnLine(content: String): String {
        var boundaryStart = content.length
        while (boundaryStart > 0 && (content[boundaryStart - 1] == ' ' || content[boundaryStart - 1] == '\t')) boundaryStart--
        if (boundaryStart > 0 && content[boundaryStart - 1] == '\n') boundaryStart--
        if (boundaryStart > 0 && content[boundaryStart - 1] == '\r') boundaryStart--
        return content.substring(0, boundaryStart) + "\n"
    }

    private fun formatOpeningBoundary(content: String, separate: Boolean, spaces: Boolean): String {
        if (separate) return putOpeningDelimiterOnOwnLine(content)
        val body = content.trimStart(' ', '\t')
        return if (spaces && body.isNotEmpty()) " $body" else body
    }

    private fun formatClosingBoundary(content: String, separate: Boolean, spaces: Boolean): String {
        if (separate) return putClosingDelimiterOnOwnLine(content)
        val body = content.trimEnd(' ', '\t')
        return if (spaces && body.isNotEmpty()) "$body " else body
    }

    private fun addBaseIndent(content: String, baseIndent: String, closingExtraIndent: Int): String {
        val lines = content.split('\n')
        return lines.mapIndexed { index, rawLine ->
            val line = rawLine.removeSuffix("\r")
            if (index == 0) {
                line
            } else if (index == lines.lastIndex && line.all { it == ' ' || it == '\t' }) {
                baseIndent + " ".repeat(closingExtraIndent)
            } else if (line.all { it == ' ' || it == '\t' }) {
                // Indentation on an otherwise empty line is not semantic and must
                // not be fed back into the next formatter pass. Keeping it here
                // made nested scriptlets gain or lose spaces on every reformat.
                ""
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
            ProgressManager.checkCanceled()
            val profile = profiles[index]
            val openOffset = host.outerRange.startOffset
            val openLineStart = document.getLineStartOffset(document.getLineNumber(openOffset))
            val beforeOpen = snapshot.subSequence(openLineStart, openOffset)
            if (beforeOpen.all { char -> char == ' ' || char == '\t' } && profile.openingDepth > 0) {
                replacements[TextRange(openLineStart, openOffset)] =
                    beforeOpen.toString() + " ".repeat(profile.openingDepth * indentSize)
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
        applyDocumentReplacements(document, replacements.entries.map { (range, replacement) -> range to replacement })
    }

    /**
     * The post-format pass adds indentation for VBScript control flow spanning
     * template-data blocks. Some malformed legacy HTML is deliberately left
     * untouched by the platform formatter, so that added indentation would be
     * fed back and accumulated on the next reformat. Remove only the exact
     * space suffix that our post-pass can have added, then recompute it after
     * the platform HTML formatter has established the new base indentation.
     */
    private fun removeSemanticLayout(
        document: com.intellij.openapi.editor.Document,
        profiles: List<ControlProfile>,
        indentSize: Int
    ) {
        val snapshot = document.text
        val hosts = collectFragments(snapshot)
        if (hosts.size != profiles.size) return

        val replacements = linkedMapOf<TextRange, String>()
        hosts.forEachIndexed { index, host ->
            ProgressManager.checkCanceled()
            val profile = profiles[index]
            val openOffset = host.outerRange.startOffset
            val openLineStart = document.getLineStartOffset(document.getLineNumber(openOffset))
            val beforeOpen = snapshot.subSequence(openLineStart, openOffset).toString()
            if (beforeOpen.all { char -> char == ' ' || char == '\t' } && profile.openingDepth > 0) {
                val normalized = removeSemanticIndentSuffix(beforeOpen, profile.openingDepth * indentSize)
                if (normalized != beforeOpen) {
                    replacements[TextRange(openLineStart, openOffset)] = normalized
                }
            }

            val segmentStart = host.outerRange.endOffset
            val segmentEnd = hosts.getOrNull(index + 1)?.outerRange?.startOffset ?: snapshot.length
            if (profile.endDepth > 0) {
                collectHtmlLineUnindentReplacements(
                    snapshot,
                    segmentStart,
                    segmentEnd,
                    profile.endDepth * indentSize,
                    replacements
                )
            }
        }
        applyDocumentReplacements(document, replacements.entries.map { (range, replacement) -> range to replacement })
    }

    private fun collectFragments(text: String): List<Fragment> {
        val lexer = AspLexer()
        lexer.start(text)
        return buildList {
            var tokenCount = 0
            while (lexer.tokenType != null) {
                if (tokenCount++ and 0xff == 0) ProgressManager.checkCanceled()
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
        replacements: List<String>,
        keywordCase: Int
    ): Boolean {
        if (fragments.size != replacements.size) return false
        return fragments.indices.all { index ->
            ProgressManager.checkCanceled()
            val fragment = fragments[index]
            val range = fragment.contentRange
                range.startOffset >= 0 && range.endOffset <= snapshot.length &&
                range.substring(snapshot) == fragment.originalContent &&
                preservesVbScriptTokens(fragment.originalContent, replacements[index], keywordCase)
        }
    }

    private fun preservesVbScriptTokens(before: String, after: String, keywordCase: Int): Boolean {
        val expected = VbScriptKeywordCaseSupport.normalizeText(before, keywordCase)
        return nonWhitespaceSkeleton(expected) == nonWhitespaceSkeleton(after) &&
            significantVbScriptTokens(expected) == significantVbScriptTokens(after)
    }

    private fun normalizeAspKeywordCase(text: String, keywordCase: Int): String {
        val fragments = collectFragments(text)
        if (fragments.isEmpty()) return text
        return buildString(text.length) {
            var sourceOffset = 0
            fragments.forEach { fragment ->
                append(text, sourceOffset, fragment.contentRange.startOffset)
                append(VbScriptKeywordCaseSupport.normalizeText(fragment.originalContent, keywordCase))
                sourceOffset = fragment.contentRange.endOffset
            }
            append(text, sourceOffset, text.length)
        }
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

    private fun aspParseErrorCount(file: PsiFile): Int {
        val aspPsi = file.viewProvider.getPsi(AspLanguage) ?: return 0
        return PsiTreeUtil.collectElementsOfType(aspPsi, PsiErrorElement::class.java).size
    }

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

    private fun collectHtmlLineUnindentReplacements(
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
                val normalized = removeSemanticIndentSuffix(currentIndent, extraIndent)
                if (normalized != currentIndent) {
                    replacements[TextRange(lineStart, firstCode)] = normalized
                }
            }
            lineStart = nextLineStart(text, firstCode, endOffset)
        }
    }

    private fun removeSemanticIndentSuffix(indent: String, extraIndent: Int): String {
        if (extraIndent <= 0 || indent.length < extraIndent) return indent
        val suffixStart = indent.length - extraIndent
        return if (indent.regionMatches(suffixStart, " ".repeat(extraIndent), 0, extraIndent)) {
            indent.substring(0, suffixStart)
        } else {
            indent
        }
    }

    /**
     * The HTML formatter treats ASP blocks as outer-language placeholders and
     * cannot reliably indent a placeholder that starts a continuation line in
     * a quoted attribute value. Align only that narrow case relative to the
     * line where the attribute value starts. ASP ranges are skipped while
     * scanning so quotes in expressions do not close the HTML attribute.
     */
    private fun alignMultilineAttributeScriptlets(
        document: com.intellij.openapi.editor.Document,
        settings: CodeStyleSettings
    ) {
        val text = document.text
        val fragments = collectFragments(text)
        if (fragments.isEmpty()) return
        val continuationIndent = settings.getCommonSettings(HTMLLanguage.INSTANCE)
            .indentOptions?.CONTINUATION_INDENT_SIZE ?: 4
        val scanner = HtmlAttributeScanner(text)
        val replacements = mutableListOf<Pair<TextRange, String>>()

        fragments.forEach { fragment ->
            ProgressManager.checkCanceled()
            scanner.scanUntil(fragment.outerRange.startOffset)
            if (scanner.attributeLinePrefix != null) {
                val lineStart = findLineStart(text, fragment.outerRange.startOffset)
                val before = text.substring(lineStart, fragment.outerRange.startOffset)
                if (before.all { it == ' ' || it == '\t' } &&
                    lineStart > scanner.attributeLineStart
                ) {
                    val desired = scanner.attributeLinePrefix + " ".repeat(continuationIndent)
                    if (before != desired) {
                        replacements += TextRange(lineStart, fragment.outerRange.startOffset) to desired
                    }
                }
            }
            scanner.skipUntil(fragment.outerRange.endOffset)
        }

        applyDocumentReplacements(document, replacements)
    }

    private fun applyDocumentReplacements(
        document: com.intellij.openapi.editor.Document,
        replacements: Collection<Pair<TextRange, String>>
    ) {
        if (replacements.isEmpty()) return
        val action = Runnable {
            replacements.sortedByDescending { (range, _) -> range.startOffset }
                .forEach { (range, replacement) ->
                    ProgressManager.checkCanceled()
                    document.replaceString(range.startOffset, range.endOffset, replacement)
                }
        }
        if (replacements.size >= BULK_UPDATE_REPLACEMENT_THRESHOLD) {
            DocumentUtil.executeInBulk(document, true, action)
        } else {
            action.run()
        }
    }

    private class HtmlAttributeScanner(private val text: String) {
        var attributeLinePrefix: String? = null
            private set
        var attributeLineStart: Int = -1
            private set
        private var offset = 0
        private var inTag = false
        private var quote: Char? = null

        fun scanUntil(limit: Int) {
            while (offset < limit) {
                val char = text[offset]
                if (!inTag) {
                    if (char == '<' && isHtmlTagStart(offset)) inTag = true
                } else if (quote == null) {
                    when (char) {
                        '>' -> inTag = false
                        '\'', '"' -> {
                            quote = char
                            attributeLineStart = lineStartAt(offset)
                            attributeLinePrefix = leadingIndentAt(attributeLineStart)
                        }
                    }
                } else if (char == quote) {
                    quote = null
                    attributeLinePrefix = null
                    attributeLineStart = -1
                }
                offset++
            }
        }

        fun skipUntil(limit: Int) {
            offset = limit.coerceAtLeast(offset)
        }

        private fun lineStartAt(position: Int): Int {
            var start = position
            while (start > 0 && text[start - 1] != '\n' && text[start - 1] != '\r') start--
            return start
        }

        private fun leadingIndentAt(lineStart: Int): String {
            var end = lineStart
            while (end < text.length && (text[end] == ' ' || text[end] == '\t')) end++
            return text.substring(lineStart, end)
        }

        private fun isHtmlTagStart(position: Int): Boolean {
            val next = text.getOrNull(position + 1) ?: return false
            // Exclude comments/declarations so quotes inside <!-- ... --> or a
            // doctype cannot be mistaken for a multiline attribute value.
            return next.isLetter() || next == '/'
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
            ProgressManager.checkCanceled()
            val startDepth = tracker.depth
            var hasBoundary = false
            var codeDepth: Int? = null
            if (fragment.expressionPrefix == null) {
                fragment.originalContent.lineSequence().forEach { rawLine ->
                    val result = tracker.consume(rawLine)
                    if (rawLine.isNotBlank()) {
                        codeDepth = minOf(codeDepth ?: result.indentLevel, result.indentLevel)
                    }
                    if (result.isBoundary) hasBoundary = true
                }
            }
            val hasLineBreak = fragment.originalContent.any { char -> char == '\n' || char == '\r' }
            ControlProfile(
                endDepth = tracker.depth,
                openingDepth = if (hasLineBreak) startDepth else codeDepth ?: startDepth,
                isBoundary = hasBoundary && !hasLineBreak
            )
        }
    }

    private fun marker(namespace: String, index: Int, boundary: String): String =
        "'${namespace}${index}_${boundary}__"

    private data class Fragment(
        val outerRange: TextRange,
        val contentRange: TextRange,
        val originalContent: String,
        val expressionPrefix: String?,
        val baseIndent: String,
        val standalone: Boolean
    )

    private data class MarkedSource(
        val text: String,
        val namespace: String
    )

    private data class ControlProfile(
        val endDepth: Int,
        val openingDepth: Int,
        val isBoundary: Boolean
    )

    companion object {
        private const val BULK_UPDATE_REPLACEMENT_THRESHOLD = 64
        private val LOG = Logger.getInstance(AspPostFormatProcessor::class.java)
        private val isProcessing = ThreadLocal.withInitial { false }
    }
}

internal object AspFormatPerformanceTrace {
    private const val PROPERTY = "asp.format.performance.trace"

    fun record(stage: String, file: PsiFile, startedAtNanos: Long) {
        if (!java.lang.Boolean.getBoolean(PROPERTY)) return
        val elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000
        println("ASP FORMAT TRACE stage=$stage elapsed=${elapsedMillis}ms file=${file.name}")
    }
}

internal object AspFormatOperationGuard {
    internal data class Snapshot(
        val text: String,
        val fullFile: Boolean,
        val parseErrorCount: Int,
        val commandIdentity: Runnable?,
        val capturedAtNanos: Long
    )

    private const val COMMANDLESS_SNAPSHOT_MAX_AGE_NANOS = 5L * 60L * 1_000_000_000L
    private val current = ThreadLocal.withInitial { WeakHashMap<PsiFile, Snapshot>() }

    fun capture(file: PsiFile, range: TextRange) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        documentManager.commitDocument(document)
        val aspPsi = file.viewProvider.getPsi(AspLanguage) ?: file
        current.get()[file] = Snapshot(
            text = document.text,
            fullFile = range.startOffset == 0 && range.endOffset >= file.textLength,
            parseErrorCount = PsiTreeUtil.collectElementsOfType(aspPsi, PsiErrorElement::class.java).size,
            commandIdentity = CommandProcessor.getInstance().currentCommand,
            capturedAtNanos = System.nanoTime()
        )
    }

    fun take(file: PsiFile): Snapshot? {
        val snapshots = current.get()
        val snapshot = snapshots.remove(file)
        if (snapshots.isEmpty()) current.remove()
        snapshot ?: return null

        val currentCommand = CommandProcessor.getInstance().currentCommand
        if (snapshot.commandIdentity !== currentCommand) return null
        if (currentCommand == null && System.nanoTime() - snapshot.capturedAtNanos > COMMANDLESS_SNAPSHOT_MAX_AGE_NANOS) {
            return null
        }
        return snapshot
    }

    fun discard(file: PsiFile) {
        val snapshots = current.get()
        snapshots.remove(file)
        if (snapshots.isEmpty()) current.remove()
    }
}
