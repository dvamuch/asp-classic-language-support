package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbPsiFactory(private val project: Project) {
    fun createId(name: String): VbId {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.vbs", VbScriptFileType, "dim $name")
        return PsiTreeUtil.findChildOfType(file, VbId::class.java)
            ?: throw IllegalStateException("Failed to create VbId for name=$name")
    }
}
