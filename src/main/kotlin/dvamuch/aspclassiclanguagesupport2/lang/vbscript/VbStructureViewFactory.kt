package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbClassStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPropertyStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSubStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import dvamuch.aspclassiclanguagesupport2.lang.AspVbScriptContext
import javax.swing.Icon

class VbStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return VbStructureViewBuilder(psiFile) { psiFile }
    }
}

internal class VbStructureViewBuilder(
    private val physicalFile: PsiFile,
    private val structureFileProvider: () -> PsiFile?
) : TreeBasedStructureViewBuilder() {
    override fun createStructureViewModel(editor: Editor?): StructureViewModel {
        val root = VbStructureRootElement(physicalFile, structureFileProvider)
        return StructureViewModelBase(physicalFile, editor, root)
            .withSuitableClasses(VbId::class.java)
    }
}

private class VbStructureRootElement(
    physicalFile: PsiFile,
    private val structureFileProvider: () -> PsiFile?
) : VbStructureElement<PsiFile>(physicalFile) {
    override fun presentableText(): String = element?.name.orEmpty()

    override fun structureChildren(): Collection<StructureViewTreeElement> {
        val structureFile = structureFileProvider() ?: return emptyList()
        return structureDeclarations(structureFile).map(::VbStructureDeclarationElement)
    }
}

private class VbStructureDeclarationElement(
    id: VbId
) : VbStructureElement<VbId>(id) {
    override fun presentableText(): String {
        val id = element ?: return ""
        val name = (id as? VbNamedElement)?.name.orEmpty()
        return when (val declaration = id.parent) {
            is VbFunctionStmt -> name + declaration.paramList?.text.orEmpty()
            is VbSubStmt -> name + declaration.paramList?.text.orEmpty()
            is VbPropertyStmt -> "${propertyAccessor(declaration)} $name${declaration.paramList?.text.orEmpty()}"
            else -> name
        }
    }

    override fun locationString(): String? = when (element?.parent) {
        is VbClassStmt -> "Class"
        is VbFunctionStmt -> "Function"
        is VbSubStmt -> "Sub"
        is VbPropertyStmt -> "Property"
        else -> null
    }

    override fun presentationIcon(open: Boolean): Icon? = when (element?.parent) {
        is VbClassStmt -> AllIcons.Nodes.Class
        is VbFunctionStmt -> AllIcons.Nodes.Function
        is VbSubStmt -> AllIcons.Nodes.Method
        is VbPropertyStmt -> AllIcons.Nodes.Property
        else -> null
    }

    override fun structureChildren(): Collection<StructureViewTreeElement> {
        val declaration = element?.parent as? VbClassStmt ?: return emptyList()
        return structureDeclarations(declaration).map(::VbStructureDeclarationElement)
    }

    private fun propertyAccessor(property: VbPropertyStmt): String = when {
        property.node.findChildByType(VbTypes.GET) != null -> "Get"
        property.node.findChildByType(VbTypes.LET) != null -> "Let"
        property.node.findChildByType(VbTypes.SET) != null -> "Set"
        else -> "Property"
    }
}

private abstract class VbStructureElement<T : PsiElement>(element: T) : StructureViewTreeElement {
    private val pointer = SmartPointerManager.createPointer(element)

    protected val element: T?
        get() = pointer.element

    override fun getValue(): Any? = element

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = presentableText()

        override fun getLocationString(): String? = locationString()

        override fun getIcon(unused: Boolean): Icon? = presentationIcon(unused)
    }

    override fun getChildren(): Array<TreeElement> = structureChildren().toTypedArray()

    override fun navigate(requestFocus: Boolean) {
        navigatable()?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = navigatable()?.canNavigate() == true

    override fun canNavigateToSource(): Boolean = navigatable()?.canNavigateToSource() == true

    protected abstract fun presentableText(): String

    protected open fun locationString(): String? = null

    protected open fun presentationIcon(open: Boolean): Icon? = null

    protected abstract fun structureChildren(): Collection<StructureViewTreeElement>

    private fun navigatable(): Navigatable? = element?.navigationElement as? Navigatable
}

private fun structureDeclarations(scope: PsiElement): List<VbId> {
    val analysisContext = (scope as? PsiFile)?.let(AspVbScriptContext::forAnalysisFile)
    return VbFileSymbolTable.get(scope.containingFile).declarations()
        .asSequence()
        .filter { declaration -> declaration.scope === scope }
        .map { declaration -> declaration.id }
        .filter { id ->
            id.parent is VbClassStmt ||
                id.parent is VbFunctionStmt ||
                id.parent is VbSubStmt ||
                id.parent is VbPropertyStmt
        }
        .sortedBy { id -> id.textOffset }
        .map { id -> analysisContext?.hostIdForAnalysis(id) ?: id }
        .toList()
}
