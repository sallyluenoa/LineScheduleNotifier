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

import org.fog_rock.lineschedulenotifier.domain.datasource.ApplicationDataSource
import org.fog_rock.lineschedulenotifier.domain.datasource.ScheduleDataSource
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal class MockDataSource : ScheduleDataSource, ApplicationDataSource {
    private val logger = LoggerFactory.getLogger(MockDataSource::class.java)

    override fun fetchDataByRange(range: String): List<List<Any>> {
        logger.info("Mock fetchDataByRange called with range: $range")
        return getMockDataForRange(range)
    }

    override fun fetchMonthlyData(yearMonth: YearMonth): List<List<Any>> {
        logger.info("Mock fetchMonthlyData called with yearMonth: $yearMonth")
        return generateMonthlySchedule(yearMonth)
    }

    private fun getMockDataForRange(range: String): List<List<Any>> = when (range) {
        "push" -> listOf(
            listOf("to"), // Header
            listOf("U_MOCK_ID_1"),
            listOf("U_MOCK_ID_2")
        )
        "webhook" -> listOf(
            listOf("This is a mock reply message.")
        )
        else -> {
            logger.warn("Unexpected range for MockDataSource: $range")
            emptyList()
        }
    }

    private fun generateMonthlySchedule(yearMonth: YearMonth): List<List<Any>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val header = listOf("Date", "Day of the Week", "Period", "Events & Schedule", "Items to Bring & Assignments")

        val monthlyData = mutableListOf<List<Any>>()
        monthlyData.add(header)

        val daysInMonth = yearMonth.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            monthlyData.add(
                listOf(
                    date.format(formatter),
                    dayOfWeek,
                    "Mock Period",
                    "Mock Event for ${date.monthValue}/${date.dayOfMonth}",
                    "Mock Item for ${date.monthValue}/${date.dayOfMonth}"
                )
            )
        }
        return monthlyData
    }
}
