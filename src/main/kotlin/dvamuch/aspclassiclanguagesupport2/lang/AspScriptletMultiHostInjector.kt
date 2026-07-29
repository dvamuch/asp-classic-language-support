package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

class AspScriptletMultiHostInjector : MultiHostInjector {
    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(AspOuterPsiElement::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val contextHost = context as? AspOuterPsiElement ?: return
        if (aspScriptletInfo(contextHost) == null) return
        val templatePsi = contextHost.containingFile

        var injecting = false
        val traverser = SyntaxTraverser.psiTraverser(templatePsi)
        for (element in traverser) {
            val aspHost = element as? AspOuterPsiElement ?: continue
            val info = aspScriptletInfo(aspHost) ?: continue
            if (!injecting) {
                registrar.startInjecting(VbScriptLanguage)
                injecting = true
            }
            registrar.addPlace(info.prefix, "\n", aspHost, info.range)
        }
        if (injecting) {
            registrar.doneInjecting()
        }
    }
}

internal data class AspScriptletInfo(val range: TextRange, val prefix: String?)

internal fun aspScriptletInfo(host: AspOuterPsiElement): AspScriptletInfo? {
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
    return AspScriptletInfo(TextRange(start, end), prefix)
}
