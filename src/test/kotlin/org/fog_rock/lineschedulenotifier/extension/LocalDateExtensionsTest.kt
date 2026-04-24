/*
 * Copyright (c) 2026 SallyLueNoa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fog_rock.lineschedulenotifier.extension

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LocalDateExtensionsTest {

    private val startDate = LocalDate.of(2026, 4, 20)
    private val endDate = LocalDate.of(2026, 4, 30)

    @Test
    fun testIsBetween_dateWithinRange() {
        val date = LocalDate.of(2026, 4, 25)
        assertTrue(date.isBetween(startDate, endDate))
    }

    @Test
    fun testIsBetween_dateIsStartDate() {
        val date = LocalDate.of(2026, 4, 20)
        assertTrue(date.isBetween(startDate, endDate))
    }

    @Test
    fun testIsBetween_dateIsEndDate() {
        val date = LocalDate.of(2026, 4, 30)
        assertTrue(date.isBetween(startDate, endDate))
    }

    @Test
    fun testIsBetween_dateBeforeRange() {
        val date = LocalDate.of(2026, 4, 19)
        assertFalse(date.isBetween(startDate, endDate))
    }

    @Test
    fun testIsBetween_dateAfterRange() {
        val date = LocalDate.of(2026, 5, 1)
        assertFalse(date.isBetween(startDate, endDate))
    }
}
