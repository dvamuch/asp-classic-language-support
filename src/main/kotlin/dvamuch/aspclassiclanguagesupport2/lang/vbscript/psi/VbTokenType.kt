package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.tree.IElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

class VbTokenType(debugName: String) : IElementType(debugName, VbScriptLanguage) {
    override fun toString(): String = "VBScriptToken." + super.toString()
}
