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
        if (file.language == VbScriptLanguage) return matchVbScript(file, offset)
        if (file.viewProvider !is AspFileViewProvider) return null

        val manager = InjectedLanguageManager.getInstance(file.project)
        val injectedContext = sequenceOf(offset, offset - 1)
            .filter { candidate -> candidate >= 0 && candidate < file.textLength }
            .mapNotNull { candidate ->
                manager.findInjectedElementAt(file, candidate)?.let { element -> candidate to element }
            }
            .firstOrNull { (_, element) -> element.containingFile.language == VbScriptLanguage }
            ?: return null
        val (hostOffset, injectedElement) = injectedContext
        val injectedFile = injectedElement.containingFile
        val injectedDocument = PsiDocumentManager.getInstance(file.project).getDocument(injectedFile) as? DocumentWindow
            ?: return null
        val injectedOffset = injectedDocument.hostToInjected(hostOffset)
        if (injectedOffset < 0) return null

        val pair = VbKeywordPairMatcher.match(injectedFile, injectedOffset) ?: return null
        return Pair.create(
            injectedDocument.injectedToHost(pair.first),
            injectedDocument.injectedToHost(pair.second)
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
