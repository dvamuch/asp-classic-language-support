package dvamuch.aspclassiclanguagesupport2.feedback

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class AspFeedbackFileEditorListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (!isAspClassicSource(file)) return

        val application = ApplicationManager.getApplication()
        if (
            application.isHeadlessEnvironment ||
            application.isUnitTestMode ||
            java.lang.Boolean.getBoolean("idea.plugin.in.sandbox.mode")
        ) {
            return
        }

        if (!AspFeedbackPromptState.getInstance().recordAspFileOpen()) return

        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                "Enjoying ASP Classic Language Support?",
                "An honest Marketplace review helps other ASP Classic developers discover the plugin. " +
                    "Bug reports and feature suggestions are also welcome.",
                NotificationType.INFORMATION
            )
            .addAction(
                NotificationAction.createSimpleExpiring("Leave a review") {
                    BrowserUtil.browse(AspFeedbackLinks.MARKETPLACE_REVIEWS)
                }
            )
            .addAction(
                NotificationAction.createSimpleExpiring("Report an issue") {
                    BrowserUtil.browse(AspFeedbackLinks.GITHUB_ISSUES)
                }
            )
            .addAction(NotificationAction.createSimpleExpiring("Don't show again") {})
            .notify(source.project)
    }

    internal fun isAspClassicSource(file: VirtualFile): Boolean =
        file.extension?.lowercase() in SUPPORTED_EXTENSIONS

    companion object {
        private const val NOTIFICATION_GROUP_ID = "ASP Classic feedback"
        private val SUPPORTED_EXTENSIONS = setOf("asp", "inc", "vbs")
    }
}
