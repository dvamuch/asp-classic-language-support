package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.HtmlUtil
import java.util.concurrent.ConcurrentHashMap

class AspHtmlDebugAnnotator : Annotator {
    private val logger = Logger.getInstance(AspHtmlDebugAnnotator::class.java)
    private val loggedFiles = ConcurrentHashMap.newKeySet<String>()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val tag = element as? HtmlTag ?: return
        if (tag.name != "html") return

        val file = tag.containingFile ?: return
        val vFile = file.virtualFile ?: return
        val extension = vFile.extension?.lowercase() ?: ""
        if (extension != "asp" && extension != "inc") return

        val path = vFile.path
        if (!loggedFiles.add(path)) return

        val viewProvider = file.viewProvider
        val xmlFile = file as? XmlFile
        val rootTag: XmlTag? = xmlFile?.rootTag
        val elementDescriptor = rootTag?.getDescriptor()
        val templateDataLanguage = (viewProvider as? TemplateLanguageFileViewProvider)?.templateDataLanguage
        val allFiles = viewProvider.allFiles.joinToString { psi ->
            "${psi.javaClass.simpleName}:${psi.language.id}:${psi.fileType.name}"
        }

        logger.warn(
            "ASP HTML debug: file=${file.javaClass.name} lang=${file.language.id} " +
                "fileType=${file.fileType.name} viewProvider=${viewProvider.javaClass.name} " +
                "baseLang=${viewProvider.baseLanguage.id} templateDataLang=${templateDataLanguage?.id} " +
                "xmlFile=${xmlFile?.javaClass?.name} rootTag=${rootTag?.name} " +
                "isHtmlTag=${rootTag?.let { HtmlUtil.isHtmlTag(it) }} " +
                "ns=${rootTag?.namespace} elementDescriptor=${elementDescriptor?.javaClass?.name} " +
                "allFiles=[$allFiles]"
        )
    }
}
