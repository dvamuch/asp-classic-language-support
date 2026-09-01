package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable

/**
 * One lightweight analysis PSI for all scriptlets in an ASP file.
 *
 * Editor injection remains one-to-one with a scriptlet. This context provides
 * the combined VBScript parse/symbol space without attaching N copies of an
 * N-host injection to the editor document.
 */
internal class AspVbScriptContext private constructor(
    val aspFile: PsiFile,
    val analysisFile: PsiFile,
    private val segments: List<Segment>
) {
    fun hostToAnalysis(hostOffset: Int): Int? {
        val segment = segments.binarySearchBy(hostOffset) { it.hostRange.startOffset }
            .let { index -> if (index >= 0) index else -index - 2 }
        val match = segments.getOrNull(segment) ?: return null
        if (hostOffset !in match.hostRange.startOffset until match.hostRange.endOffset) return null
        return match.analysisRange.startOffset + hostOffset - match.hostRange.startOffset
    }

    fun analysisToHost(analysisOffset: Int): Int? {
        val segment = segments.binarySearchBy(analysisOffset) { it.analysisRange.startOffset }
            .let { index -> if (index >= 0) index else -index - 2 }
        val match = segments.getOrNull(segment) ?: return null
        if (analysisOffset !in match.analysisRange.startOffset until match.analysisRange.endOffset) return null
        return match.hostRange.startOffset + analysisOffset - match.analysisRange.startOffset
    }

    fun analysisRangeToHost(range: TextRange): TextRange? {
        if (range.isEmpty) return null
        val start = analysisToHost(range.startOffset) ?: return null
        val end = analysisToHost(range.endOffset - 1)?.plus(1) ?: return null
        return TextRange(start, end)
    }

    fun analysisSpanToHost(range: TextRange): TextRange? {
        if (range.isEmpty) return null
        val start = analysisToHost(range.startOffset) ?: nextMappedHostOffset(range.startOffset) ?: return null
        val end = analysisToHost(range.endOffset - 1)?.plus(1)
            ?: previousMappedHostOffset(range.endOffset - 1)?.plus(1)
            ?: return null
        if (end <= start) return null
        return TextRange(start, end)
    }

    private fun nextMappedHostOffset(analysisOffset: Int): Int? {
        return segments.firstOrNull { it.analysisRange.startOffset >= analysisOffset }?.hostRange?.startOffset
    }

    private fun previousMappedHostOffset(analysisOffset: Int): Int? {
        return segments.lastOrNull { it.analysisRange.endOffset <= analysisOffset + 1 }?.hostRange?.endOffset?.minus(1)
    }

    fun analysisIdAtHostOffset(hostOffset: Int): VbId? {
        val analysisOffset = hostToAnalysis(hostOffset) ?: return null
        val leaf = analysisFile.findElementAt(analysisOffset) ?: return null
        return PsiTreeUtil.getParentOfType(leaf, VbId::class.java, false)
    }

    fun hostIdForAnalysis(id: VbId): VbId? {
        val hostOffset = analysisToHost(id.textOffset) ?: return null
        val topLevel = aspFile.viewProvider.getPsi(aspFile.viewProvider.baseLanguage) ?: aspFile
        val injected = InjectedLanguageManager.getInstance(aspFile.project)
            .findInjectedElementAt(topLevel, hostOffset)
            ?: return null
        return PsiTreeUtil.getParentOfType(injected, VbId::class.java, false)
    }

    fun hostDeclarationIds(): List<VbId> {
        return VbFileSymbolTable.get(analysisFile).declarations()
            .mapNotNull { declaration -> hostIdForAnalysis(declaration.id) }
    }

    companion object {
        fun get(element: PsiElement): AspVbScriptContext? {
            val manager = InjectedLanguageManager.getInstance(element.project)
            val topLevel = manager.getTopLevelFile(element)
            val aspFile = topLevel.viewProvider.getPsi(AspLanguage) ?: return null
            return getForAspFile(aspFile)
        }

        fun getForAspFile(aspFile: PsiFile): AspVbScriptContext {
            return CachedValuesManager.getCachedValue(aspFile) {
                CachedValueProvider.Result.create(build(aspFile), aspFile)
            }
        }

        fun forAnalysisFile(file: PsiFile): AspVbScriptContext? = file.getUserData(CONTEXT_KEY)

        private fun build(aspFile: PsiFile): AspVbScriptContext {
            val segments = mutableListOf<Segment>()
            val source = buildString {
                PsiTreeUtil.collectElementsOfType(aspFile, AspOuterPsiElement::class.java)
                    .sortedBy { it.textRange.startOffset }
                    .forEach { host ->
                        val info = aspScriptletInfo(host) ?: return@forEach
                        val hostRange = info.range.shiftRight(host.textRange.startOffset)
                        append(info.prefix.orEmpty())
                        val analysisStart = length
                        append(info.range.substring(host.text))
                        val analysisEnd = length
                        segments += Segment(
                            hostRange = hostRange,
                            analysisRange = TextRange(analysisStart, analysisEnd)
                        )
                        append('\n')
                    }
            }
            val analysisFile = PsiFileFactory.getInstance(aspFile.project).createFileFromText(
                "${aspFile.name}.combined.vbs",
                VbScriptFileType,
                source
            )
            return AspVbScriptContext(aspFile, analysisFile, segments).also { context ->
                analysisFile.putUserData(CONTEXT_KEY, context)
            }
        }

        private val CONTEXT_KEY = Key.create<AspVbScriptContext>("asp.combined.vbscript.context")
    }

    private data class Segment(
        val hostRange: TextRange,
        val analysisRange: TextRange
    )
}
