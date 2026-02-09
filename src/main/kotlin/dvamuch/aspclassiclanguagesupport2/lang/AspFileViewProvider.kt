package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider

class AspFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val templateDataLanguage: Language = HTMLLanguage.INSTANCE
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, file, physical), TemplateLanguageFileViewProvider {
    override fun getBaseLanguage(): Language = AspLanguage

    override fun getTemplateDataLanguage(): Language = templateDataLanguage

    override fun getLanguages(): Set<Language> = setOf(baseLanguage, templateDataLanguage)

    override fun createFile(language: Language): PsiFile? {
        val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language) ?: return null
        val psiFile = parserDefinition.createFile(this)
        if (language == templateDataLanguage && psiFile is PsiFileImpl) {
            psiFile.contentElementType = AspTokenTypes.TEMPLATE_FILE
        }
        return psiFile
    }

    override fun cloneInner(file: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider {
        return AspFileViewProvider(manager, file, false, templateDataLanguage)
    }
}

class AspFileViewProviderFactory : com.intellij.psi.FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        physical: Boolean
    ): FileViewProvider {
        return AspFileViewProvider(manager, file, physical)
    }
}
