package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.TemplateLanguageFileType
import javax.swing.Icon

object AspFileType : LanguageFileType(AspLanguage), TemplateLanguageFileType {
    override fun getName(): String = "ASP"

    override fun getDescription(): String = "ASP Classic file"

    override fun getDefaultExtension(): String = "asp"

    override fun getIcon(): Icon? = null
}
