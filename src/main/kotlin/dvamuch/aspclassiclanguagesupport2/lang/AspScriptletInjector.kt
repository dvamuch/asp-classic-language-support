package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

class AspScriptletInjector : LanguageInjector {
    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, injectionPlacesRegistrar: InjectedLanguagePlaces) {
        val aspHost = host as? AspOuterPsiElement ?: return
        val text = aspHost.text
        val range = scriptletRange(text) ?: return

        injectionPlacesRegistrar.addPlace(VbScriptLanguage, range, null, null)
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
}
