package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import dvamuch.aspclassiclanguagesupport2.lang.AspFileType
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeGraph
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbAspPsiUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement

internal object VbIncludeUsageSearcher {
    fun find(element: VbId, userScope: SearchScope): List<UsageInfo> {
        if (userScope !is GlobalSearchScope) return emptyList()
        return ReadAction.compute<List<UsageInfo>, RuntimeException> {
            findInReadAction(element, userScope)
        }
    }

    private fun findInReadAction(
        declarationId: VbId,
        userScope: GlobalSearchScope
    ): List<UsageInfo> {
        val declaration = VbDeclarationUtil.declaration(declarationId) ?: return emptyList()
        if (declaration.scope !is PsiFile) return emptyList()

        val name = (declarationId as? VbNamedElement)?.name ?: return emptyList()
        val declarationLocation = VbAspPsiUtil.hostLocation(declarationId) ?: return emptyList()
        val injectionManager = InjectedLanguageManager.getInstance(declarationId.project)
        val declarationAspFile = VbAspPsiUtil.aspPsi(
            injectionManager.getTopLevelFile(declarationId)
        ) ?: return emptyList()
        val consumers = AspIncludeGraph.transitiveConsumers(declarationAspFile, userScope)
        if (consumers.isEmpty()) return emptyList()

        val consumerScope = userScope.intersectWith(
            GlobalSearchScope.filesScope(
                declarationId.project,
                consumers.map { it.virtualFile }
            )
        )
        val candidateFiles = candidateFiles(name, consumerScope)
        val usages = mutableListOf<UsageInfo>()
        for (consumerAspFile in candidateFiles) {
            ProgressManager.checkCanceled()
            val vbFile = VbAspPsiUtil.injectedVbScriptFile(consumerAspFile) ?: continue
            for (candidateId in PsiTreeUtil.collectElementsOfType(vbFile, VbId::class.java)) {
                ProgressManager.checkCanceled()
                if (!(candidateId as? VbNamedElement)?.name.equals(name, ignoreCase = true)) continue
                val resolved = candidateId.reference?.resolve() ?: continue
                if (VbAspPsiUtil.hostLocation(resolved) != declarationLocation) continue
                usages.add(VbHostUsageInfo.from(UsageInfo(candidateId)))
            }
        }
        return usages
    }

    private fun candidateFiles(name: String, searchScope: GlobalSearchScope): List<PsiFile> {
        val project = searchScope.project ?: return emptyList()
        val virtualFiles = linkedSetOf<VirtualFile>()
        PsiSearchHelper.getInstance(project).processCandidateFilesForText(
            searchScope,
            UsageSearchContext.ANY,
            false,
            name
        ) { virtualFile ->
            ProgressManager.checkCanceled()
            if (virtualFile.fileType == AspFileType) virtualFiles.add(virtualFile)
            true
        }
        val psiManager = PsiManager.getInstance(project)
        return virtualFiles
            .mapNotNull(psiManager::findFile)
            .mapNotNull(VbAspPsiUtil::aspPsi)
    }
}
