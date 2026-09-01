package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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
        return AspIncludeGraph.aspPsi(aspFile)
    }
}
