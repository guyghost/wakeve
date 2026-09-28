package com.guyghost.wakeve.ui.event

import kotlin.test.Test
import kotlin.test.assertEquals

class EventLifecycleAndroidCopyTest {
    @Test
    fun `every shared blocker code has an Android string resource`() {
        assertEquals(
            EventLifecycleBlockerCodes.all.toSet(),
            eventLifecycleBlockerLabelResources.keys,
        )
    }
}
