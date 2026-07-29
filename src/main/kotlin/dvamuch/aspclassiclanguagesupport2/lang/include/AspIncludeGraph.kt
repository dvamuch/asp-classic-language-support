package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.lang.html.HTMLLanguage
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlComment
import dvamuch.aspclassiclanguagesupport2.lang.AspLanguage

object AspIncludeGraph {
    fun directIncludes(aspFile: PsiFile): List<PsiFile> {
        val htmlPsi = aspFile.viewProvider.getPsi(HTMLLanguage.INSTANCE) ?: return emptyList()
        return PsiTreeUtil.collectElementsOfType(htmlPsi, XmlComment::class.java)
            .sortedBy { it.textOffset }
            .mapNotNull { comment ->
                comment.references
                    .filterIsInstance<AspIncludeReference>()
                    .firstOrNull()
                    ?.resolve()
                    ?.containingFile
                    ?.let(::aspPsi)
            }
    }

    fun aspPsi(file: PsiFile): PsiFile? = file.viewProvider.getPsi(AspLanguage)
}
