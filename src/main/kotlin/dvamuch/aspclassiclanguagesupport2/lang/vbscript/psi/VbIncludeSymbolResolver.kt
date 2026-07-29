package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeGraph

internal object VbIncludeSymbolResolver {
    fun resolve(usage: VbId, name: String): PsiElement? {
        for (includedFile in includedVbScriptFiles(usage)) {
            val declarations = VbFileSymbolTable.get(includedFile)
                .declarations(name)
                .filter { it.scope is PsiFile }

            val explicit = declarations
                .filter { !it.implicit }
                .maxByOrNull { it.id.textOffset }
            if (explicit != null) return explicit.id

            val implicit = declarations
                .filter { it.implicit }
                .maxByOrNull { it.id.textOffset }
            if (implicit != null) return implicit.id
        }
        return null
    }

    private fun includedVbScriptFiles(usage: VbId): List<PsiFile> {
        val manager = InjectedLanguageManager.getInstance(usage.project)
        val topLevelFile = manager.getTopLevelFile(usage)
        val sourceAspFile = VbAspPsiUtil.aspPsi(topLevelFile) ?: return emptyList()
        return CachedValuesManager.getCachedValue(sourceAspFile) {
            CachedValueProvider.Result.create(
                buildIncludedVbScriptFiles(sourceAspFile),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    private fun buildIncludedVbScriptFiles(sourceAspFile: PsiFile): List<PsiFile> {
        val visited = mutableSetOf(fileKey(sourceAspFile))
        val result = mutableListOf<PsiFile>()

        fun visit(aspFile: PsiFile) {
            for (includedAspFile in AspIncludeGraph.directIncludes(aspFile)) {
                if (!visited.add(fileKey(includedAspFile))) continue
                VbAspPsiUtil.injectedVbScriptFile(includedAspFile)?.let(result::add)
                visit(includedAspFile)
            }
        }

        visit(sourceAspFile)
        return result
    }

    private fun fileKey(file: PsiFile): String = file.virtualFile.url
}
