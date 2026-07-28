package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlComment

class AspIncludeReference(
    element: XmlComment,
    private val directive: AspIncludeDirective
) : PsiReferenceBase<XmlComment>(element, directive.pathRange, false) {

    override fun resolve(): PsiElement? {
        val target = when (directive.kind) {
            AspIncludeKind.FILE -> resolveRelativeFile()
            AspIncludeKind.VIRTUAL -> resolveVirtualFile()
        } ?: return null

        if (target.isDirectory) return null
        return PsiManager.getInstance(element.project).findFile(target)
    }

    override fun getVariants(): Array<Any> = emptyArray()

    private fun resolveRelativeFile(): VirtualFile? {
        val sourceFile = element.containingFile.originalFile.virtualFile ?: return null
        val sourceDirectory = sourceFile.parent ?: return null
        return VfsUtilCore.findRelativeFile(normalizedPath(), sourceDirectory)
    }

    private fun resolveVirtualFile(): VirtualFile? {
        val relativePath = normalizedPath().trimStart('/')
        if (relativePath.isEmpty()) return null

        return projectRoots().firstNotNullOfOrNull { root ->
            VfsUtilCore.findRelativeFile(relativePath, root)
        }
    }

    private fun projectRoots(): List<VirtualFile> {
        val project = element.project
        val contentRoots = ProjectRootManager.getInstance(project).contentRoots.asList()
        val projectDirectory = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        return buildList {
            addAll(contentRoots)
            if (projectDirectory != null && projectDirectory !in contentRoots) add(projectDirectory)
        }
    }

    private fun normalizedPath(): String = directive.path.replace('\\', '/')
}
