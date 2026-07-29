package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlComment
import dvamuch.aspclassiclanguagesupport2.lang.AspFileType
import dvamuch.aspclassiclanguagesupport2.lang.AspLanguage

object AspIncludeGraph {
    fun directIncludes(aspFile: PsiFile): List<PsiFile> {
        val htmlPsi = aspFile.viewProvider.getPsi(HTMLLanguage.INSTANCE) ?: return emptyList()
        return PsiTreeUtil.collectElementsOfType(htmlPsi, XmlComment::class.java)
            .sortedBy { it.textOffset }
            .mapNotNull { comment ->
                comment.references
                    .filterIsInstance<AspIncludeReference>()
                    .firstOrNull()
                    ?.resolve()
                    ?.containingFile
                    ?.let(::aspPsi)
            }
    }

    fun transitiveConsumers(targetAspFile: PsiFile): List<PsiFile> {
        return CachedValuesManager.getCachedValue(targetAspFile) {
            CachedValueProvider.Result.create(
                buildTransitiveConsumers(targetAspFile),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    fun aspPsi(file: PsiFile): PsiFile? = file.viewProvider.getPsi(AspLanguage)

    private fun buildTransitiveConsumers(targetAspFile: PsiFile): List<PsiFile> {
        val project = targetAspFile.project
        val psiManager = PsiManager.getInstance(project)
        val projectScope = GlobalSearchScope.projectScope(project)
        val allAspFiles = FileTypeIndex.getFiles(AspFileType, projectScope)
            .mapNotNull(psiManager::findFile)
            .mapNotNull(::aspPsi)

        val reverseEdges = mutableMapOf<String, MutableList<PsiFile>>()
        for (sourceFile in allAspFiles) {
            ProgressManager.checkCanceled()
            for (includedFile in directIncludes(sourceFile)) {
                reverseEdges.getOrPut(fileKey(includedFile), ::mutableListOf).add(sourceFile)
            }
        }

        val visited = mutableSetOf(fileKey(targetAspFile))
        val queue = ArrayDeque<PsiFile>()
        val result = mutableListOf<PsiFile>()
        queue.add(targetAspFile)

        while (queue.isNotEmpty()) {
            ProgressManager.checkCanceled()
            val includedFile = queue.removeFirst()
            for (consumer in reverseEdges[fileKey(includedFile)].orEmpty()) {
                if (!visited.add(fileKey(consumer))) continue
                result.add(consumer)
                queue.add(consumer)
            }
        }
        return result
    }

    private fun fileKey(file: PsiFile): String = file.virtualFile.url
}
