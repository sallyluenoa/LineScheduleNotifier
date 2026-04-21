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
import org.fog_rock.lineschedulenotifier.domain.repository.ApplicationDataSource
import org.fog_rock.lineschedulenotifier.domain.repository.ScheduleDataSource
import org.slf4j.LoggerFactory

internal class MockDataSource : ScheduleDataSource, ApplicationDataSource {
    private val logger = LoggerFactory.getLogger(MockDataSource::class.java)

    override fun fetchDataByRange(range: String): List<List<Any>> {
        logger.info("Mock fetchDataByRange called with range: $range")
        return getMockDataForRange(range)
    }

    override fun fetchMonthlyData(yearMonth: YearMonth): List<List<Any>> {
        logger.info("Mock fetchMonthlyData called with yearMonth: $yearMonth")
        return getMockDataForRange("schedule")
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
        "schedule" -> {
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            listOf(
                listOf("Date", "Day of the Week", "Period", "Events & Schedule", "Items to Bring & Assignments"),
                listOf(today.minusDays(1).format(formatter), "Yesterday", "1", "Past Event", "Past Item"),
                listOf(today.format(formatter), "Today", "2", "Today's Event", "Today's Item"),
                listOf(today.plusDays(1).format(formatter), "Tomorrow", "3", "Future Event", "Future Item")
            )
        }
        else -> {
            logger.warn("Unexpected range for MockDataSource: $range")
            emptyList()
        }
    }
}
