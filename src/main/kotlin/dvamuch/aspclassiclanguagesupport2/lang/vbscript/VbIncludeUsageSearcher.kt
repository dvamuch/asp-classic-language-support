package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import dvamuch.aspclassiclanguagesupport2.lang.AspFileType
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeGraph
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbAspPsiUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement

internal object VbIncludeUsageSearcher {
    fun find(element: VbId, userScope: SearchScope): List<UsageInfo> {
        val usages = mutableListOf<UsageInfo>()
        process(element, userScope, Processor {
            usages.add(it)
            true
        })
        return usages
    }

    fun process(
        element: VbId,
        userScope: SearchScope,
        processor: Processor<in UsageInfo>
    ): Boolean {
        if (userScope !is GlobalSearchScope) return true
        val context = ReadAction.compute<SearchContext?, RuntimeException> {
            prepareInReadAction(element, userScope)
        } ?: return true

        for (consumerFile in context.candidateFiles) {
            ProgressManager.checkCanceled()
            val completed = ReadAction.compute<Boolean, RuntimeException> {
                processFileInReadAction(consumerFile, context, processor)
            }
            if (!completed) return false
        }
        return true
    }

    private data class SearchContext(
        val project: Project,
        val name: String,
        val declarationLocation: VbAspPsiUtil.HostLocation,
        val declarationAspUrl: String,
        val candidateFiles: List<VirtualFile>,
        val includedDeclarationCache: MutableMap<String, Boolean> = mutableMapOf()
    )

    private fun prepareInReadAction(
        declarationId: VbId,
        userScope: GlobalSearchScope
    ): SearchContext? {
        val declaration = VbDeclarationUtil.declaration(declarationId) ?: return null
        if (declaration.scope !is PsiFile) return null

        val name = (declarationId as? VbNamedElement)?.name ?: return null
        val declarationLocation = VbAspPsiUtil.hostLocation(declarationId) ?: return null
        val injectionManager = InjectedLanguageManager.getInstance(declarationId.project)
        val declarationAspFile = VbAspPsiUtil.aspPsi(
            injectionManager.getTopLevelFile(declarationId)
        ) ?: return null
        val consumers = AspIncludeGraph.transitiveConsumers(declarationAspFile, userScope)
        if (consumers.isEmpty()) return null

        val consumerScope = userScope.intersectWith(
            GlobalSearchScope.filesScope(
                declarationId.project,
                consumers.map { it.virtualFile }
            )
        )
        return SearchContext(
            declarationId.project,
            name,
            declarationLocation,
            declarationAspFile.virtualFile.url,
            candidateFiles(name, consumerScope)
        )
    }

    private fun processFileInReadAction(
        consumerFile: VirtualFile,
        context: SearchContext,
        processor: Processor<in UsageInfo>
    ): Boolean {
        val consumerAspFile = PsiManager.getInstance(context.project).findFile(consumerFile)
            ?.let(VbAspPsiUtil::aspPsi) ?: return true
        if (!targetIsFirstIncludedDeclaration(consumerAspFile, context)) return true
        val scan = VbAspIdentifierScanner.scan(consumerAspFile, context.name)
        if (!scan.requiresPsiResolution) {
            for (range in scan.ranges) {
                if (!processor.process(UsageInfo(consumerAspFile, range.startOffset, range.endOffset, false))) {
                    return false
                }
            }
            return true
        }

        val vbFile = VbAspPsiUtil.injectedVbScriptFile(consumerAspFile) ?: return true
        for (candidateId in PsiTreeUtil.collectElementsOfType(vbFile, VbId::class.java)) {
            ProgressManager.checkCanceled()
            if (!(candidateId as? VbNamedElement)?.name.equals(context.name, ignoreCase = true)) continue
            val resolved = candidateId.reference?.resolve() ?: continue
            if (VbAspPsiUtil.hostLocation(resolved) != context.declarationLocation) continue
            if (!processor.process(VbHostUsageInfo.from(UsageInfo(candidateId)))) return false
        }
        return true
    }

    private fun targetIsFirstIncludedDeclaration(
        consumerAspFile: PsiFile,
        context: SearchContext
    ): Boolean {
        val visited = mutableSetOf(consumerAspFile.virtualFile.url)

        fun visit(file: PsiFile): Boolean? {
            for (includedFile in AspIncludeGraph.directIncludes(file)) {
                val url = includedFile.virtualFile.url
                if (!visited.add(url)) continue
                if (url == context.declarationAspUrl) return true
                val declaresName = context.includedDeclarationCache.getOrPut(url) {
                    includedFile.text.contains(context.name, ignoreCase = true) &&
                        VbAspIdentifierScanner.scan(includedFile, context.name).requiresPsiResolution
                }
                if (declaresName) return false
                visit(includedFile)?.let { return it }
            }
            return null
        }

        return visit(consumerAspFile) == true
    }

    private fun candidateFiles(name: String, searchScope: GlobalSearchScope): List<VirtualFile> {
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
        return virtualFiles.toList()
    }
}
