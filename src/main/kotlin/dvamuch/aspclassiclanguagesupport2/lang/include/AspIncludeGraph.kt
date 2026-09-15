package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlComment
import dvamuch.aspclassiclanguagesupport2.lang.AspLanguage

internal object AspIncludeGraph {
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

    fun transitiveConsumers(
        includedAspFile: PsiFile,
        searchScope: GlobalSearchScope
    ): List<PsiFile> {
        val consumers = linkedMapOf<String, PsiFile>()
        val visited = mutableSetOf(includedAspFile.virtualFile.url)
        val pending = ArrayDeque<PsiFile>()
        val psiManager = PsiManager.getInstance(includedAspFile.project)
        pending.add(includedAspFile)

        while (pending.isNotEmpty()) {
            ProgressManager.checkCanceled()
            val includedFile = pending.removeFirst()
            for (candidateFile in AspIncludeIndex.candidateConsumers(includedFile.virtualFile, searchScope)) {
                ProgressManager.checkCanceled()
                val consumer = psiManager.findFile(candidateFile)?.let(::aspPsi) ?: continue
                val includesTarget = directIncludes(consumer).any {
                    it.virtualFile.url == includedFile.virtualFile.url
                }
                if (!includesTarget) continue
                val consumerUrl = consumer.virtualFile.url
                if (visited.add(consumerUrl)) {
                    consumers[consumerUrl] = consumer
                    pending.add(consumer)
                }
            }
        }

        return consumers.values.toList()
    }

    fun aspPsi(file: PsiFile): PsiFile? = file.viewProvider.getPsi(AspLanguage)
}
