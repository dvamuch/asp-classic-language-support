package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.streams.asSequence
import kotlin.system.measureTimeMillis

class AspLargeFileSmokeTest : BasePlatformTestCase() {
    fun testParsesLargeSyntheticFileAndBuildsInjection() {
        val text = AspTestData.read("large/large_mixed.asp")
        val elapsedMs = measureTimeMillis {
            val file = myFixture.configureByText(AspFileType, text)
            assertInjectedVbscriptIsParseable(file, requireCrossScriptletResolve = true)
        }
        assertTrue("Large synthetic file should parse quickly enough for smoke checks", elapsedMs < 15_000)
    }

    fun testOptionalRealWorldAspFixtures() {
        val files = listAspFixtures(AspTestData.realWorldDir())
        if (files.isEmpty()) return

        files.forEach { path ->
            val text = Files.readString(path, StandardCharsets.UTF_8)
            val file = myFixture.configureByText(AspFileType, text)
            assertInjectedVbscriptIsParseable(file, requireCrossScriptletResolve = false)
        }
    }

    private fun assertInjectedVbscriptIsParseable(psiFile: PsiFile, requireCrossScriptletResolve: Boolean) {
        val aspPsi = psiFile.viewProvider.getPsi(AspLanguage)
        assertNotNull("ASP PSI should exist in view provider", aspPsi)
        val hosts = PsiTreeUtil.collectElementsOfType(aspPsi, AspOuterPsiElement::class.java)
        assertTrue("Expected at least one ASP scriptlet block", hosts.isNotEmpty())

        val manager = InjectedLanguageManager.getInstance(project)
        val injectedFiles = linkedSetOf<PsiFile>()

        hosts.forEach { host ->
            val start = host.textRange.startOffset
            val end = host.textRange.endOffset
            for (offset in listOf(start + 2, start + 3, start + 4)) {
                if (offset >= end) continue
                val injected = manager.findInjectedElementAt(psiFile, offset) ?: continue
                injected.containingFile?.let { injectedFiles.add(it) }
                break
            }
        }
        assertTrue("Expected injected VBScript PSI for ASP scriptlets", injectedFiles.isNotEmpty())

        injectedFiles.forEach { vbFile ->
            val parseErrors = PsiTreeUtil.collectElementsOfType(vbFile, PsiErrorElement::class.java)
            assertTrue(
                "Injected VBScript should not degrade into massive parse failure in ${vbFile.name}",
                parseErrors.size < 500
            )
        }

        if (requireCrossScriptletResolve) {
            val usagePrefix = "Response.Write \"<p>user=\" & userName & \"</p>\""
            val usageLineOffset = psiFile.text.indexOf(usagePrefix)
            assertTrue("Large fixture must contain userName usage line", usageLineOffset >= 0)
            val idOffset = psiFile.text.indexOf("userName", usageLineOffset)
            assertTrue("Large fixture must contain userName token in usage line", idOffset >= 0)
            val injectedAtUsage = manager.findInjectedElementAt(psiFile, idOffset)
            assertNotNull("Injected element should exist at userName usage offset", injectedAtUsage)

            val id = PsiTreeUtil.getParentOfType(injectedAtUsage, VbId::class.java, false)
            if (id != null) {
                id.references.firstOrNull()?.resolve()
            }
        }
    }

    private fun listAspFixtures(dir: Path): List<Path> {
        if (!Files.exists(dir)) return emptyList()
        Files.list(dir).use { stream ->
            return stream.asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.extension.equals("asp", ignoreCase = true) || it.extension.equals("inc", ignoreCase = true) }
                .sortedBy { it.fileName.toString() }
                .toList()
        }
    }
}
