package dvamuch.aspclassiclanguagesupport2.feedback

import junit.framework.TestCase

class AspFeedbackPromptPolicyTest : TestCase() {
    fun testDoesNotPromptBeforeMinimumUsageAge() {
        val state = AspFeedbackPromptState.StoredState()
        val firstUse = 1_000L

        repeat(AspFeedbackPromptPolicy.MINIMUM_ASP_FILE_OPENS + 3) { index ->
            assertFalse(
                AspFeedbackPromptPolicy.recordAspFileOpen(
                    state,
                    firstUse + index
                )
            )
        }
    }

    fun testPromptsOnceAfterSustainedUse() {
        val state = AspFeedbackPromptState.StoredState()
        val firstUse = 1_000L

        repeat(AspFeedbackPromptPolicy.MINIMUM_ASP_FILE_OPENS - 1) {
            assertFalse(AspFeedbackPromptPolicy.recordAspFileOpen(state, firstUse))
        }

        val eligibleTime = firstUse +
            AspFeedbackPromptPolicy.MINIMUM_USAGE_DAYS * 24L * 60L * 60L * 1000L
        assertTrue(AspFeedbackPromptPolicy.recordAspFileOpen(state, eligibleTime))
        assertTrue(state.promptShown)
        assertFalse(AspFeedbackPromptPolicy.recordAspFileOpen(state, eligibleTime + 1))
    }

    fun testClockMovingBackwardsRestartsWaitingPeriod() {
        val state = AspFeedbackPromptState.StoredState().apply {
            firstAspUseAtMillis = 10_000L
            aspFilesOpened = AspFeedbackPromptPolicy.MINIMUM_ASP_FILE_OPENS
        }

        assertFalse(AspFeedbackPromptPolicy.recordAspFileOpen(state, 5_000L))
        assertEquals(5_000L, state.firstAspUseAtMillis)
        assertFalse(state.promptShown)
    }
}
