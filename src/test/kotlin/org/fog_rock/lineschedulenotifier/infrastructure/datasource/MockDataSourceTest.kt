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

package org.fog_rock.lineschedulenotifier.infrastructure.datasource

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MockDataSourceTest {

    private lateinit var dataSource: MockDataSource

    @BeforeEach
    fun setUp() {
        dataSource = MockDataSource()
    }

    @Test
    fun testFetchDataByKey_push() {
        val result = dataSource.fetchDataByKey("mock_key", "push")
        val expected = listOf(
            listOf("to"), // Header
            listOf("U_MOCK_ID_1"),
            listOf("U_MOCK_ID_2")
        )
        assertEquals(expected, result)
    }

    @Test
    fun testFetchDataByKey_webhook() {
        val result = dataSource.fetchDataByKey("mock_key", "webhook")
        val expected = listOf(
            listOf("This is a mock reply message.")
        )
        assertEquals(expected, result)
    }

    @Test
    fun testFetchDataByKey_unknown() {
        val result = dataSource.fetchDataByKey("mock_key", "unknown_range")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFetchMonthlyData_for30DayMonth() {
        assertMonthlyData(YearMonth.of(2026, 4), 30) // April
    }

    @Test
    fun testFetchMonthlyData_for31DayMonth() {
        assertMonthlyData(YearMonth.of(2026, 5), 31) // May
    }

    @Test
    fun testFetchMonthlyData_forFebruary() {
        assertMonthlyData(YearMonth.of(2026, 2), 28) // February
    }

    @Test
    fun testFetchMonthlyData_forLeapFebruary() {
        assertMonthlyData(YearMonth.of(2024, 2), 29) // Leap February
    }

    private fun assertMonthlyData(yearMonth: YearMonth, expectedDays: Int) {
        val result = dataSource.fetchMonthlyData(yearMonth)

        // Check if all rows have 5 columns
        assertTrue(result.all { it.size == 5 })

        // Check header row
        assertEquals(
            listOf("Date", "Day of the Week", "Period", "Events & Schedule", "Items to Bring & Assignments"),
            result[0]
        )

        // Check data size (header + days of month)
        assertEquals(1 + expectedDays, result.size)

        // Check first and last day
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        assertEquals(yearMonth.atDay(1).format(formatter), result[1][0])
        assertEquals(yearMonth.atDay(expectedDays).format(formatter), result.last()[0])
    }
}
