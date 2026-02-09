package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.openapi.diagnostic.Logger
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage
import java.util.concurrent.ConcurrentHashMap

class AspScriptletInjector : LanguageInjector {
    private val logger = Logger.getInstance(AspScriptletInjector::class.java)
    private val loggedHosts = ConcurrentHashMap.newKeySet<String>()

    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, injectionPlacesRegistrar: InjectedLanguagePlaces) {
        val aspHost = host as? AspOuterPsiElement ?: return
        val text = aspHost.text
        val range = scriptletRange(text) ?: return

        injectionPlacesRegistrar.addPlace(VbScriptLanguage, range, null, null)
        logOnce(aspHost, range)
    }

    private fun scriptletRange(text: String): TextRange? {
        if (!text.startsWith("<%") || !text.endsWith("%>")) return null
        if (text.startsWith("<%--")) return null
        if (text.startsWith("<%@")) return null

        val start = if (text.startsWith("<%=")) 3 else 2
        val end = text.length - 2
        if (start >= end) return null
        return TextRange(start, end)
    }

    private fun logOnce(host: AspOuterPsiElement, range: TextRange) {
        val file = host.containingFile ?: return
        val key = file.virtualFile?.path + ":" + host.textOffset
        if (!loggedHosts.add(key)) return
        logger.warn(
            "ASP inject debug: file=${file.virtualFile?.path} hostRange=${host.textRange} " +
                "injectRange=$range hostText='${host.text}'"
        )
    }
}
