package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
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

private class VbFindUsagesHandler(element: PsiElement) : FindUsagesHandler(element) {
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Boolean {
        val declarationId = element as? VbId ?: return false
        val userScope = options.searchScope
        val fastTrack = options.fastTrack
        val localScope = ReadAction.compute<LocalSearchScope, RuntimeException> {
            LocalSearchScope(element.containingFile)
        }
        options.searchScope = localScope
        options.fastTrack = null
        val localCompleted = try {
            super.processElementUsages(
                element,
                Processor { usage -> processor.process(VbHostUsageInfo.from(usage)) },
                options
            )
        } finally {
            options.searchScope = userScope
            options.fastTrack = fastTrack
        }
        if (!localCompleted) return false

        for (usage in VbIncludeUsageSearcher.find(declarationId, userScope)) {
            ProgressManager.checkCanceled()
            if (!processor.process(usage)) return false
        }
        return true
    }
}
