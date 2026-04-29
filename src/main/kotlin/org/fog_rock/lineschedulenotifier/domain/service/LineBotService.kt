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

import org.fog_rock.frlineagent.core.domain.model.push.Notification
import org.fog_rock.frlineagent.core.domain.model.webhook.EventType
import org.fog_rock.frlineagent.core.domain.model.webhook.LineWebhookEvent
import org.fog_rock.frlineagent.core.domain.model.webhook.MessageType
import org.fog_rock.frlineagent.core.domain.model.webhook.SourceType
import org.fog_rock.frlineagent.core.domain.service.AbstractLineBotService
import org.fog_rock.frlineagent.core.domain.service.LineClient
import org.fog_rock.frlineagent.core.domain.service.SignatureVerifier
import org.fog_rock.lineschedulenotifier.domain.config.AppConfig
import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.fog_rock.lineschedulenotifier.domain.provider.GeneralInfoProvider
import org.fog_rock.lineschedulenotifier.domain.provider.WeeklyScheduleProvider
import org.fog_rock.lineschedulenotifier.domain.datasource.ApplicationDataSource
import org.fog_rock.lineschedulenotifier.domain.service.common.ReplyTrigger
import org.slf4j.LoggerFactory

/**
 * Service class for handling LINE Bot operations.
 */
class LineBotService(
    private val config: AppConfig,
    private val appDataSource: ApplicationDataSource,
    private val messageProvider: MessageProvider,
    private val weeklyScheduleProvider: WeeklyScheduleProvider,
    private val generalInfoProvider: GeneralInfoProvider,
    lineClient: LineClient,
    verifier: SignatureVerifier
) : AbstractLineBotService(lineClient, verifier) {

    private val logger = LoggerFactory.getLogger(LineBotService::class.java)

    companion object {
        // Default range for scheduled push notifications (To, Message)
        private const val SHEET_RANGE_PUSH = "push"
    }

    override fun createReplyMessage(event: LineWebhookEvent.Event, botId: String): String? {
        val source = shouldReply(event, botId) ?: run {
            logger.info("Should not reply to the event.")
            return null
        }

        // `shouldReply` ensures that `event.message` is a non-null text message.
        val messageText = event.message?.text
        if (messageText.isNullOrBlank()) {
            logger.warn("Message text is null or blank.")
            return null
        }

        val triggers = ReplyTrigger.fromAll(messageText)
        logger.info("Detected triggers: $triggers for message: '$messageText'")

        if (triggers.size > 1) {
            return messageProvider.getMessage(MessageKeys.ERROR_MULTIPLE_COMMANDS)
        }

        return when (triggers.firstOrNull()) {
            ReplyTrigger.USER_ID -> createUserIdMessage(source)
            ReplyTrigger.GROUP_ID -> createGroupIdMessage(source)
            ReplyTrigger.SCHEDULE -> weeklyScheduleProvider.provideMessage()
            ReplyTrigger.GENERAL_INFO -> generalInfoProvider.provideMessage()
            null -> messageProvider.getMessage(MessageKeys.REPLY_UNKNOWN_COMMAND)
        }
    }

    override fun createPushNotifications(): List<Notification> {
        // Fetch recipient list
        val recipients = fetchRecipients()
        if (recipients.isEmpty()) {
            logger.info("No recipients found.")
            return emptyList()
        }

        // Get weekly schedule message
        val message = weeklyScheduleProvider.provideMessage()
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
        val sheetData = appDataSource.fetchDataByKey(config.googleSheetsSpreadsheetIdKey, SHEET_RANGE_PUSH)
        if (sheetData.size <= 1) { // Check for header
            logger.info("No recipient data or only header found in sheet.")
            return emptyList()
        }
        // Skip header row and map to recipient ID
        return sheetData.drop(1).mapNotNull { it.getOrNull(0)?.toString() }
    }

    private fun shouldReply(event: LineWebhookEvent.Event, botId: String): LineWebhookEvent.Source? {
        if (event.eventType != EventType.MESSAGE) {
            logger.info("The event type is not message. eventType: ${event.eventType}")
            return null
        }
        val message = event.message
        if (message?.messageType != MessageType.TEXT) {
            logger.info("The message type is not text. messageType: ${message?.messageType}")
            return null
        }
        val source = event.source
        logger.info("sourceType: ${source?.sourceType}")
        return when (source?.sourceType) {
            SourceType.USER -> {
                logger.info("Source type is USER. Replying.")
                source
            }
            SourceType.GROUP -> {
                val shouldReplyToGroup = message.mention?.mentionees?.any { it.userId == botId } ?: false
                logger.info("Source type is GROUP. Bot mentioned: $shouldReplyToGroup")
                if (shouldReplyToGroup) source else null
            }
            else -> {
                logger.info("Source type is ${source?.sourceType}. Not replying.")
                null
            }
        }
    }

    private fun createUserIdMessage(source: LineWebhookEvent.Source): String {
        // If the user ID cannot be retrieved, output a log, return an error message, and exit the function (Guard-Clause).
        val userId = source.userId ?: run {
            logger.warn(
                "Attempted to get USER_ID in a non-user context. sourceType: ${source.sourceType}"
            )
            return messageProvider.getMessage(MessageKeys.ERROR_INVALID_CONTEXT_FOR_USER_ID)
        }
        // From here on, it is guaranteed that userId is non-null.
        return messageProvider.getMessage(MessageKeys.REPLY_USER_ID, userId)
    }

    private fun createGroupIdMessage(source: LineWebhookEvent.Source): String {
        // If the group ID cannot be retrieved, output a log, return an error message, and exit the function (Guard-Clause).
        val groupId = source.groupId ?: run {
            logger.warn(
                "Attempted to get GROUP_ID in a non-group context. sourceType: ${source.sourceType}"
            )
            return messageProvider.getMessage(MessageKeys.ERROR_INVALID_CONTEXT_FOR_GROUP_ID)
        }
        // From here on, it is guaranteed that groupId is non-null.
        return messageProvider.getMessage(MessageKeys.REPLY_GROUP_ID, groupId)
    }
}
