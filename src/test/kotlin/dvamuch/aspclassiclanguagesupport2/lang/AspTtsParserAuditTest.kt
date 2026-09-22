package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.streams.asSequence

/**
 * Optional read-only parser inventory for the external TTS checkout.
 *
 * Unlike a regression test, parser diagnostics do not fail the task: old
 * projects can legitimately contain archived, truncated or non-ASP data in an
 * `.asp` file. Parser crashes do fail it. The complete diagnostics inventory is
 * written to `build/reports/tts-parser-audit.txt` for manual classification.
 */
class AspTtsParserAuditTest : BasePlatformTestCase() {
    fun testOptionalTtsParserAudit() {
        val configuredRoot = System.getProperty(TTS_PROJECT_DIR_PROPERTY)
        if (configuredRoot.isNullOrBlank()) return

        val corpusRoot = Path.of(configuredRoot).toAbsolutePath().normalize()
        require(Files.isDirectory(corpusRoot)) { "TTS project directory does not exist: $corpusRoot" }
        val pathFilter = System.getProperty(TTS_PATH_FILTER_PROPERTY).orEmpty()
        val files = Files.walk(corpusRoot).use { stream ->
            stream.asSequence()
                .filter(Files::isRegularFile)
                .filter { it.extension.lowercase() in SUPPORTED_EXTENSIONS }
                .filter {
                    pathFilter.isBlank() ||
                        corpusRoot.relativize(it).toString().contains(pathFilter, ignoreCase = true)
                }
                .sortedBy { corpusRoot.relativize(it).toString() }
                .toList()
        }
        require(files.isNotEmpty()) { "No TTS files matched path filter: $pathFilter" }

        val findings = mutableListOf<FileFinding>()
        val crashes = mutableListOf<String>()
        val startedAt = System.nanoTime()
        files.forEachIndexed { index, path ->
            val relativePath = corpusRoot.relativize(path).toString()
            try {
                val source = readSource(path)
                val root = myFixture.configureByText(path.fileName.toString(), source)
                val aspPsi = root.viewProvider.getPsi(AspLanguage)
                    ?: error("ASP PSI is unavailable")
                val errors = PsiTreeUtil.collectElementsOfType(aspPsi, PsiErrorElement::class.java)
                if (errors.isNotEmpty()) {
                    findings += FileFinding(
                        relativePath,
                        errors.map { error ->
                            val offset = error.textOffset.coerceIn(0, source.length)
                            ErrorFinding(
                                line = source.lineNumberAt(offset),
                                description = error.errorDescription,
                                context = source.contextAt(offset)
                            )
                        }
                    )
                }
            } catch (error: Throwable) {
                crashes += "$relativePath: ${error::class.java.simpleName}: ${error.message}"
            }

            if ((index + 1) % 100 == 0 || index + 1 == files.size) {
                println(
                    "TTS parser audit: ${index + 1}/${files.size}, " +
                        "diagnosticFiles=${findings.size}, crashes=${crashes.size}"
                )
                System.out.flush()
            }
        }

        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
        val reportPath = Path.of(System.getProperty("user.dir"), "build", "reports", "tts-parser-audit.txt")
        writeReport(reportPath, corpusRoot, files.size, elapsedMillis, findings, crashes)
        println("TTS parser audit report: $reportPath")

        assertTrue(
            "Parser crashed for ${crashes.size} TTS files. Full report: $reportPath\n" +
                crashes.take(20).joinToString("\n"),
            crashes.isEmpty()
        )
    }

    private fun writeReport(
        reportPath: Path,
        corpusRoot: Path,
        checkedFiles: Int,
        elapsedMillis: Long,
        findings: List<FileFinding>,
        crashes: List<String>
    ) {
        Files.createDirectories(reportPath.parent)
        Files.writeString(
            reportPath,
            buildString {
                appendLine("TTS parser audit")
                appendLine("Corpus: $corpusRoot")
                appendLine("Files checked: $checkedFiles")
                appendLine("Files with diagnostics: ${findings.size}")
                appendLine("Parser crashes: ${crashes.size}")
                appendLine("Elapsed: ${elapsedMillis}ms")
                appendLine()
                crashes.forEach { appendLine("CRASH\t$it") }
                findings.forEach { finding ->
                    appendLine("FILE\t${finding.path}\t${finding.errors.size}")
                    finding.errors.forEach { error ->
                        appendLine(
                            "  line ${error.line}\t${error.description}\t${error.context}"
                        )
                    }
                }
            },
            StandardCharsets.UTF_8
        )
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

    private fun String.lineNumberAt(offset: Int): Int =
        1 + take(offset).count { it == '\n' }

    private fun String.contextAt(offset: Int): String {
        val start = (offset - CONTEXT_RADIUS).coerceAtLeast(0)
        val end = (offset + CONTEXT_RADIUS).coerceAtMost(length)
        return substring(start, end)
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
    }

    private data class FileFinding(val path: String, val errors: List<ErrorFinding>)

    private data class ErrorFinding(val line: Int, val description: String, val context: String)

    companion object {
        private const val TTS_PROJECT_DIR_PROPERTY = "tts.project.dir"
        private const val TTS_PATH_FILTER_PROPERTY = "tts.path.filter"
        private const val CONTEXT_RADIUS = 60
        private val SUPPORTED_EXTENSIONS = setOf("asp", "inc")
        private val WINDOWS_1251 = charset("windows-1251").newDecoder()
    }
}
