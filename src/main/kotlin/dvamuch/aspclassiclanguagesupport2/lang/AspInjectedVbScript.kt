package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

internal object AspInjectedVbScript {
    fun findFile(root: PsiElement): PsiFile? {
        val aspFile = root.containingFile?.viewProvider?.getPsi(AspLanguage) ?: return null
        return AspVbScriptContext.getForAspFile(aspFile).analysisFile
    }
}
