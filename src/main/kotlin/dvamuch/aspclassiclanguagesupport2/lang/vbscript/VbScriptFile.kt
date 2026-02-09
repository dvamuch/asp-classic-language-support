package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class VbScriptFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, VbScriptLanguage) {
    override fun getFileType() = VbScriptFileType

    override fun toString(): String = "VBScript File"
}
