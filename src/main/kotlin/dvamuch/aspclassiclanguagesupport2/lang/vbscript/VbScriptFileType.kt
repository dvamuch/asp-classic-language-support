package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object VbScriptFileType : LanguageFileType(VbScriptLanguage) {
    override fun getName(): String = "VBScript"

    override fun getDescription(): String = "VBScript file"

    override fun getDefaultExtension(): String = "vbs"

    override fun getIcon(): Icon? = null
}
