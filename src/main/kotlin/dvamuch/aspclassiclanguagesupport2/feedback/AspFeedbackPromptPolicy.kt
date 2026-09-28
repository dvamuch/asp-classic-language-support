package dvamuch.aspclassiclanguagesupport2.feedback

internal object AspFeedbackPromptPolicy {
    const val MINIMUM_ASP_FILE_OPENS = 8
    const val MINIMUM_USAGE_DAYS = 7L

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    private const val MINIMUM_USAGE_MILLIS = MINIMUM_USAGE_DAYS * MILLIS_PER_DAY

    fun recordAspFileOpen(
        state: AspFeedbackPromptState.StoredState,
        nowMillis: Long
    ): Boolean {
        if (state.promptShown) return false

        if (state.firstAspUseAtMillis <= 0L || nowMillis < state.firstAspUseAtMillis) {
            state.firstAspUseAtMillis = nowMillis
        }
        if (state.aspFilesOpened < Int.MAX_VALUE) {
            state.aspFilesOpened++
        }

        val usedLongEnough = nowMillis - state.firstAspUseAtMillis >= MINIMUM_USAGE_MILLIS
        if (state.aspFilesOpened < MINIMUM_ASP_FILE_OPENS || !usedLongEnough) return false

        state.promptShown = true
        return true
    }
}
