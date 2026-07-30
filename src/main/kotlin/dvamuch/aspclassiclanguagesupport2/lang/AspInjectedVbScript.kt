package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

internal object AspInjectedVbScript {
    fun findFile(root: PsiElement): PsiFile? {
        val firstScriptlet = PsiTreeUtil.collectElementsOfType(root, AspOuterPsiElement::class.java)
            .firstOrNull { host -> aspScriptletInfo(host) != null }
            ?: return null
        val manager = InjectedLanguageManager.getInstance(root.project)
        var result: PsiFile? = null
        manager.enumerate(firstScriptlet) { injectedFile, _ ->
            if (result == null && injectedFile.language == VbScriptLanguage) result = injectedFile
        }
        return result
    }
}
