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
import org.fog_rock.lineschedulenotifier.domain.repository.SheetsRepository
import org.slf4j.LoggerFactory

/**
 * Service class for handling LINE Bot operations.
 */
class LineBotService(
    private val sheetsRepo: SheetsRepository,
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

        // Get today's schedule message
        val message = createTodayScheduleMessage()
        if (message.isNullOrBlank()) {
            logger.info("No schedule for today or message is blank.")
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

    private fun createTodayScheduleMessage(): String? {
        val sheetData = sheetsRepo.fetchSheetData(SHEET_RANGE_SCHEDULE)
        if (sheetData.size <= 1) { // Needs at least a header and one data row
            logger.info("No schedule data or only header found in sheet.")
            return null
        }

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

        // Find schedule for today
        val scheduleRow = sheetData.drop(1).find { row ->
            if (row.isEmpty() || row[0].toString().isBlank()) return@find false
            try {
                val date = LocalDate.parse(row[0].toString(), formatter)
                date.isEqual(today)
            } catch (e: DateTimeParseException) {
                logger.warn("Failed to parse date: ${row[0]}", e)
                false
            }
        }

        return scheduleRow?.let {
            if (it.size < 5) {
                logger.warn("Schedule row has fewer than 5 columns: $it")
                return@let null
            }
            // "Date, Day of the Week, Period, Events & Schedule, Items to Bring & Assignments"
            val date = it.getOrNull(0)?.toString() ?: ""
            val dayOfWeek = it.getOrNull(1)?.toString() ?: ""
            val period = it.getOrNull(2)?.toString() ?: ""
            val events = it.getOrNull(3)?.toString() ?: ""
            val items = it.getOrNull(4)?.toString() ?: ""

            """
            [Today's Schedule]
            Date: $date ($dayOfWeek)
            Period: $period
            Events & Schedule: $events
            Items to Bring & Assignments: $items
            """.trimIndent()
        }
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
