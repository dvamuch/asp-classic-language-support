package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.html.HTMLLanguage
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

object AspTokenTypes {
    val TEMPLATE_DATA: IElementType = IElementType("ASP_TEMPLATE_DATA", HTMLLanguage.INSTANCE)
    val OUTER: IElementType = AspOuterElementType("ASP_OUTER", AspLanguage)
    val TEMPLATE_FILE: IFileElementType = TemplateDataElementType(
        "ASP_TEMPLATE_FILE",
        AspLanguage,
        TEMPLATE_DATA,
        OUTER
    )
    val FILE: IFileElementType = IFileElementType(AspLanguage)
}
