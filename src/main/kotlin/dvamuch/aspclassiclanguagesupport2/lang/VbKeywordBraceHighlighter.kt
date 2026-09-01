package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.highlighting.HeavyBraceHighlighter
import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbKeywordPairMatcher
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

class VbKeywordBraceHighlighter : HeavyBraceHighlighter() {
    override fun isAvailable(file: PsiFile, offset: Int): Boolean {
        return file.language == VbScriptLanguage || file.viewProvider is AspFileViewProvider
    }

    override fun matchBrace(file: PsiFile, offset: Int): Pair<TextRange, TextRange>? {
        if (file.language == VbScriptLanguage) {
            val manager = InjectedLanguageManager.getInstance(file.project)
            if (manager.isInjectedFragment(file)) {
                val topLevel = manager.getTopLevelFile(file)
                if (topLevel.viewProvider is AspFileViewProvider) {
                    val hostPair = matchAsp(topLevel, offset) ?: return null
                    val window = PsiDocumentManager.getInstance(file.project).getDocument(file) as? DocumentWindow
                        ?: return null
                    val first = hostRangeToInjected(window, hostPair.first) ?: return null
                    val second = hostRangeToInjected(window, hostPair.second) ?: return null
                    return Pair.create(first, second)
                }
            }
            return matchVbScript(file, offset)
        }
        if (file.viewProvider !is AspFileViewProvider) return null

        return matchAsp(file, offset)
    }

    private fun hostRangeToInjected(window: DocumentWindow, range: TextRange): TextRange? {
        val start = window.hostToInjected(range.startOffset)
        val end = window.hostToInjected(range.endOffset - 1)
        if (start < 0 || end < 0) return null
        return TextRange(start, end + 1)
    }

    private fun matchAsp(file: PsiFile, offset: Int): Pair<TextRange, TextRange>? {
        val aspFile = file.viewProvider.getPsi(AspLanguage) ?: return null
        val context = AspVbScriptContext.getForAspFile(aspFile)
        val analysisOffset = sequenceOf(offset, offset - 1)
            .filter { candidate -> candidate >= 0 && candidate < file.textLength }
            .mapNotNull(context::hostToAnalysis)
            .firstOrNull()
            ?: return null
        val pair = VbKeywordPairMatcher.match(context.analysisFile, analysisOffset) ?: return null
        return Pair.create(
            context.analysisRangeToHost(pair.first) ?: return null,
            context.analysisRangeToHost(pair.second) ?: return null
        )
    }

    private fun matchVbScript(file: PsiFile, offset: Int): Pair<TextRange, TextRange>? {
        val manager = InjectedLanguageManager.getInstance(file.project)
        if (!manager.isInjectedFragment(file)) return VbKeywordPairMatcher.match(file, offset)

        // BackgroundHighlighter selects the injected PSI file, but passes the
        // caret offset in host-document coordinates to HeavyBraceHighlighter.
        val injectedDocument = PsiDocumentManager.getInstance(file.project).getDocument(file) as? DocumentWindow
            ?: return null
        val injectedOffset = sequenceOf(offset, offset - 1)
            .filter { hostOffset -> hostOffset >= 0 }
            .map { hostOffset -> injectedDocument.hostToInjected(hostOffset) }
            .firstOrNull { candidate -> candidate >= 0 }
            ?: return null
        return VbKeywordPairMatcher.match(file, injectedOffset)
    }
}
