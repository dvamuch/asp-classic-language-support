package com.dmitry.aspclassic

import com.dmitry.aspclassic.psi.*
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

/**
 * Structure view for ASP Classic files
 */
class AspStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return AspStructureViewModel(psiFile, editor)
            }
        }
    }
}

class AspStructureViewModel(psiFile: PsiFile, editor: Editor?) :
    StructureViewModelBase(psiFile, editor, AspStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = 
        element.value is AspFunctionDeclaration || element.value is AspSubDeclaration
}

class AspStructureViewElement(private val element: PsiElement) : StructureViewTreeElement {
    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        if (element is com.intellij.pom.Navigatable) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = 
        element is com.intellij.pom.Navigatable && element.canNavigate()

    override fun canNavigateToSource(): Boolean = 
        element is com.intellij.pom.Navigatable && element.canNavigateToSource()

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? {
                return when (element) {
                    is AspFunctionDeclaration -> "Function: ${extractName(element)}"
                    is AspSubDeclaration -> "Sub: ${extractName(element)}"
                    is PsiFile -> element.name
                    else -> element.text?.take(50)
                }
            }

            override fun getLocationString(): String? = null

            override fun getIcon(unused: Boolean): Icon? = AspIcons.FILE

            private fun extractName(element: PsiElement): String {
                val identifier = PsiTreeUtil.findChildOfType(element, PsiElement::class.java)
                return identifier?.text ?: "unnamed"
            }
        }
    }

    override fun getChildren(): Array<TreeElement> {
        if (element is PsiFile) {
            val functions = PsiTreeUtil.findChildrenOfAnyType(
                element,
                AspFunctionDeclaration::class.java,
                AspSubDeclaration::class.java
            )
            return functions.map { AspStructureViewElement(it) }.toTypedArray()
        }
        return emptyArray()
    }
}

