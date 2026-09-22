package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.xml.XmlComment
import dvamuch.aspclassiclanguagesupport2.lang.AspFileViewProvider

class AspMissingIncludeInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val comment = element as? XmlComment ?: return
                if (comment.containingFile.viewProvider !is AspFileViewProvider) return
                val directive = AspIncludeDirectiveParser.parse(comment.text) ?: return
                if (AspIncludeReference(comment, directive).resolve() != null) return

                holder.registerProblem(
                    comment,
                    "Included file not found: ${directive.path}",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                    directive.pathRange
                )
            }
        }
    }
}
