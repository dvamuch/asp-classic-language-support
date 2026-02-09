package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

object AspTokenTypes {
    val TEMPLATE_DATA: IElementType = IElementType("ASP_TEMPLATE_DATA", AspLanguage)
    val OUTER: IElementType = IElementType("ASP_OUTER", AspLanguage)
    val FILE: IFileElementType = TemplateDataElementType(
        "ASP_FILE",
        AspLanguage,
        TEMPLATE_DATA,
        OUTER
    )
}
