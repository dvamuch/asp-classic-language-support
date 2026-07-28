package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import com.intellij.psi.util.PsiTreeUtil

class VbReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val id = if (element is VbId) element
        else PsiTreeUtil.getParentOfType(element, VbId::class.java, false)
            ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(VbLeafReference(element, id))
    }

    private class VbLeafReference(
        element: PsiElement,
        private val id: VbId
    ) : com.intellij.psi.PsiReferenceBase<PsiElement>(element, TextRange(0, element.textLength), false) {
        override fun resolve(): PsiElement? = VbResolveUtil.resolve(id)
    }
}
