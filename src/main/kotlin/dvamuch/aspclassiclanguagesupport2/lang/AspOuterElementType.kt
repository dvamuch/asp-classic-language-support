package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.tree.OuterLanguageElementType

class AspOuterElementType(debugName: String, language: Language) : OuterLanguageElementType(debugName, language) {
    override fun createLeafNode(leafText: CharSequence): ASTNode {
        return AspOuterPsiElement(this, leafText)
    }
}
