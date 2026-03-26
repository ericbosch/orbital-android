package com.orbital.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BackendProfileTest {

    @Test
    fun `fromStored defaults to orbitdock when value is missing`() {
        assertEquals(BackendProfile.ORBITDOCK, BackendProfile.fromStored(null))
    }

    @Test
    fun `fromStored keeps explicit orbital value`() {
        assertEquals(BackendProfile.ORBITAL, BackendProfile.fromStored("orbital"))
    }
}
