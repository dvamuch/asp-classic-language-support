package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.html.HTMLLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

object AspTokenTypes {
    val TEMPLATE_DATA: IElementType = VbTypes.ASP_TEMPLATE_DATA
    val OUTER: IElementType = AspOuterElementType("ASP_OUTER", AspLanguage)
    val TEMPLATE_FILE: IFileElementType = AspTemplateDataElementType(
        "ASP_TEMPLATE_FILE",
        AspLanguage,
        TEMPLATE_DATA,
        OUTER
    )
    val FILE: IFileElementType = IFileElementType(AspLanguage)
}
