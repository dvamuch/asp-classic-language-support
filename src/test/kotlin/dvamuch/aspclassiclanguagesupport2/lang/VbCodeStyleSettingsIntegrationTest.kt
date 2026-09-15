package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptCodeStyleSettings
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptCodeStyleSettingsFactory
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptCodeStyleSettingsProvider
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptCodeStylePanel
import java.awt.Component
import java.awt.Container
import javax.swing.JComboBox

class VbCodeStyleSettingsIntegrationTest : BasePlatformTestCase() {
    fun testRegistersNamedCodeStylePageAndPersistentSettings() {
        val languageProvider = LanguageCodeStyleSettingsProvider.forLanguage(VbScriptLanguage)
        assertTrue(languageProvider is VbScriptCodeStyleSettingsProvider)

        val settings = CodeStyleSettings()
        assertEquals(
            VbScriptCodeStyleSettings.KEYWORD_CASE_PRESERVE,
            settings.getCustomSettings(VbScriptCodeStyleSettings::class.java).KEYWORD_CASE
        )

        val configurable = VbScriptCodeStyleSettingsFactory().createConfigurable(settings, settings.clone())
        try {
            assertEquals("VBScript", configurable.displayName)
            val component = configurable.createComponent()
            assertNotNull(component)
            val combo = descendants(component!!).filterIsInstance<JComboBox<*>>().single()
            assertEquals(
                listOf("Preserve existing", "lower case", "Title Case"),
                (0 until combo.itemCount).map(combo::getItemAt)
            )
            val panel = (configurable as CodeStyleAbstractConfigurable).panel as VbScriptCodeStylePanel

            assertTrue(panel.previewTextForTest(), panel.previewTextForTest().contains("iF ready tHen"))

            combo.selectedIndex = VbScriptCodeStyleSettings.KEYWORD_CASE_LOWER
            assertTrue(panel.previewTextForTest(), panel.previewTextForTest().contains("if ready then"))
            assertTrue(panel.previewTextForTest(), panel.previewTextForTest().contains("end if"))
            assertFalse(panel.previewTextForTest(), panel.previewTextForTest().contains("If ready Then"))

            combo.selectedIndex = VbScriptCodeStyleSettings.KEYWORD_CASE_TITLE
            assertTrue(panel.previewTextForTest(), panel.previewTextForTest().contains("If ready Then"))
            assertTrue(panel.previewTextForTest(), panel.previewTextForTest().contains("End If"))

            combo.selectedIndex = VbScriptCodeStyleSettings.KEYWORD_CASE_LOWER
            configurable.apply()
            assertEquals(
                VbScriptCodeStyleSettings.KEYWORD_CASE_LOWER,
                settings.getCustomSettings(VbScriptCodeStyleSettings::class.java).KEYWORD_CASE
            )
        } finally {
            configurable.disposeUIResources()
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) {
            component.components.forEach { child -> yieldAll(descendants(child)) }
        }
    }
}
