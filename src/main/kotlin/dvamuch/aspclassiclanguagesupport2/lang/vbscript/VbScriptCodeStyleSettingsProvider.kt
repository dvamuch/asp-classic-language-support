package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class VbScriptCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = VbScriptLanguage

    override fun getConfigurableDisplayName(): String = "VBScript"

    override fun customizeSettings(
        consumer: CodeStyleSettingsCustomizable,
        settingsType: SettingsType
    ) {
        when (settingsType) {
            SettingsType.INDENT_SETTINGS -> consumer.showStandardOptions(
                "USE_TAB_CHARACTER",
                "TAB_SIZE",
                "INDENT_SIZE",
                "CONTINUATION_INDENT_SIZE"
            )

            SettingsType.LANGUAGE_SPECIFIC -> consumer.showCustomOption(
                VbScriptCodeStyleSettings::class.java,
                "KEYWORD_CASE",
                "Keyword case",
                "Code style",
                arrayOf("Preserve existing", "lower case", "Title Case"),
                intArrayOf(
                    VbScriptCodeStyleSettings.KEYWORD_CASE_PRESERVE,
                    VbScriptCodeStyleSettings.KEYWORD_CASE_LOWER,
                    VbScriptCodeStyleSettings.KEYWORD_CASE_TITLE
                )
            )

            else -> Unit
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String = CODE_SAMPLE

    companion object {
        val CODE_SAMPLE = """
            oPtion eXplicit

            iF ready tHen
                fOr eAch item iN items
                    Response.Write item.Name
                nExt
            eLse
                Response.Write "No items"
            eNd iF
        """.trimIndent()
    }
}
