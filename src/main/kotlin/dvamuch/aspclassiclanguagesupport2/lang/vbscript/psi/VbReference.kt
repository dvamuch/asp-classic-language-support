package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VbReference(element: VbId) : PsiReferenceBase<VbId>(element, element.textRangeInParent, false) {
    override fun resolve(): PsiElement? {
        return VbResolveUtil.resolve(myElement)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
