package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import dvamuch.aspclassiclanguagesupport2.lang.AspFileType
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeGraph
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbAspPsiUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement

class VbReferencesSearchExecutor : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ): Boolean = ReadAction.compute<Boolean, RuntimeException> {
        executeInReadAction(queryParameters, consumer)
    }

    private fun executeInReadAction(
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
        val candidateFiles = VbUsageCandidateFiles.find(
            declarationId.project,
            name,
            queryParameters.scopeDeterminedByUser
        )

        val visibleConsumerFiles = VbUsageCandidateFiles.visibleConsumers(
            declarationAspFile,
            candidateFiles
        )
        for (consumerAspFile in visibleConsumerFiles) {
            ProgressManager.checkCanceled()
            if (consumerAspFile.virtualFile == declarationAspFile.virtualFile) continue
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

internal object VbUsageCandidateFiles {
    fun find(project: Project, name: String, searchScope: SearchScope): List<PsiFile> {
        val virtualFiles = linkedSetOf<VirtualFile>()
        val injectionManager = InjectedLanguageManager.getInstance(project)
        val psiManager = PsiManager.getInstance(project)
        val searchHelper = PsiSearchHelper.getInstance(project)

        when (searchScope) {
            is GlobalSearchScope -> searchHelper.processCandidateFilesForText(
                searchScope,
                UsageSearchContext.ANY,
                false,
                name
            ) { virtualFile ->
                ProgressManager.checkCanceled()
                if (virtualFile.fileType == AspFileType) virtualFiles.add(virtualFile)
                true
            }

            is LocalSearchScope -> searchScope.scope.forEach { element ->
                ProgressManager.checkCanceled()
                val virtualFile = injectionManager.getTopLevelFile(element).virtualFile
                if (virtualFile != null && virtualFile.fileType == AspFileType) {
                    virtualFiles.add(virtualFile)
                }
            }

            else -> FileTypeIndex.getFiles(AspFileType, GlobalSearchScope.projectScope(project))
                .asSequence()
                .filter(searchScope::contains)
                .filter { virtualFile ->
                    ProgressManager.checkCanceled()
                    psiManager.findFile(virtualFile)?.let { searchHelper.hasIdentifierInFile(it, name) } == true
                }
                .forEach(virtualFiles::add)
        }

        return virtualFiles
            .mapNotNull(psiManager::findFile)
            .mapNotNull(VbAspPsiUtil::aspPsi)
    }

    fun visibleConsumers(declarationAspFile: PsiFile, candidates: List<PsiFile>): List<PsiFile> {
        val declarationUrl = declarationAspFile.virtualFile.url
        val knownConsumers = mutableSetOf<String>()

        fun includesDeclaration(aspFile: PsiFile, visiting: MutableSet<String>): Boolean {
            ProgressManager.checkCanceled()
            val fileUrl = aspFile.virtualFile.url
            if (fileUrl == declarationUrl) return true
            if (fileUrl in knownConsumers) return true
            if (!visiting.add(fileUrl)) return false

            val result = AspIncludeGraph.directIncludes(aspFile).any { includedFile ->
                includesDeclaration(includedFile, visiting)
            }
            visiting.remove(fileUrl)
            if (result) knownConsumers.add(fileUrl)
            return result
        }

        return candidates.filter { candidate ->
            includesDeclaration(candidate, mutableSetOf())
        }
    }
}
