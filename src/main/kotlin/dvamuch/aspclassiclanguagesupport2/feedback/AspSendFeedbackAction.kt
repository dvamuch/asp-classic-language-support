package dvamuch.aspclassiclanguagesupport2.feedback

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class AspSendFeedbackAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        BrowserUtil.browse(AspFeedbackLinks.GITHUB_ISSUES)
    }
}
