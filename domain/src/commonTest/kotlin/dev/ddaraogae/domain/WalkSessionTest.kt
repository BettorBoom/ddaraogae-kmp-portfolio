package dev.ddaraogae.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class WalkSessionTest {
    @Test fun rejectsMissingIdentity() {
        assertFailsWith<IllegalArgumentException> { WalkSession("", "user", 1, WalkStatus.Recording) }
        assertFailsWith<IllegalArgumentException> { WalkSession("walk", "", 1, WalkStatus.Recording) }
    }

    @Test fun rejectsClockReversal() {
        assertFailsWith<IllegalArgumentException> {
            WalkSession("walk", "user", 100, WalkStatus.Completed, 99)
        }
    }

    @Test fun keepsIdentityWhenCompleted() {
        val walk = WalkSession("walk", "user", 100, WalkStatus.Recording)
        assertEquals("walk", walk.copy(status = WalkStatus.Completed, endedAtMillis = 200).id)
    }
}
