package com.kara.kara_general_api.domain.model.room.vo

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class BoundingBoxTest {
    @Test
    fun `should build a valid bounding box`() {
        assertDoesNotThrow { BoundingBox(minLat = 48.8, minLng = 2.2, maxLat = 48.9, maxLng = 2.4) }
    }

    @Test
    fun `should accept a degenerate box where corners coincide`() {
        assertDoesNotThrow { BoundingBox(minLat = 10.0, minLng = 10.0, maxLat = 10.0, maxLng = 10.0) }
    }

    @Test
    fun `should reject when min latitude exceeds max latitude`() {
        assertThrows(IllegalArgumentException::class.java) {
            BoundingBox(minLat = 49.0, minLng = 2.2, maxLat = 48.0, maxLng = 2.4)
        }
    }

    @Test
    fun `should reject when min longitude exceeds max longitude`() {
        assertThrows(IllegalArgumentException::class.java) {
            BoundingBox(minLat = 48.0, minLng = 3.0, maxLat = 48.9, maxLng = 2.4)
        }
    }

    @Test
    fun `should reject latitude out of range`() {
        assertThrows(IllegalArgumentException::class.java) {
            BoundingBox(minLat = -91.0, minLng = 2.2, maxLat = 48.9, maxLng = 2.4)
        }
    }

    @Test
    fun `should reject longitude out of range`() {
        assertThrows(IllegalArgumentException::class.java) {
            BoundingBox(minLat = 48.8, minLng = 2.2, maxLat = 48.9, maxLng = 181.0)
        }
    }
}
