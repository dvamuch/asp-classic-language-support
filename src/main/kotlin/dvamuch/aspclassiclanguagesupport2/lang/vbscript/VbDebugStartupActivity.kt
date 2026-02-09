package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.psi.PsiReferenceContributor

class VbDebugStartupActivity : StartupActivity.DumbAware {
    private val logger = Logger.getInstance(VbDebugStartupActivity::class.java)

    override fun runActivity(project: Project) {
        val contributors = PsiReferenceContributor.EP_NAME.extensionList
        val names = contributors.map { it.javaClass.name }
        val hasVbContributor = names.contains(VbReferenceContributor::class.java.name)
        logger.warn(
            "VBScript ref debug: startup contributors=${names.joinToString()} " +
                "hasVbContributor=$hasVbContributor"
        )
    }
}
