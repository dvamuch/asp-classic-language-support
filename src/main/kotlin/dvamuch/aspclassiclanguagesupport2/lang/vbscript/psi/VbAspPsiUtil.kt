package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.injected.editor.DocumentWindow
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.AspOuterPsiElement
import dvamuch.aspclassiclanguagesupport2.lang.aspScriptletInfo
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeGraph

internal object VbAspPsiUtil {
    data class HostLocation(val fileUrl: String, val offset: Int)

    fun aspPsi(file: PsiFile): PsiFile? = AspIncludeGraph.aspPsi(file)

    fun hostLocation(element: PsiElement): HostLocation? {
        val containingFile = element.containingFile ?: return null
        val manager = InjectedLanguageManager.getInstance(element.project)
        val topLevelFile = manager.getTopLevelFile(element)
        val virtualFile = topLevelFile.virtualFile ?: return null
        val document = PsiDocumentManager.getInstance(element.project).getDocument(containingFile)
        val hostOffset = if (document is DocumentWindow) {
            document.injectedToHost(element.textOffset)
        } else {
            element.textOffset
        }
        return HostLocation(virtualFile.url, hostOffset)
    }

    fun injectedVbScriptFile(aspFile: PsiFile): PsiFile? {
        val manager = InjectedLanguageManager.getInstance(aspFile.project)
        val hosts = PsiTreeUtil.collectElementsOfType(aspFile, AspOuterPsiElement::class.java)
            .sortedBy { it.textOffset }
        for (host in hosts) {
            val info = aspScriptletInfo(host) ?: continue
            val hostOffset = host.textRange.startOffset + info.range.startOffset
            val injected = manager.findInjectedElementAt(aspFile, hostOffset) ?: continue
            return injected.containingFile
        }
        return null
    }
}
