package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(VbId::class.java),
            VbReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(VbTypes.IDENTIFIER),
            VbReferenceProvider()
        )
    }
}
