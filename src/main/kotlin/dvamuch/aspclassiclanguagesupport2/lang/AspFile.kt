package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class AspFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AspLanguage) {
    override fun getFileType() = AspFileType

    override fun toString(): String = "ASP Classic File"
}
