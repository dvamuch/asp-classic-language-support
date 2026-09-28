package dvamuch.aspclassiclanguagesupport2.feedback

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(
    name = "AspClassicFeedbackPrompt",
    storages = [Storage("aspClassicFeedback.xml")]
)
class AspFeedbackPromptState : PersistentStateComponent<AspFeedbackPromptState.StoredState> {
    private var storedState = StoredState()

    override fun getState(): StoredState = storedState

    override fun loadState(state: StoredState) {
        storedState = state
    }

    @Synchronized
    fun recordAspFileOpen(nowMillis: Long = System.currentTimeMillis()): Boolean =
        AspFeedbackPromptPolicy.recordAspFileOpen(storedState, nowMillis)

    class StoredState {
        var firstAspUseAtMillis: Long = 0
        var aspFilesOpened: Int = 0
        var promptShown: Boolean = false
    }

    companion object {
        fun getInstance(): AspFeedbackPromptState = service()
    }
}
