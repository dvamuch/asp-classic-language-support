package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.lang.html.HTMLLanguage
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.templateLanguages.OuterLanguageElementImpl
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage
import java.util.concurrent.ConcurrentHashMap

class AspScriptletMultiHostInjector : MultiHostInjector {
    private val logger = Logger.getInstance(AspScriptletMultiHostInjector::class.java)

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(PsiFile::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val psiFile = context as? PsiFile ?: return
        if (!psiFile.language.isKindOf(HTMLLanguage.INSTANCE)) return
        if (psiFile.viewProvider !is AspFileViewProvider) return

        var hostCount = 0
        var injectedCount = 0
        var injecting = false
        val traverser = SyntaxTraverser.psiTraverser(psiFile)
        for (element in traverser) {
            val aspHost = element as? AspOuterPsiElement ?: continue
            hostCount++
            val info = scriptletInfo(aspHost) ?: continue
            if (!injecting) {
                registrar.startInjecting(VbScriptLanguage)
                injecting = true
            }
            registrar.addPlace(info.prefix, "\n", aspHost, info.range)
            injectedCount++
        }
        if (hostCount == 0) {
            return
        }
        if (injecting) {
            registrar.doneInjecting()
        }
    }

    private data class ScriptletInfo(val range: TextRange, val prefix: String?)

    private fun scriptletInfo(host: AspOuterPsiElement): ScriptletInfo? {
        val text = host.node.chars
        val length = text.length
        if (length < 4) return null
        if (text[0] != '<' || text[1] != '%') return null
        if (text[length - 2] != '%' || text[length - 1] != '>') return null

        val third = text[2]
        if (third == '-' && length > 3 && text[3] == '-') return null
        if (third == '@') return null

        val isExpression = third == '='
        val start = if (isExpression) 3 else 2
        val end = length - 2
        if (start >= end) return null
        val prefix = if (isExpression) "Response.Write " else null
        return ScriptletInfo(TextRange(start, end), prefix)
    }
}
