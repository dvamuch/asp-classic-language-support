package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider

class VbScriptFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val spacingBuilder = VbScriptFormattingSupport.createSpacingBuilder(settings)
        val indentOptions = settings.getCommonSettings(VbScriptLanguage).indentOptions
        val indentSize = indentOptions?.INDENT_SIZE ?: 4
        val tabSize = indentOptions?.TAB_SIZE ?: indentSize
        val baseIndentProvider = VbScriptBaseIndentProvider.forFile(formattingContext.containingFile, tabSize)
        val rootBlock = VbScriptFormattingBlock(
            node = formattingContext.node,
            indent = null,
            spacingBuilder = spacingBuilder,
            indentSize = indentSize,
            baseIndentProvider = baseIndentProvider
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            rootBlock,
            settings
        )
    }

}
