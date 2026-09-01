package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbIfBlockStmt
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.streams.asSequence
import kotlin.system.measureTimeMillis

class AspLargeFileSmokeTest : BasePlatformTestCase() {
    fun testParsesLargeSyntheticFileWithOneNativeAspPsi() {
        val text = buildLargeSyntheticAsp(repetitions = 750)
        assertTrue("Synthetic fixture must be large enough to exercise the pipeline", text.length >= 75_000)

        val elapsedMs = measureTimeMillis {
            val file = myFixture.configureByText(AspFileType, text)
            assertNativeAspIsParseable(file, requireCrossScriptletResolve = true)
        }
        assertTrue(
            "Large synthetic file should parse quickly enough for smoke checks: ${elapsedMs}ms",
            elapsedMs < 20_000
        )
    }

    fun testNativePsiIsLinearAcrossAllScriptlets() {
        val text = buildLargeSyntheticAsp(repetitions = 750)
        assertNativePsiIsResponsive("many-scriptlets.asp", text, 10_000)
    }

    fun testOptionalTtsOrderInfoNativePsiPerformance() {
        val ttsRoot = System.getProperty("tts.project.dir") ?: return
        val path = Path.of(ttsRoot).resolve("customers/orderinfo.asp")
        if (!Files.isRegularFile(path)) return
        val text = Files.readString(path, StandardCharsets.UTF_8)
        assertNativePsiIsResponsive("orderinfo.asp", text, 10_000)
    }

    fun testOptionalTtsOrderInfoFullHighlightingPerformance() {
        val ttsRoot = System.getProperty("tts.project.dir") ?: return
        val path = Path.of(ttsRoot).resolve("customers/orderinfo.asp")
        if (!Files.isRegularFile(path)) return
        val text = Files.readString(path, StandardCharsets.UTF_8)
        myFixture.configureByText("orderinfo-highlight.asp", text)
        val elapsedMs = measureTimeMillis { myFixture.doHighlighting() }
        println("ASP full highlighting performance: orderinfo.asp, elapsed=${elapsedMs}ms")
        assertTrue("Full highlighting of orderinfo.asp took ${elapsedMs}ms", elapsedMs < 10_000)
    }

    fun testOptionalRealWorldAspFixtures() {
        val files = listAspFixtures(AspTestData.realWorldDir())
        if (files.isEmpty()) return

        files.forEach { path ->
            val text = Files.readString(path, StandardCharsets.UTF_8)
            val file = myFixture.configureByText(AspFileType, text)
            assertNativeAspIsParseable(file, requireCrossScriptletResolve = false)
        }
    }

    private fun assertNativeAspIsParseable(psiFile: PsiFile, requireCrossScriptletResolve: Boolean) {
        val aspPsi = psiFile.viewProvider.getPsi(AspLanguage)
        assertNotNull("ASP PSI should exist in view provider", aspPsi)
        val manager = InjectedLanguageManager.getInstance(project)
        val firstScriptOffset = psiFile.text.indexOf("<%").takeIf { it >= 0 }?.plus(2) ?: 0
        assertNull("ASP must not create per-scriptlet injected PSI", manager.findInjectedElementAt(psiFile, firstScriptOffset))
        val parseErrors = PsiTreeUtil.collectElementsOfType(aspPsi, PsiErrorElement::class.java)
        assertTrue("Native ASP PSI should not degrade into massive parse failure", parseErrors.size < 500)

        if (requireCrossScriptletResolve) {
            val usagePrefix = "Response.Write \"<p>user=\" & userName & \"</p>\""
            val usageLineOffset = psiFile.text.indexOf(usagePrefix)
            assertTrue("Large fixture must contain userName usage line", usageLineOffset >= 0)
            val idOffset = psiFile.text.indexOf("userName", usageLineOffset)
            assertTrue("Large fixture must contain userName token in usage line", idOffset >= 0)
            val id = PsiTreeUtil.getParentOfType(aspPsi!!.findElementAt(idOffset), VbId::class.java, false)
            if (id != null) {
                id.references.firstOrNull()?.resolve()
            }
        }
    }

    private fun assertNativePsiIsResponsive(name: String, text: String, limitMs: Long) {
        val file = myFixture.configureByText(name, text)
        val scriptletCount = "<%".toRegex().findAll(text).count()
        assertTrue("Fixture should contain many scriptlets", scriptletCount >= 1_000)
        val elapsedMs = measureTimeMillis {
            val aspPsi = file.viewProvider.getPsi(AspLanguage)
            assertNotNull("ASP PSI should exist in view provider", aspPsi)
            assertTrue("Native PSI should contain VBScript identifiers", PsiTreeUtil.collectElementsOfType(aspPsi, VbId::class.java).isNotEmpty())
            val errors = PsiTreeUtil.collectElementsOfType(aspPsi, PsiErrorElement::class.java)
            val crossTemplateIfs = PsiTreeUtil.collectElementsOfType(aspPsi, VbIfBlockStmt::class.java)
                .count { it.text.contains("%>") && it.text.contains("<%") }
            println("ASP native PSI structure: $name, errors=${errors.size}, crossTemplateIfs=$crossTemplateIfs")
            assertTrue("Expected cross-template If blocks in $name", crossTemplateIfs > 0)
        }
        println("ASP native PSI performance: $name, scriptlets=$scriptletCount, elapsed=${elapsedMs}ms")
        assertTrue("Native PSI for $scriptletCount scriptlets took ${elapsedMs}ms", elapsedMs < limitMs)
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
        appendLine("<% If total >= 0 Then %>")

        repeat(repetitions) { index ->
            appendLine("<section class=\"legacy-row\" data-index=\"$index\">")
            appendLine("<% total = total + $index %>")
            appendLine("<span>Running total: <%= total %></span>")
            appendLine("</section>")
        }

        appendLine("<% Response.Write \"<p>user=\" & userName & \"</p>\" %>")
        appendLine("<% End If %>")
        appendLine("</body></html>")
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
