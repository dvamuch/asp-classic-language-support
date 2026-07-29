package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean {
        val id = element as? VbId ?: return false
        return VbDeclarationUtil.declaration(id) != null
    }

    override fun createFindUsagesHandler(
        element: PsiElement,
        forHighlightUsages: Boolean
    ): FindUsagesHandler = VbFindUsagesHandler(element)
}

private class VbFindUsagesHandler(element: PsiElement) : FindUsagesHandler(element)
