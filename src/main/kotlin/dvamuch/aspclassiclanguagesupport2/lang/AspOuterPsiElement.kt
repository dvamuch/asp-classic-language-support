package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.templateLanguages.OuterLanguageElementImpl
import com.intellij.psi.tree.IElementType

class AspOuterPsiElement(type: IElementType, text: CharSequence) :
    OuterLanguageElementImpl(type, text),
    PsiLanguageInjectionHost {

    override fun isValidHost(): Boolean = true

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return replaceWithText(text) as PsiLanguageInjectionHost
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return AspTextEscaper(this)
    }

    private class AspTextEscaper(host: AspOuterPsiElement) : LiteralTextEscaper<AspOuterPsiElement>(host) {
        override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
            outChars.append(rangeInsideHost.substring(myHost.text))
            return true
        }

        override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
            val offset = rangeInsideHost.startOffset + offsetInDecoded
            return if (offset <= rangeInsideHost.endOffset) offset else -1
        }

        override fun isOneLine(): Boolean = false
    }
}
