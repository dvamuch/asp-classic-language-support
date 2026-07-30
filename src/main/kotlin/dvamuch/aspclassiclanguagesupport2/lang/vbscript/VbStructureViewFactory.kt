package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbClassStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPropertyStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSubStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
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
) : PsiTreeElementBase<PsiFile>(physicalFile) {
    override fun getPresentableText(): String = element?.name.orEmpty()

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val structureFile = structureFileProvider() ?: return emptyList()
        return structureDeclarations(structureFile).map(::VbStructureDeclarationElement)
    }
}

private class VbStructureDeclarationElement(
    id: VbId
) : PsiTreeElementBase<VbId>(id) {
    override fun getPresentableText(): String {
        val id = element ?: return ""
        val name = (id as? VbNamedElement)?.name.orEmpty()
        return when (val declaration = id.parent) {
            is VbFunctionStmt -> name + declaration.paramList?.text.orEmpty()
            is VbSubStmt -> name + declaration.paramList?.text.orEmpty()
            is VbPropertyStmt -> "${propertyAccessor(declaration)} $name${declaration.paramList?.text.orEmpty()}"
            else -> name
        }
    }

    override fun getLocationString(): String? = when (element?.parent) {
        is VbClassStmt -> "Class"
        is VbFunctionStmt -> "Function"
        is VbSubStmt -> "Sub"
        is VbPropertyStmt -> "Property"
        else -> null
    }

    override fun getIcon(open: Boolean): Icon? = when (element?.parent) {
        is VbClassStmt -> AllIcons.Nodes.Class
        is VbFunctionStmt -> AllIcons.Nodes.Function
        is VbSubStmt -> AllIcons.Nodes.Method
        is VbPropertyStmt -> AllIcons.Nodes.Property
        else -> null
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
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

private fun structureDeclarations(scope: PsiElement): List<VbId> {
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
        .toList()
}
