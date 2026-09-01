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
        val text = buildLargeSyntheticAsp(repetitions = 750)
        assertTrue("Synthetic fixture must be large enough to exercise the pipeline", text.length >= 75_000)

        val elapsedMs = measureTimeMillis {
            val file = myFixture.configureByText(AspFileType, text)
            assertInjectedVbscriptIsParseable(file, requireCrossScriptletResolve = true)
        }
        assertTrue(
            "Large synthetic file should parse quickly enough for smoke checks: ${elapsedMs}ms",
            elapsedMs < 20_000
        )
    }

    fun testInjectionLookupIsLinearAcrossAllScriptlets() {
        val text = buildLargeSyntheticAsp(repetitions = 750)
        assertAllScriptletInjectionsAreResponsive("many-scriptlets.asp", text, 20_000)
    }

    fun testOptionalTtsOrderInfoInjectionPerformance() {
        val ttsRoot = System.getProperty("tts.project.dir") ?: return
        val path = Path.of(ttsRoot).resolve("customers/orderinfo.asp")
        if (!Files.isRegularFile(path)) return
        val text = Files.readString(path, StandardCharsets.UTF_8)
        assertAllScriptletInjectionsAreResponsive("orderinfo.asp", text, 30_000)
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

        sampleHosts(hosts).forEach { host ->
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

    private fun assertAllScriptletInjectionsAreResponsive(name: String, text: String, limitMs: Long) {
        val file = myFixture.configureByText(name, text)
        val aspPsi = file.viewProvider.getPsi(AspLanguage)
        assertNotNull("ASP PSI should exist in view provider", aspPsi)
        val hosts = PsiTreeUtil.collectElementsOfType(aspPsi, AspOuterPsiElement::class.java)
            .filter { aspScriptletInfo(it) != null }
            .sortedBy { it.textRange.startOffset }
        assertTrue("Fixture should contain many scriptlets", hosts.size >= 1_000)

        val manager = InjectedLanguageManager.getInstance(project)
        val injectedFiles = linkedSetOf<PsiFile>()
        val elapsedMs = measureTimeMillis {
            hosts.forEach { host ->
                val info = aspScriptletInfo(host) ?: return@forEach
                val offset = host.textRange.startOffset + info.range.startOffset
                val injected = manager.findInjectedElementAt(file, offset)
                assertNotNull("Expected VBScript injection at host offset $offset", injected)
                injected?.containingFile?.let(injectedFiles::add)
            }
        }

        assertEquals("Each scriptlet must own exactly one lightweight injection", hosts.size, injectedFiles.size)
        println("ASP injection performance: $name, hosts=${hosts.size}, elapsed=${elapsedMs}ms")
        assertTrue("All ${hosts.size} injection lookups took ${elapsedMs}ms", elapsedMs < limitMs)
    }

    private fun buildLargeSyntheticAsp(repetitions: Int): String = buildString {
        appendLine("<%@ Language=\"VBScript\" %>")
        appendLine("<%")
        appendLine("Option Explicit")
        appendLine("Dim total, userName")
        appendLine("total = 0")
        appendLine("userName = \"guest\"")
        appendLine("%>")
        appendLine("<!doctype html>")
        appendLine("<html><body>")

        repeat(repetitions) { index ->
            appendLine("<section class=\"legacy-row\" data-index=\"$index\">")
            appendLine("<% total = total + $index %>")
            appendLine("<span>Running total: <%= total %></span>")
            appendLine("</section>")
        }

        appendLine("<% Response.Write \"<p>user=\" & userName & \"</p>\" %>")
        appendLine("</body></html>")
    }

    private fun sampleHosts(hosts: Collection<AspOuterPsiElement>): List<AspOuterPsiElement> {
        val allHosts = hosts.toList()
        if (allHosts.size <= MAX_INJECTION_SAMPLES) return allHosts

        val lastIndex = allHosts.lastIndex
        return List(MAX_INJECTION_SAMPLES) { sampleIndex ->
            allHosts[sampleIndex * lastIndex / (MAX_INJECTION_SAMPLES - 1)]
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

    companion object {
        private const val MAX_INJECTION_SAMPLES = 32
    }
}
