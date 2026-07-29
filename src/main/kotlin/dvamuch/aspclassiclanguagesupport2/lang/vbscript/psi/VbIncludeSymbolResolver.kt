package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlComment
import dvamuch.aspclassiclanguagesupport2.lang.AspLanguage
import dvamuch.aspclassiclanguagesupport2.lang.AspOuterPsiElement
import dvamuch.aspclassiclanguagesupport2.lang.aspScriptletInfo
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeReference

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
        val sourceAspFile = aspPsi(topLevelFile) ?: return emptyList()
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
            for (includedAspFile in directIncludes(aspFile)) {
                if (!visited.add(fileKey(includedAspFile))) continue
                injectedVbScriptFile(includedAspFile)?.let(result::add)
                visit(includedAspFile)
            }
        }

        visit(sourceAspFile)
        return result
    }

    private fun directIncludes(aspFile: PsiFile): List<PsiFile> {
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

    private fun injectedVbScriptFile(aspFile: PsiFile): PsiFile? {
        val manager = InjectedLanguageManager.getInstance(aspFile.project)
        val hosts = PsiTreeUtil.collectElementsOfType(aspFile, AspOuterPsiElement::class.java)
            .sortedBy { it.textOffset }
        for (host in hosts) {
            val info = aspScriptletInfo(host) ?: continue
            val hostOffset = host.textRange.startOffset + info.range.startOffset
            val injected = manager.findInjectedElementAt(aspFile, hostOffset) ?: continue
            return injected.containingFile
        }
        return null
    }

    private fun aspPsi(file: PsiFile): PsiFile? {
        return file.viewProvider.getPsi(AspLanguage)
    }

    private fun fileKey(file: PsiFile): String = file.virtualFile.url
}
