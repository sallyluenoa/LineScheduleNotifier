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

import java.time.LocalDate
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
    fun testFetchDataByRange_push() {
        val result = dataSource.fetchDataByRange("push")
        val expected = listOf(
            listOf("to"), // Header
            listOf("U_MOCK_ID_1"),
            listOf("U_MOCK_ID_2")
        )
        assertEquals(expected, result)
    }

    @Test
    fun testFetchDataByRange_webhook() {
        val result = dataSource.fetchDataByRange("webhook")
        val expected = listOf(
            listOf("This is a mock reply message.")
        )
        assertEquals(expected, result)
    }

    @Test
    fun testFetchDataByRange_schedule() {
        val result = dataSource.fetchDataByRange("schedule")
        // Check if all rows have 5 columns
        assertTrue(result.all { it.size == 5 })
        // Check header row
        assertEquals(
            listOf("Date", "Day of the Week", "Period", "Events & Schedule", "Items to Bring & Assignments"),
            result[0]
        )
        // Check if today's data is included
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        assertTrue(result.any { it[0] == today && it[1] == "Today" })
    }

    @Test
    fun testFetchDataByRange_unknown() {
        val result = dataSource.fetchDataByRange("unknown_range")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFetchMonthlyData() {
        val yearMonth = YearMonth.now()
        val result = dataSource.fetchMonthlyData(yearMonth)
        // This should return the same as "schedule" for the mock.
        // Check if all rows have 5 columns
        assertTrue(result.all { it.size == 5 })
        // Check header row
        assertEquals(
            listOf("Date", "Day of the Week", "Period", "Events & Schedule", "Items to Bring & Assignments"),
            result[0]
        )
        // Check if today's data is included
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        assertTrue(result.any { it[0] == today && it[1] == "Today" })
    }
}
