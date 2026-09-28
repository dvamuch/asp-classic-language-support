package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable

/** Read-action bridge available across every supported IntelliJ Platform build. */
internal object AspReadAction {
    fun <T> compute(action: () -> T): T =
        ApplicationManager.getApplication().runReadAction(Computable(action))
}
