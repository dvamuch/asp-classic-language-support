package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbUnresolvedIdentifierInspection
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** Read-only inventory of Option Explicit warnings in the external TTS checkout. */
class AspTtsInspectionAuditTest : BasePlatformTestCase() {
    fun testOptionalTtsOptionExplicitInspectionAudit() {
        val configuredRoot = System.getProperty(TTS_PROJECT_DIR_PROPERTY)
        if (configuredRoot.isNullOrBlank()) return

        val corpusRoot = Path.of(configuredRoot).toAbsolutePath().normalize()
        val sourcePaths = (OPTION_EXPLICIT_PATHS + SUPPORT_PATHS).associateWith { relativePath ->
            val path = corpusRoot.resolve(relativePath)
            require(Files.isRegularFile(path)) { "TTS audit file does not exist: $path" }
            readSource(path)
        }
        val projectFiles = sourcePaths.mapValues { (relativePath, source) ->
            myFixture.addFileToProject(relativePath, source)
        }
        myFixture.enableInspections(VbUnresolvedIdentifierInspection::class.java)

        val warnings = mutableListOf<Warning>()
        OPTION_EXPLICIT_PATHS.forEach { relativePath ->
            val source = sourcePaths.getValue(relativePath)
            val file = projectFiles.getValue(relativePath)
            myFixture.configureFromExistingVirtualFile(file.virtualFile)
            myFixture.doHighlighting()
                .filter { it.description?.startsWith(UNDECLARED_PREFIX) == true }
                .forEach { info ->
                    val offset = info.startOffset.coerceIn(0, source.length)
                    warnings += Warning(
                        relativePath,
                        1 + source.take(offset).count { it == '\n' },
                        info.description ?: UNDECLARED_PREFIX
                    )
                }
        }

        val reportPath = Path.of(
            System.getProperty("user.dir"),
            "build",
            "reports",
            "tts-option-explicit-inspection-audit.txt"
        )
        Files.createDirectories(reportPath.parent)
        Files.writeString(
            reportPath,
            buildString {
                appendLine("TTS Option Explicit inspection audit")
                appendLine("Files checked: ${OPTION_EXPLICIT_PATHS.size}")
                appendLine("Warnings: ${warnings.size}")
                warnings.forEach { warning ->
                    appendLine("${warning.path}:${warning.line}\t${warning.description}")
                }
            },
            StandardCharsets.UTF_8
        )
        println("TTS Option Explicit inspection audit: warnings=${warnings.size}, report=$reportPath")
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

    private data class Warning(val path: String, val line: Int, val description: String)

    companion object {
        private const val TTS_PROJECT_DIR_PROPERTY = "tts.project.dir"
        private const val UNDECLARED_PREFIX = "Undeclared identifier:"
        private val OPTION_EXPLICIT_PATHS = listOf(
            "500-100.asp",
            "inc/500-100.asp",
            "inc/500-101.asp",
            "inc/500-102.asp",
            "loyalty/default.asp"
        )
        private val SUPPORT_PATHS = listOf(
            "inc/logger.asp",
            "inc/sendMail2.asp"
        )
        private val WINDOWS_1251 = charset("windows-1251").newDecoder()
    }
}
