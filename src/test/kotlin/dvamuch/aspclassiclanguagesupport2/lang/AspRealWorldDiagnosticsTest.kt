package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
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
            val hosts = PsiTreeUtil.collectElementsOfType(aspPsi, AspOuterPsiElement::class.java)
            val manager = InjectedLanguageManager.getInstance(project)
            val injected = linkedSetOf<PsiFile>()
            hosts.forEach { host ->
                val start = host.textRange.startOffset
                val end = host.textRange.endOffset
                for (offset in listOf(start + 2, start + 3, start + 4)) {
                    if (offset >= end) continue
                    val element = manager.findInjectedElementAt(root, offset) ?: continue
                    element.containingFile?.let { injected.add(it) }
                    break
                }
            }

            val errors = injected.flatMap { PsiTreeUtil.collectElementsOfType(it, PsiErrorElement::class.java) }
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
