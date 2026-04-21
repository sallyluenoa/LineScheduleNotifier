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

package org.fog_rock.lineschedulenotifier.domain.provider

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.time.LocalDate
import java.time.YearMonth
import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.fog_rock.lineschedulenotifier.domain.repository.ScheduleDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WeeklyScheduleProviderTest {

    private lateinit var scheduleRepo: ScheduleDataSource
    private lateinit var messageProvider: MessageProvider
    private lateinit var weeklyScheduleProvider: WeeklyScheduleProvider

    private val today = LocalDate.of(2026, 4, 21) // Tuesday

    @BeforeEach
    fun setup() {
        // Mock LocalDate.now()
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns today

        scheduleRepo = mockk()
        messageProvider = mockk(relaxed = true) {
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_TITLE) } returns "Weekly Schedule"
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_NONE) } returns "None"
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_EVENTS, any()) } answers { "Events: ${firstArg<String>()}" }
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_ITEMS, any()) } answers { "Items: ${firstArg<String>()}" }
        }

        weeklyScheduleProvider = WeeklyScheduleProvider(scheduleRepo, messageProvider)
    }

    @Test
    fun testProvideMessage_singleEvent() {
        // Arrange
        val scheduleDate = today.plusDays(2) // 2026-04-23
        val data = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf(scheduleDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")), "Thu", "1", "Test Event", "Test Item")
        )
        every { scheduleRepo.fetchMonthlyData(YearMonth.from(today)) } returns data

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        val expected = """
            Weekly Schedule

            [4/23(Thu) 1]
            Events: Test Event
            Items: Test Item
            """.trimIndent()
        assertEquals(expected, result?.trim())
    }

    @Test
    fun testProvideMessage_multipleEvents_acrossMonths() {
        // Arrange
        val aprilDate = LocalDate.of(2026, 4, 30) // Thursday
        val mayDate = LocalDate.of(2026, 5, 1) // Friday
        val aprilData = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf("2026/04/30", "Thu", "2", "April Event", "April Item")
        )
        val mayData = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf("2026/05/01", "Fri", "3", "May Event", "May Item")
        )
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns aprilDate.minusDays(2) // Set today to 2026-04-28 to cross month

        every { scheduleRepo.fetchMonthlyData(YearMonth.of(2026, 4)) } returns aprilData
        every { scheduleRepo.fetchMonthlyData(YearMonth.of(2026, 5)) } returns mayData

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        val expected = """
            Weekly Schedule

            [4/30(Thu) 2]
            Events: April Event
            Items: April Item

            [5/1(Fri) 3]
            Events: May Event
            Items: May Item
            """.trimIndent()
        assertEquals(expected, result?.trim())
    }

    @Test
    fun testProvideMessage_noEvents() {
        // Arrange
        every { scheduleRepo.fetchMonthlyData(any()) } returns listOf(listOf("Header"))

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        assertNull(result)
    }

    @Test
    fun testProvideMessage_eventAndItemAreBlank() {
        // Arrange
        val scheduleDate = today.plusDays(3)
        val data = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf(scheduleDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")), "Fri", "4", "", "")
        )
        every { scheduleRepo.fetchMonthlyData(YearMonth.from(today)) } returns data

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        assertNull(result)
    }

    @Test
    fun testProvideMessage_invalidDateFormat() {
        // Arrange
        val data = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf("2026-04-24", "Fri", "5", "Invalid Date Event", "Item")
        )
        every { scheduleRepo.fetchMonthlyData(YearMonth.from(today)) } returns data

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        assertNull(result)
    }
}
