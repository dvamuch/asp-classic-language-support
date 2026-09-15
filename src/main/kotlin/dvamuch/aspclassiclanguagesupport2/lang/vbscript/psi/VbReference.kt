package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.openapi.util.TextRange

class VbReference(element: VbId) : PsiReferenceBase<VbId>(element, TextRange(0, element.textLength), false) {
    override fun resolve(): PsiElement? {
        return VbResolveUtil.resolve(myElement)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
