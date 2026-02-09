package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiReferenceContributor

class VbDebugProjectActivity : ProjectActivity {
    private val logger = Logger.getInstance(VbDebugProjectActivity::class.java)

    override suspend fun execute(project: Project) {
        val contributors = PsiReferenceContributor.EP_NAME.extensionList
        val names = contributors.map { it.javaClass.name }
        val hasVbContributor = names.contains(VbReferenceContributor::class.java.name)
        logger.warn(
            "VBScript ref debug: project activity contributors=${names.joinToString()} " +
                "hasVbContributor=$hasVbContributor project=${project.name}"
        )
    }
}
