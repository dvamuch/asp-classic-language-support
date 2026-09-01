package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.highlighting.HeavyBraceHighlighter
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbKeywordPairMatcher
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

class VbKeywordBraceHighlighter : HeavyBraceHighlighter() {
    override fun isAvailable(file: PsiFile, offset: Int): Boolean {
        return file.language == VbScriptLanguage || file.viewProvider is AspFileViewProvider
    }

    override fun matchBrace(file: PsiFile, offset: Int): Pair<TextRange, TextRange>? {
        if (file.language == VbScriptLanguage) {
            return VbKeywordPairMatcher.match(file, offset)
        }
        if (file.viewProvider !is AspFileViewProvider) return null

        return matchAsp(file, offset)
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
}
