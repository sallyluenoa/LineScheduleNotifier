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

package org.fog_rock.lineschedulenotifier.domain.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import org.fog_rock.frlineagent.core.domain.model.push.Notification
import org.fog_rock.frlineagent.core.domain.model.webhook.EventType
import org.fog_rock.frlineagent.core.domain.model.webhook.LineWebhookEvent
import org.fog_rock.frlineagent.core.domain.model.webhook.MessageType
import org.fog_rock.frlineagent.core.domain.model.webhook.SourceType
import org.fog_rock.frlineagent.core.domain.service.AbstractLineBotService
import org.fog_rock.frlineagent.core.domain.service.LineClient
import org.fog_rock.frlineagent.core.domain.service.SignatureVerifier
import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.fog_rock.lineschedulenotifier.domain.repository.SheetsRepository
import org.fog_rock.lineschedulenotifier.extension.isBetween
import org.slf4j.LoggerFactory

/**
 * Service class for handling LINE Bot operations.
 */
class LineBotService(
    private val sheetsRepo: SheetsRepository,
    private val messageProvider: MessageProvider,
    lineClient: LineClient,
    verifier: SignatureVerifier
) : AbstractLineBotService(lineClient, verifier) {
    private val logger = LoggerFactory.getLogger(LineBotService::class.java)

    companion object {
        // Default range for webhook data retrieval
        private const val SHEET_RANGE_WEBHOOK = "webhook"
        // Default range for scheduled push notifications (To, Message)
        private const val SHEET_RANGE_PUSH = "push"
        // Default range for schedule data
        private const val SHEET_RANGE_SCHEDULE = "schedule"

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

    override fun createReplyMessage(event: LineWebhookEvent.Event, botId: String): String? {
        if (!shouldReply(event, botId)) {
            logger.info("Should not reply to the event.")
            return null
        }

        // Request Data from Sheets
        val sheetData = sheetsRepo.fetchSheetData(SHEET_RANGE_WEBHOOK)
        if (sheetData.isEmpty() || sheetData[0].isEmpty()) {
            logger.info("No reply message found in sheet.")
            return null
        }

        // Just return the message string. The base class will send it.
        return sheetData[0][0].toString()
    }

    override fun createPushNotifications(): List<Notification> {
        // Fetch recipient list
        val recipients = fetchRecipients()
        if (recipients.isEmpty()) {
            logger.info("No recipients found.")
            return emptyList()
        }

        // Get weekly schedule message
        val message = createWeeklyScheduleMessage()
        if (message.isNullOrBlank()) {
            logger.info("No schedule for the upcoming week or message is blank.")
            return emptyList()
        }

        // Send the message to each recipient
        return recipients.map { to ->
            Notification(to = to, message = message)
        }
    }

    private fun fetchRecipients(): List<String> {
        val sheetData = sheetsRepo.fetchSheetData(SHEET_RANGE_PUSH)
        if (sheetData.size <= 1) { // Check for header
            logger.info("No recipient data or only header found in sheet.")
            return emptyList()
        }
        // Skip header row and map to recipient ID
        return sheetData.drop(1).mapNotNull { row ->
            if (row.isNotEmpty() && row[0].toString().isNotBlank()) {
                row[0].toString()
            } else {
                null
            }
        }
    }

    private fun createWeeklyScheduleMessage(): String? {
        val sheetData = sheetsRepo.fetchSheetData(SHEET_RANGE_SCHEDULE)
        if (sheetData.size <= 1) { // Needs at least a header and one data row
            logger.info("No schedule data or only header found in sheet.")
            return null
        }

        val today = LocalDate.now()
        val startDate = today.plusDays(1)
        val endDate = today.plusWeeks(1)
        val sheetDateFormatter = DateTimeFormatter.ofPattern(SHEET_DATE_FORMAT)

        // Find and sort schedules for the upcoming week
        val scheduleRows = sheetData.drop(1).mapNotNull { row ->
            val dateStr = row.getOrNull(COL_SCHEDULE_DATE)?.toString().orEmpty()
            if (dateStr.isBlank()) {
                return@mapNotNull null
            }
            val date = try {
                 LocalDate.parse(dateStr, sheetDateFormatter)
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

        if (scheduleRows.isEmpty()) {
            logger.info("No schedule found for the upcoming week.")
            return null
        }

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

    private fun shouldReply(event: LineWebhookEvent.Event, botId: String): Boolean {
        if (event.eventType != EventType.MESSAGE) {
            logger.info("The event type is not message. eventType: ${event.eventType}")
            return false
        }
        val message = event.message
        if (message?.messageType != MessageType.TEXT) {
            logger.info("The message type is not text. messageType: ${message?.messageType}")
            return false
        }
        val source = event.source
        logger.info("sourceType: ${source?.sourceType}")
        return when (source?.sourceType) {
            SourceType.USER -> {
                logger.info("Source type is USER. Replying.")
                true
            }
            SourceType.GROUP -> {
                val shouldReplyToGroup = message.mention?.mentionees?.any { it.userId == botId } ?: false
                logger.info("Source type is GROUP. Bot mentioned: $shouldReplyToGroup")
                shouldReplyToGroup
            }
            else -> {
                logger.info("Source type is ${source?.sourceType}. Not replying.")
                false
            }
        }
    }
}
