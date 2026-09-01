package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange

internal data class AspScriptletInfo(val range: TextRange, val prefix: String?)

/** Extracts the VBScript content range from an outer element in the HTML PSI. */
internal fun aspScriptletInfo(host: AspOuterPsiElement): AspScriptletInfo? {
    val text = host.node.chars
    val length = text.length
    if (length < 4 || text[0] != '<' || text[1] != '%') return null
    if (text[length - 2] != '%' || text[length - 1] != '>') return null

    val third = text[2]
    if (third == '-' && length > 3 && text[3] == '-') return null
    if (third == '@') return null

    val isExpression = third == '='
    val start = if (isExpression) 3 else 2
    val end = length - 2
    if (start >= end) return null
    return AspScriptletInfo(TextRange(start, end), if (isExpression) "Response.Write " else null)
}
