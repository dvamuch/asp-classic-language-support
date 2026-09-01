package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

/**
 * Compatibility facade for features that previously consumed a synthetic,
 * concatenated VBScript file. ASP now owns a native file-wide VBScript PSI, so
 * all offsets and elements map directly to the host document.
 */
internal class AspVbScriptContext private constructor(val aspFile: PsiFile) {
    val analysisFile: PsiFile get() = aspFile

    fun hostToAnalysis(hostOffset: Int): Int? = hostOffset.takeIf { it in 0 until aspFile.textLength }
    fun analysisToHost(analysisOffset: Int): Int? = analysisOffset.takeIf { it in 0 until aspFile.textLength }
    fun analysisRangeToHost(range: TextRange): TextRange? = validRange(range)
    fun analysisSpanToHost(range: TextRange): TextRange? = validRange(range)

    fun analysisIdAtHostOffset(hostOffset: Int): VbId? {
        val leaf = aspFile.findElementAt(hostOffset) ?: return null
        return PsiTreeUtil.getParentOfType(leaf, VbId::class.java, false)
    }

    fun hostIdForAnalysis(id: VbId): VbId? = id.takeIf { it.containingFile == aspFile }

    fun hostDeclarationIds(): List<VbId> =
        VbFileSymbolTable.get(aspFile).declarations().map { it.id }

    private fun validRange(range: TextRange): TextRange? =
        range.takeIf { !it.isEmpty && it.startOffset >= 0 && it.endOffset <= aspFile.textLength }

    companion object {
        fun get(element: PsiElement): AspVbScriptContext? {
            val aspFile = element.containingFile?.viewProvider?.getPsi(AspLanguage) ?: return null
            return AspVbScriptContext(aspFile)
        }

        fun getForAspFile(aspFile: PsiFile): AspVbScriptContext =
            AspVbScriptContext(aspFile.viewProvider.getPsi(AspLanguage) ?: aspFile)

        fun forAnalysisFile(file: PsiFile): AspVbScriptContext? =
            file.viewProvider.getPsi(AspLanguage)?.let(::AspVbScriptContext)
    }
}
