package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbReferenceContributor : PsiReferenceContributor() {
    private val logger = Logger.getInstance(VbReferenceContributor::class.java)

    init {
        logger.warn("VBScript ref debug: VbReferenceContributor loaded")
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        logger.warn("VBScript ref contributor registered")
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
