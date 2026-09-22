package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

class VbScriptCodeStyleSettings(container: CodeStyleSettings) :
    CustomCodeStyleSettings("VbScriptCodeStyleSettings", container) {

    @JvmField
    var KEYWORD_CASE: Int = KEYWORD_CASE_TITLE

    @JvmField
    var SPACE_INSIDE_ASP_DELIMITERS: Boolean = true

    @JvmField
    var MATCH_ASP_DELIMITER_PLACEMENT: Boolean = true

    companion object {
        const val KEYWORD_CASE_PRESERVE = 0
        const val KEYWORD_CASE_LOWER = 1
        const val KEYWORD_CASE_TITLE = 2
    }
}
