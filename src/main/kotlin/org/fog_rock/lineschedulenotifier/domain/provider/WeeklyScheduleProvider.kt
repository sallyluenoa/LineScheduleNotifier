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

import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.fog_rock.lineschedulenotifier.domain.repository.ScheduleDataSource
import org.fog_rock.lineschedulenotifier.extension.isBetween
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * A provider class that generates a weekly schedule message.
 */
class WeeklyScheduleProvider(
    private val scheduleRepo: ScheduleDataSource,
    private val messageProvider: MessageProvider,
) {
    private val logger = LoggerFactory.getLogger(WeeklyScheduleProvider::class.java)

    companion object {
        // Date format used in the spreadsheet
        private const val SHEET_DATE_FORMAT = "yyyy/MM/dd"
        // Date format used in the message
        private const val MSG_DATE_FORMAT = "M/d"

        // Column indices for the schedule sheet.
        private const val COL_SCHEDULE_DATE = 0
        private const val COL_SCHEDULE_DAY_OF_WEEK = 1
        private const val COL_SCHEDULE_PERIOD = 2
        private const val COL_SCHEDULE_EVENTS = 3
        private const val COL_SCHEDULE_ITEMS = 4
    }

    /**
     * Provide a weekly schedule message.
     * @return A formatted message string, or null if no schedule is available.
     */
    fun provideMessage(): String? {
        val today = LocalDate.now()
        val startDate = today.plusDays(1)
        val endDate = today.plusWeeks(1)

        val sheetData = fetchWeeklySheetData(startDate, endDate)
        if (sheetData.size <= 1) { // Needs at least a header and one data row
            logger.info("No schedule data or only header found in sheet.")
            return null
        }

        val sheetDateFormatter = DateTimeFormatter.ofPattern(SHEET_DATE_FORMAT)
        val scheduleRows = filterAndSortWeeklySchedule(sheetData, startDate, endDate, sheetDateFormatter)
        if (scheduleRows.isEmpty()) {
            logger.info("No schedule found for the upcoming week.")
            return null
        }

        return buildScheduleMessage(scheduleRows)
    }

    private fun fetchWeeklySheetData(startDate: LocalDate, endDate: LocalDate): List<List<Any>> {
        val startYearMonth = YearMonth.from(startDate)
        val endYearMonth = YearMonth.from(endDate)

        val sheetData = mutableListOf<List<Any>>()

        if (startYearMonth == endYearMonth) {
            // The entire week is in the same month.
            sheetData.addAll(scheduleRepo.fetchMonthlyData(startYearMonth))
        } else {
            // The week spans across two months.
            val startMonthData = scheduleRepo.fetchMonthlyData(startYearMonth)
            val endMonthData = scheduleRepo.fetchMonthlyData(endYearMonth)
            sheetData.addAll(startMonthData)
            if (sheetData.isNotEmpty() && endMonthData.isNotEmpty()) {
                sheetData.addAll(endMonthData.drop(1)) // Exclude header
            } else {
                sheetData.addAll(endMonthData)
            }
        }
        return sheetData
    }

    private fun filterAndSortWeeklySchedule(
        sheetData: List<List<Any>>,
        startDate: LocalDate,
        endDate: LocalDate,
        formatter: DateTimeFormatter
    ): List<Pair<LocalDate, List<Any>>> = sheetData.drop(1).mapNotNull { row ->
        val dateStr = row.getOrNull(COL_SCHEDULE_DATE)?.toString().orEmpty()
        if (dateStr.isBlank()) {
            return@mapNotNull null
        }
        val date = try {
            LocalDate.parse(dateStr, formatter)
        } catch (e: DateTimeParseException) {
            logger.warn("Failed to parse date from row: $dateStr", e)
            return@mapNotNull null
        }
        if (date.isBetween(startDate, endDate)) {
            date to row // Pair date and row for sorting
        } else {
            null
        }
    }.sortedBy { it.first }

    private fun buildScheduleMessage(scheduleRows: List<Pair<LocalDate, List<Any>>>): String {
        val messageDateFormatter = DateTimeFormatter.ofPattern(MSG_DATE_FORMAT)
        val message = StringBuilder()
        message.append(messageProvider.getMessage(MessageKeys.SCHEDULE_WEEKLY_TITLE))
        message.append("\n\n")

        scheduleRows.forEach { (date, row) ->
            val events = row.getOrNull(COL_SCHEDULE_EVENTS)?.toString().orEmpty()
            val items = row.getOrNull(COL_SCHEDULE_ITEMS)?.toString().orEmpty()

            // Skip if both events and items are blank
            if (events.isBlank() && items.isBlank()) {
                return@forEach
            }

            val dateStr = date.format(messageDateFormatter)
            val dayOfWeek = row.getOrNull(COL_SCHEDULE_DAY_OF_WEEK)?.toString().orEmpty()
            val period = row.getOrNull(COL_SCHEDULE_PERIOD)?.toString().orEmpty()

            message.append("[$dateStr($dayOfWeek) $period]\n")
            val eventMessage = messageProvider.getMessage(
                MessageKeys.SCHEDULE_WEEKLY_EVENTS,
                events.ifBlank { messageProvider.getMessage(MessageKeys.SCHEDULE_WEEKLY_NONE) }
            )
            message.append(eventMessage)
            message.append("\n")
            if (items.isNotBlank()) {
                message.append(messageProvider.getMessage(MessageKeys.SCHEDULE_WEEKLY_ITEMS, items))
                message.append("\n")
            }
            message.append("\n")
        }

        return message.toString().trim()
    }
}
