package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLexerAdapter
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.streams.asSequence

/**
 * Optional destructive-change safety gate for the external TTS corpus.
 *
 * The source files are only read. Formatting is performed on in-memory fixture
 * copies. Run explicitly with:
 *
 * ./gradlew test --tests '*AspTtsFormattingSafetyTest' \
 *   -PttsProjectDir=/absolute/path/to/TTS \
 *   -PttsBatchSize=100 -PttsBatchIndex=0
 *
 * Batch indexes are zero-based. Use -PttsPathFilter=relative/path to isolate a
 * single file or directory. Progress is streamed to stdout and persisted under
 * build/reports even if the process is interrupted.
 */
class AspTtsFormattingSafetyTest : BasePlatformTestCase() {
    fun testFormattingPreservesTtsCorpusSyntax() {
        val configuredRoot = System.getProperty(TTS_PROJECT_DIR_PROPERTY)
        if (configuredRoot.isNullOrBlank()) return

        val corpusRoot = Path.of(configuredRoot).toAbsolutePath().normalize()
        require(Files.isDirectory(corpusRoot)) { "TTS project directory does not exist: $corpusRoot" }
        val allFiles = Files.walk(corpusRoot).use { stream ->
            stream.asSequence()
                .filter(Files::isRegularFile)
                .filter { path -> path.extension.lowercase() in SUPPORTED_EXTENSIONS }
                .sortedBy { path -> corpusRoot.relativize(path).toString() }
                .toList()
        }
        val pathFilter = System.getProperty(TTS_PATH_FILTER_PROPERTY).orEmpty()
        val filteredFiles = if (pathFilter.isBlank()) {
            allFiles
        } else {
            allFiles.filter { path ->
                corpusRoot.relativize(path).toString().contains(pathFilter, ignoreCase = true)
            }
        }
        require(filteredFiles.isNotEmpty()) { "No TTS files matched path filter: $pathFilter" }

        val batchSize = positiveIntProperty(TTS_BATCH_SIZE_PROPERTY, filteredFiles.size)
        val batchIndex = nonNegativeIntProperty(TTS_BATCH_INDEX_PROPERTY, 0)
        val batchStart = batchIndex * batchSize
        require(batchStart < filteredFiles.size) {
            "Batch $batchIndex starts at $batchStart, but only ${filteredFiles.size} files matched"
        }
        val files = filteredFiles.drop(batchStart).take(batchSize)
        val batchEnd = batchStart + files.size
        val reportPath = batchReportPath(batchIndex)
        val progressPath = progressReportPath()

        val failures = mutableListOf<Failure>()
        println(
            "TTS formatter safety batch $batchIndex: files ${batchStart + 1}..$batchEnd " +
                "of ${filteredFiles.size} matched (${allFiles.size} total)"
        )
        System.out.flush()
        writeProgress(progressPath, batchIndex, batchStart, batchEnd, 0, null, failures.size)
        writeReport(reportPath, allFiles.size, filteredFiles.size, batchIndex, batchStart, batchEnd, 0, failures)

        files.forEachIndexed { index, path ->
            val relativePath = corpusRoot.relativize(path).toString()
            val absoluteIndex = batchStart + index
            val startedAt = System.nanoTime()
            println("[${index + 1}/${files.size}] START #${absoluteIndex + 1} $relativePath")
            System.out.flush()
            writeProgress(progressPath, batchIndex, batchStart, batchEnd, index, relativePath, failures.size)
            val failuresBefore = failures.size
            try {
                verifyFile(path, relativePath, failures)
            } catch (error: Throwable) {
                failures += Failure(relativePath, "formatter threw ${error::class.java.simpleName}: ${error.message}")
            }
            val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
            val newFailures = failures.drop(failuresBefore)
            if (newFailures.isEmpty()) {
                println("[${index + 1}/${files.size}] OK ${elapsedMillis}ms $relativePath")
            } else {
                newFailures.forEach { failure ->
                    println("[${index + 1}/${files.size}] FAIL ${elapsedMillis}ms ${failure.path}: ${failure.reason}")
                }
            }
            System.out.flush()
            writeProgress(progressPath, batchIndex, batchStart, batchEnd, index + 1, null, failures.size)
            writeReport(
                reportPath,
                allFiles.size,
                filteredFiles.size,
                batchIndex,
                batchStart,
                batchEnd,
                index + 1,
                failures
            )
        }

        val summary = failures.take(MAX_FAILURES_IN_ASSERTION).joinToString("\n") { failure ->
            "- ${failure.path}: ${failure.reason}"
        }
        assertTrue(
            "Formatting safety violations in batch $batchIndex: ${failures.size}/${files.size}. " +
                "Full report: $reportPath\n$summary",
            failures.isEmpty()
        )
    }

    private fun verifyFile(path: Path, relativePath: String, failures: MutableList<Failure>) {
        val original = readSource(path)
        val file = myFixture.configureByText(path.fileName.toString(), original)
        // Compare against the document text actually presented to the formatter.
        // IntelliJ removes an encoding BOM while creating the PSI/document, which
        // is an input-decoding change rather than a formatting change.
        val before = snapshot(file, file.text, path.extension)

        val document = myFixture.editor.document
        var firstDestructiveChange: String? = null
        val listener = object : DocumentListener {
            override fun beforeDocumentChange(event: DocumentEvent) {
                if (firstDestructiveChange == null &&
                    event.oldFragment.filterNot(Char::isWhitespace) != event.newFragment.filterNot(Char::isWhitespace)
                ) {
                    firstDestructiveChange = "offset=${event.offset}, oldLength=${event.oldLength}, newLength=${event.newLength}\n" +
                        Throwable().stackTrace.joinToString("\n")
                }
            }
        }
        document.addDocumentListener(listener)
        try {
            WriteCommandAction.runWriteCommandAction(project) {
                CodeStyleManager.getInstance(project).reformat(file)
            }
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        } finally {
            document.removeDocumentListener(listener)
        }

        val formatted = file.text
        val after = snapshot(file, formatted, path.extension)

        if (before.nonWhitespaceSkeleton != after.nonWhitespaceSkeleton) {
            val artifactDir = reportsDir().resolve("tts-format-diagnostics").resolve(relativePath)
            Files.createDirectories(artifactDir)
            Files.writeString(artifactDir.resolve("before.txt"), original)
            Files.writeString(artifactDir.resolve("after.txt"), formatted)
            Files.writeString(artifactDir.resolve("first-change.txt"), firstDestructiveChange.orEmpty())
            failures += Failure(
                relativePath,
                "non-whitespace characters changed: " + firstDifference(
                    before.nonWhitespaceSkeleton,
                    after.nonWhitespaceSkeleton
                )
            )
        }
        if (before.vbTokens != after.vbTokens) {
            failures += Failure(relativePath, "VBScript token stream changed: ${firstTokenDifference(before.vbTokens, after.vbTokens)}")
        }
        if (after.parseErrorCount > before.parseErrorCount) {
            failures += Failure(
                relativePath,
                "parser errors increased from ${before.parseErrorCount} to ${after.parseErrorCount}"
            )
        }
    }

    private fun snapshot(file: PsiFile, text: String, extension: String): Snapshot {
        return Snapshot(
            nonWhitespaceSkeleton = text.filterNot(Char::isWhitespace),
            vbTokens = if (extension.equals("vbs", ignoreCase = true)) {
                lexVbScript(text)
            } else {
                lexAspScriptlets(text)
            },
            parseErrorCount = parseErrorCount(file, extension)
        )
    }

    private fun lexAspScriptlets(text: String): List<TokenFingerprint> {
        val lexer = AspLexer()
        lexer.start(text)
        val result = mutableListOf<TokenFingerprint>()
        while (lexer.tokenType != null) {
            val type = lexer.tokenType
            if (type != AspTokenTypes.TEMPLATE_DATA && type != TokenType.WHITE_SPACE && type != VbTypes.EOL) {
                val rawText = text.substring(lexer.tokenStart, lexer.tokenEnd)
                val tokenText = if (type == VbTypes.COMMENT) rawText.filterNot(Char::isWhitespace) else rawText
                result += TokenFingerprint(type.toString(), tokenText)
            }
            lexer.advance()
        }
        return result
    }

    private fun lexVbScript(text: String): List<TokenFingerprint> {
        val lexer = VbScriptLexerAdapter()
        lexer.start(text)
        return buildList {
            while (lexer.tokenType != null) {
                val type = lexer.tokenType
                if (type != TokenType.WHITE_SPACE && type != VbTypes.EOL) {
                    val tokenText = text.substring(lexer.tokenStart, lexer.tokenEnd)
                    val normalizedText = if (type == VbTypes.COMMENT) {
                        // Whitespace inside a comment is non-executable and may be
                        // adjusted when an inline scriptlet gets delimiter padding.
                        // Keep every non-whitespace character in the fingerprint.
                        tokenText.filterNot(Char::isWhitespace)
                    } else {
                        tokenText
                    }
                    add(TokenFingerprint(type.toString(), normalizedText))
                }
                lexer.advance()
            }
        }
    }

    private fun parseErrorCount(file: PsiFile, extension: String): Int {
        if (extension.equals("vbs", ignoreCase = true)) {
            return PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java).size
        }

        val aspPsi = file.viewProvider.getPsi(AspLanguage) ?: return 0
        return PsiTreeUtil.collectElementsOfType(aspPsi, PsiErrorElement::class.java).size
    }

    fun testSafetyFingerprintActuallySeesNativeAspCode() {
        val before = lexAspScriptlets("<% value = \"a b\" %>")
        assertTrue("ASP fingerprint must contain executable tokens", before.size > 2)
        assertFalse(before == lexAspScriptlets("<% value = \"ab\" %>"))
        val broken = myFixture.configureByText("broken.asp", "<% Dim =\n%>")
        assertTrue("Parser safety check must see native ASP errors", parseErrorCount(broken, "asp") > 0)
    }

    private fun firstDifference(before: String, after: String): String {
        val mismatch = (0 until minOf(before.length, after.length)).firstOrNull { before[it] != after[it] }
            ?: minOf(before.length, after.length)
        return "offset=$mismatch, before=${context(before, mismatch)}, after=${context(after, mismatch)}"
    }

    private fun firstTokenDifference(
        before: List<TokenFingerprint>,
        after: List<TokenFingerprint>
    ): String {
        val mismatch = (0 until minOf(before.size, after.size)).firstOrNull { before[it] != after[it] }
            ?: minOf(before.size, after.size)
        return "index=$mismatch, before=${before.getOrNull(mismatch)}, after=${after.getOrNull(mismatch)}"
    }

    private fun context(text: String, offset: Int): String {
        val start = (offset - 40).coerceAtLeast(0)
        val end = (offset + 40).coerceAtMost(text.length)
        return text.substring(start, end).replace("\r", "\\r").replace("\n", "\\n")
    }

    private fun readSource(path: Path): String {
        val bytes = Files.readAllBytes(path)
        return runCatching {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrElse {
            WINDOWS_1251.decode(ByteBuffer.wrap(bytes)).toString()
        }
    }

    private fun batchReportPath(batchIndex: Int): Path {
        return reportsDir().resolve("tts-format-safety-batch-${batchIndex.toString().padStart(4, '0')}.txt")
    }

    private fun progressReportPath(): Path = reportsDir().resolve("tts-format-safety-progress.txt")

    private fun reportsDir(): Path = Path.of(System.getProperty("user.dir"), "build", "reports")

    private fun writeReport(
        reportPath: Path,
        totalCorpusFiles: Int,
        matchedFiles: Int,
        batchIndex: Int,
        batchStart: Int,
        batchEnd: Int,
        checkedFiles: Int,
        failures: List<Failure>
    ) {
        Files.createDirectories(reportPath.parent)
        val report = buildString {
            appendLine("TTS formatting safety report")
            appendLine("Corpus files: $totalCorpusFiles")
            appendLine("Matched files: $matchedFiles")
            appendLine("Batch: $batchIndex")
            appendLine("Batch range: ${batchStart + 1}..$batchEnd")
            appendLine("Files checked in batch: $checkedFiles/${batchEnd - batchStart}")
            appendLine("Violations: ${failures.size}")
            failures.forEach { failure -> appendLine("${failure.path}\t${failure.reason}") }
        }
        Files.writeString(reportPath, report, StandardCharsets.UTF_8)
    }

    private fun writeProgress(
        progressPath: Path,
        batchIndex: Int,
        batchStart: Int,
        batchEnd: Int,
        completedFiles: Int,
        currentFile: String?,
        failureCount: Int
    ) {
        Files.createDirectories(progressPath.parent)
        Files.writeString(
            progressPath,
            buildString {
                appendLine("Batch: $batchIndex")
                appendLine("Batch range: ${batchStart + 1}..$batchEnd")
                appendLine("Completed: $completedFiles/${batchEnd - batchStart}")
                appendLine("Current: ${currentFile ?: "-"}")
                appendLine("Violations so far: $failureCount")
            },
            StandardCharsets.UTF_8
        )
    }

    private fun positiveIntProperty(name: String, defaultValue: Int): Int {
        val value = System.getProperty(name)?.toIntOrNull() ?: defaultValue
        require(value > 0) { "$name must be positive, got $value" }
        return value
    }

    private fun nonNegativeIntProperty(name: String, defaultValue: Int): Int {
        val value = System.getProperty(name)?.toIntOrNull() ?: defaultValue
        require(value >= 0) { "$name must be non-negative, got $value" }
        return value
    }

    private data class Snapshot(
        val nonWhitespaceSkeleton: String,
        val vbTokens: List<TokenFingerprint>,
        val parseErrorCount: Int
    )

    private data class TokenFingerprint(val type: String, val text: String)

    private data class Failure(val path: String, val reason: String)

    companion object {
        private const val TTS_PROJECT_DIR_PROPERTY = "tts.project.dir"
        private const val TTS_BATCH_SIZE_PROPERTY = "tts.batch.size"
        private const val TTS_BATCH_INDEX_PROPERTY = "tts.batch.index"
        private const val TTS_PATH_FILTER_PROPERTY = "tts.path.filter"
        private const val MAX_FAILURES_IN_ASSERTION = 40
        private val SUPPORTED_EXTENSIONS = setOf("asp", "inc", "vbs")
        private val WINDOWS_1251 = charset("windows-1251").newDecoder()
    }
}
