package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeGraph
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbAspPsiUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement

class VbReferencesSearchExecutor : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ): Boolean {
        val declarationId = queryParameters.elementToSearch as? VbId ?: return true
        val declaration = VbDeclarationUtil.declaration(declarationId) ?: return true
        if (declaration.scope !is PsiFile) return true

        val manager = InjectedLanguageManager.getInstance(declarationId.project)
        val declarationTopLevelFile = manager.getTopLevelFile(declarationId)
        val declarationAspFile = VbAspPsiUtil.aspPsi(declarationTopLevelFile) ?: return true
        val declarationLocation = VbAspPsiUtil.hostLocation(declarationId) ?: return true
        val name = (declarationId as? VbNamedElement)?.name ?: return true
        val searchScope = queryParameters.scopeDeterminedByUser

        for (consumerAspFile in AspIncludeGraph.transitiveConsumers(declarationAspFile)) {
            ProgressManager.checkCanceled()
            if (!searchScope.contains(consumerAspFile.virtualFile)) continue
            val vbFile = VbAspPsiUtil.injectedVbScriptFile(consumerAspFile) ?: continue
            val candidateIds = PsiTreeUtil.collectElementsOfType(vbFile, VbId::class.java)
            for (candidateId in candidateIds) {
                ProgressManager.checkCanceled()
                if (!(candidateId as? VbNamedElement)?.name.equals(name, ignoreCase = true)) continue
                val reference = candidateId.reference ?: continue
                val resolved = reference.resolve() ?: continue
                if (VbAspPsiUtil.hostLocation(resolved) != declarationLocation) continue
                if (!consumer.process(reference)) return false
            }
        }
        return true
    }
}
