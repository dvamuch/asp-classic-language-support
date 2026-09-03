package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel

/** Registers both the persistent settings object and the VBScript Code Style page. */
class VbScriptCodeStyleSettingsFactory : CodeStyleSettingsProvider() {
    override fun getLanguage(): Language = VbScriptLanguage

    override fun getConfigurableDisplayName(): String = "VBScript"

    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings =
        VbScriptCodeStyleSettings(settings)

    override fun createConfigurable(
        settings: CodeStyleSettings,
        modelSettings: CodeStyleSettings
    ): CodeStyleConfigurable = object : CodeStyleAbstractConfigurable(
        settings,
        modelSettings,
        configurableDisplayName
    ) {
        override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
            VbScriptCodeStylePanel(currentSettings, settings)
    }
}

internal class VbScriptCodeStylePanel(
    currentSettings: CodeStyleSettings,
    settings: CodeStyleSettings
) : CodeStyleAbstractPanel(VbScriptLanguage, currentSettings, settings) {
    private val keywordCase = ComboBox(KEYWORD_CASE_LABELS)
    private val panel = JPanel(BorderLayout(0, JBUI.scale(12)))

    init {
        val options = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            add(JBLabel("Keyword case:"))
            add(keywordCase)
        }
        val preview = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(12)
        panel.add(options, BorderLayout.NORTH)
        panel.add(preview, BorderLayout.CENTER)
        keywordCase.addActionListener {
            apply(settings)
            refreshPreview()
            somethingChanged()
        }
        addPanelToWatch(panel)
        installPreviewPanel(preview)
        refreshPreview()
    }

    override fun getRightMargin(): Int = 120

    override fun createHighlighter(scheme: EditorColorsScheme): EditorHighlighter =
        EditorHighlighterFactory.getInstance().createEditorHighlighter(VbScriptSyntaxHighlighter(), scheme)

    override fun getFileType(): FileType = VbScriptFileType

    override fun getPreviewText(): String = VbScriptCodeStyleSettingsProvider.CODE_SAMPLE

    override fun apply(settings: CodeStyleSettings) {
        settings.getCustomSettings(VbScriptCodeStyleSettings::class.java).KEYWORD_CASE = keywordCase.selectedIndex
    }

    override fun isModified(settings: CodeStyleSettings): Boolean =
        keywordCase.selectedIndex != settings
            .getCustomSettings(VbScriptCodeStyleSettings::class.java)
            .KEYWORD_CASE

    override fun getPanel(): JPanel = panel

    override fun resetImpl(settings: CodeStyleSettings) {
        val saved = settings.getCustomSettings(VbScriptCodeStyleSettings::class.java).KEYWORD_CASE
        keywordCase.selectedIndex = saved.coerceIn(KEYWORD_CASE_LABELS.indices)
        refreshPreview()
    }

    internal fun previewTextForTest(): String = getEditor()?.document?.text.orEmpty()

    private fun refreshPreview() {
        val editor = getEditor() ?: return
        val text = VbScriptKeywordCaseSupport.normalizeText(
            VbScriptCodeStyleSettingsProvider.CODE_SAMPLE,
            keywordCase.selectedIndex
        )
        if (editor.document.text == text) return
        ApplicationManager.getApplication().runWriteAction {
            editor.document.setText(text)
        }
    }

    companion object {
        private val KEYWORD_CASE_LABELS = arrayOf("Preserve existing", "lower case", "Title Case")
    }
}
