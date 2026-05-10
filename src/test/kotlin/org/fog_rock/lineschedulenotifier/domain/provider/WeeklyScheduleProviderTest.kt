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
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.time.YearMonth
import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.fog_rock.lineschedulenotifier.domain.datasource.ScheduleDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId

class WeeklyScheduleProviderTest {

    private lateinit var scheduleDataSource: ScheduleDataSource
    private lateinit var messageProvider: MessageProvider
    private lateinit var weeklyScheduleProvider: WeeklyScheduleProvider

    private val today = LocalDate.of(2026, 4, 15) // Wednesday
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    @BeforeEach
    fun setup() {
        // Mock LocalDate.now()
        mockkStatic(LocalDate::class)
        every { LocalDate.now(ZoneId.of("Asia/Tokyo")) } returns today

        scheduleDataSource = mockk()
        messageProvider = mockk(relaxed = true) {
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_TITLE) } returns "Weekly Schedule"
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_NONE) } returns "None"
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_EVENTS, any()) } answers {
                "Events: ${(invocation.args[1] as Array<*>)[0]}"
            }
            every { getMessage(MessageKeys.SCHEDULE_WEEKLY_ITEMS, any()) } answers {
                "Items: ${(invocation.args[1] as Array<*>)[0]}"
            }
        }

        weeklyScheduleProvider = WeeklyScheduleProvider(scheduleDataSource, messageProvider)
    }

    @Test
    fun testProvideMessage_singleEvent() {
        // Arrange
        val scheduleDate = today.plusDays(2) // 2026-04-17
        val data = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf(
                scheduleDate.format(dateFormatter),
                scheduleDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "1",
                "Test Event",
                "Test Item"
            )
        )
        every { scheduleDataSource.fetchMonthlyData(YearMonth.from(today)) } returns data

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        val expected = """
            Weekly Schedule

            [4/17(Fri) 1]
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
            listOf(
                aprilDate.format(dateFormatter),
                aprilDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "2",
                "April Event",
                "April Item"
            )
        )
        val mayData = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf(
                mayDate.format(dateFormatter),
                mayDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "3",
                "May Event",
                "May Item"
            )
        )
        every { LocalDate.now(ZoneId.of("Asia/Tokyo")) } returns aprilDate.minusDays(2) // Set today to 2026-04-28 to cross month

        every { scheduleDataSource.fetchMonthlyData(YearMonth.of(2026, 4)) } returns aprilData
        every { scheduleDataSource.fetchMonthlyData(YearMonth.of(2026, 5)) } returns mayData

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
        every { scheduleDataSource.fetchMonthlyData(any()) } returns listOf(listOf("Header"))

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
            listOf(
                scheduleDate.format(dateFormatter),
                scheduleDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "4",
                "",
                ""
            )
        )
        every { scheduleDataSource.fetchMonthlyData(YearMonth.from(today)) } returns data

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        assertNull(result)
    }

    @Test
    fun testProvideMessage_invalidDateFormat() {
        // Arrange
        val scheduleDate = today.plusDays(4)
        val invalidFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val data = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            listOf(
                scheduleDate.format(invalidFormatter),
                scheduleDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "5",
                "Invalid Date Event",
                "Item"
            )
        )
        every { scheduleDataSource.fetchMonthlyData(YearMonth.from(today)) } returns data

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        assertNull(result)
    }

    @Test
    fun testProvideMessage_filtersEventsToWeeklyWindow() {
        // Arrange
        // today is 2026-04-15 (Wed). The window is from 2026-04-16 (Thu) to 2026-04-22 (Wed).
        val beforeDate = today // Day before window: 2026-04-15
        val startWindowDate = today.plusDays(1) // First day of window: 2026-04-16
        val endWindowDate = today.plusDays(7) // Last day of window: 2026-04-22
        val afterDate = today.plusDays(8) // Day after window: 2026-04-23

        val data = listOf(
            listOf("Date", "Day of Week", "Period", "Events", "Items"),
            // Event before window
            listOf(
                beforeDate.format(dateFormatter),
                beforeDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "B", "Before Event", "Before Item"
            ),
            // Event at start of window
            listOf(
                startWindowDate.format(dateFormatter),
                startWindowDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "S", "Start Event", "Start Item"
            ),
            // Event at end of window
            listOf(
                endWindowDate.format(dateFormatter),
                endWindowDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "E", "End Event", "End Item"
            ),
            // Event after window
            listOf(
                afterDate.format(dateFormatter),
                afterDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "A", "After Event", "After Item"
            )
        )
        every { scheduleDataSource.fetchMonthlyData(YearMonth.from(today)) } returns data

        // Act
        val result = weeklyScheduleProvider.provideMessage()

        // Assert
        val expected = """
            Weekly Schedule

            [4/16(Thu) S]
            Events: Start Event
            Items: Start Item

            [4/22(Wed) E]
            Events: End Event
            Items: End Item
            """
        .trimIndent()
        assertEquals(expected, result?.trim())
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocalDate::class)
    }
}
