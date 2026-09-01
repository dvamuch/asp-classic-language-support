package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class AspExternalProjectDiagnosticsTest : BasePlatformTestCase() {
    fun testOptionalBugsIndexAspHasNoInjectedParseErrors() {
        val indexPath = Path.of(
            System.getProperty("user.dir"),
            "src",
            "test",
            "testData",
            "asp",
            "external-project",
            "Bugs",
            "index.asp"
        )
        if (!Files.exists(indexPath)) return

        val text = Files.readString(indexPath, StandardCharsets.UTF_8)
        val root = myFixture.configureByText(AspFileType, text)
        val aspPsi = root.viewProvider.getPsi(AspLanguage) ?: return

        val analysisFile = AspVbScriptContext.getForAspFile(aspPsi).analysisFile
        val errors = PsiTreeUtil.collectElementsOfType(analysisFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return

        val report = StringBuilder()
        report.append("index.asp: ").append(errors.size).append(" errors").append('\n')
        errors.take(15).forEach { error ->
            val source = error.containingFile.text
            val range = error.textRange
            val from = (range.startOffset - 50).coerceAtLeast(0)
            val to = (range.endOffset + 50).coerceAtMost(source.length)
            val snippet = source.substring(from, to).replace('\n', ' ')
            val line = 1 + source.take(range.startOffset).count { it == '\n' }
            report.append(" - line ").append(line).append(": ")
                .append(error.errorDescription).append(" :: ").append(snippet).append('\n')
        }

        assertTrue("Injected parse errors in Bugs/index.asp:\n$report", false)
    }
}
