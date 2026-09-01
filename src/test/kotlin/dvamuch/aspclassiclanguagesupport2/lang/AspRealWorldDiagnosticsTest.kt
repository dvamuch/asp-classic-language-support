package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.streams.asSequence

class AspRealWorldDiagnosticsTest : BasePlatformTestCase() {
    fun testRealWorldFilesHaveNoInjectedParseErrors() {
        val files = Files.list(AspTestData.realWorldDir()).use { stream ->
            stream.asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.extension.equals("asp", ignoreCase = true) || it.extension.equals("inc", ignoreCase = true) }
                .sortedBy { it.fileName.toString() }
                .toList()
        }
        if (files.isEmpty()) return

        val report = StringBuilder()
        files.forEach { path ->
            val text = Files.readString(path, StandardCharsets.UTF_8)
            val root = myFixture.configureByText(AspFileType, text)
            val aspPsi = root.viewProvider.getPsi(AspLanguage) ?: return@forEach
            val analysisFile = AspVbScriptContext.getForAspFile(aspPsi).analysisFile
            val errors = PsiTreeUtil.collectElementsOfType(analysisFile, PsiErrorElement::class.java)
            if (errors.isEmpty()) return@forEach

            report.append(path.fileName).append(": ").append(errors.size).append(" errors").append('\n')
            errors.take(12).forEach { error ->
                val textRange = error.textRange
                val source = error.containingFile.text
                val from = (textRange.startOffset - 40).coerceAtLeast(0)
                val to = (textRange.endOffset + 40).coerceAtMost(source.length)
                val snippet = source.substring(from, to).replace('\n', ' ')
                report.append("  - ").append(error.errorDescription).append(" :: ").append(snippet).append('\n')
            }
        }

        assertTrue("Injected parse errors detected:\n$report", report.isEmpty())
    }
}
