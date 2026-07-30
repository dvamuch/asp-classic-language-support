package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.psi.PsiFile
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbStructureViewBuilder

class AspStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return VbStructureViewBuilder(psiFile) {
            val aspPsi = psiFile.viewProvider.getPsi(AspLanguage) ?: psiFile
            AspInjectedVbScript.findFile(aspPsi)
        }
    }
}
