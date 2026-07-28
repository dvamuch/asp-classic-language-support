package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlComment
import com.intellij.util.ProcessingContext
import dvamuch.aspclassiclanguagesupport2.lang.AspFileViewProvider

class AspIncludeReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlComment::class.java),
            AspIncludeReferenceProvider()
        )
    }
}

private class AspIncludeReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val comment = element as? XmlComment ?: return PsiReference.EMPTY_ARRAY
        if (comment.containingFile.viewProvider !is AspFileViewProvider) return PsiReference.EMPTY_ARRAY

        val directive = AspIncludeDirectiveParser.parse(comment.text) ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(AspIncludeReference(comment, directive))
    }
}
