package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import java.util.Locale

internal object VbScriptKeywordCaseSupport {
    private val canonicalByType: Map<IElementType, String> = linkedMapOf(
        VbTypes.OPTION to "Option", VbTypes.EXPLICIT to "Explicit",
        VbTypes.DIM to "Dim", VbTypes.CONST to "Const",
        VbTypes.PUBLIC_KW to "Public", VbTypes.PRIVATE_KW to "Private", VbTypes.DEFAULT to "Default",
        VbTypes.CLASS to "Class", VbTypes.END to "End",
        VbTypes.FUNCTION to "Function", VbTypes.SUB to "Sub", VbTypes.PROPERTY to "Property",
        VbTypes.GET to "Get", VbTypes.LET to "Let", VbTypes.SET to "Set",
        VbTypes.IF to "If", VbTypes.THEN to "Then", VbTypes.ELSE to "Else", VbTypes.ELSEIF to "ElseIf",
        VbTypes.SELECT to "Select", VbTypes.CASE to "Case",
        VbTypes.FOR to "For", VbTypes.EACH to "Each", VbTypes.IN to "In",
        VbTypes.TO to "To", VbTypes.STEP to "Step", VbTypes.NEXT to "Next",
        VbTypes.DO to "Do", VbTypes.LOOP to "Loop", VbTypes.WHILE to "While",
        VbTypes.UNTIL to "Until", VbTypes.WEND to "Wend", VbTypes.WITH to "With",
        VbTypes.EXIT to "Exit", VbTypes.ON to "On", VbTypes.ERROR to "Error",
        VbTypes.RESUME to "Resume", VbTypes.GOTO to "GoTo",
        VbTypes.REDIM to "ReDim", VbTypes.PRESERVE to "Preserve", VbTypes.ERASE to "Erase",
        VbTypes.EXECUTE to "Execute", VbTypes.EXECUTEGLOBAL to "ExecuteGlobal",
        VbTypes.CALL to "Call", VbTypes.NEW to "New",
        VbTypes.BYVAL to "ByVal", VbTypes.BYREF to "ByRef", VbTypes.OPTIONAL to "Optional",
        VbTypes.TRUE to "True", VbTypes.FALSE to "False", VbTypes.NULL to "Null",
        VbTypes.EMPTY to "Empty", VbTypes.NOTHING to "Nothing",
        VbTypes.AND to "And", VbTypes.OR to "Or", VbTypes.NOT to "Not",
        VbTypes.XOR to "Xor", VbTypes.EQV to "Eqv", VbTypes.IMP to "Imp",
        VbTypes.IS to "Is", VbTypes.MOD to "Mod"
    )

    val tokenSet: TokenSet = TokenSet.create(*canonicalByType.keys.toTypedArray())

    fun mode(settings: CodeStyleSettings): Int =
        settings.getCustomSettings(VbScriptCodeStyleSettings::class.java).KEYWORD_CASE

    fun normalizeText(text: String, mode: Int): String {
        if (mode == VbScriptCodeStyleSettings.KEYWORD_CASE_PRESERVE) return text
        val lexer = VbScriptLexerAdapter()
        lexer.start(text)
        var sourceOffset = 0
        return buildString(text.length) {
            while (lexer.tokenType != null) {
                val replacement = canonicalByType[lexer.tokenType]?.let { canonical ->
                    applyMode(canonical, mode)
                }
                if (replacement != null) {
                    append(text, sourceOffset, lexer.tokenStart)
                    append(replacement)
                    sourceOffset = lexer.tokenEnd
                }
                lexer.advance()
            }
            append(text, sourceOffset, text.length)
        }
    }

    fun normalizeCompletion(keyword: String, mode: Int): String {
        if (mode != VbScriptCodeStyleSettings.KEYWORD_CASE_LOWER) return keyword
        return keyword.lowercase(Locale.ROOT)
    }

    private fun applyMode(canonical: String, mode: Int): String = when (mode) {
        VbScriptCodeStyleSettings.KEYWORD_CASE_LOWER -> canonical.lowercase(Locale.ROOT)
        VbScriptCodeStyleSettings.KEYWORD_CASE_TITLE -> canonical
        else -> canonical
    }
}
