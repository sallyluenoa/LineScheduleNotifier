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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.fog_rock.frlineagent.core.domain.model.webhook.EventType
import org.fog_rock.frlineagent.core.domain.model.webhook.LineWebhookEvent
import org.fog_rock.frlineagent.core.domain.model.webhook.MessageType
import org.fog_rock.frlineagent.core.domain.model.webhook.SourceType
import org.fog_rock.frlineagent.core.domain.service.LineClient
import org.fog_rock.frlineagent.core.domain.service.SignatureVerifier
import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.fog_rock.lineschedulenotifier.domain.provider.WeeklyScheduleProvider
import org.fog_rock.lineschedulenotifier.domain.repository.ApplicationDataSource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LineBotServiceTest {

    private lateinit var appDataSource: ApplicationDataSource
    private lateinit var weeklyScheduleProvider: WeeklyScheduleProvider
    private lateinit var messageProvider: MessageProvider
    private lateinit var lineClient: LineClient
    private lateinit var verifier: SignatureVerifier
    private lateinit var service: LineBotService

    private val botId = "U_BOT_ID"
    private val signature = "signature"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        appDataSource = mockk(relaxed = true)
        weeklyScheduleProvider = mockk(relaxed = true)
        messageProvider = mockk(relaxed = true)
        lineClient = mockk(relaxed = true)
        verifier = mockk(relaxed = true) {
            every { verify(any(), any()) } returns true
        }
        service = LineBotService(appDataSource, messageProvider, weeklyScheduleProvider, lineClient, verifier)
    }
    
    @Test
    fun testHandleWebhook_noReplyOnBlankMessage() {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.USER, text = "   ")
        val body = createWebhookJson(event)

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(exactly = 0) { lineClient.reply(any(), any()) }
    }

    @Test
    fun testHandleWebhook_replyWithUnknownCommand() {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.USER, text = "some unknown command")
        val body = createWebhookJson(event)
        val expectedReply = "Sorry, I don't understand that command."
        every { messageProvider.getMessage(MessageKeys.REPLY_UNKNOWN_COMMAND) } returns expectedReply

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(timeout = 5000) { lineClient.reply("replyToken", expectedReply) }
    }

    @Test
    fun testHandleWebhook_replyToGroupMentionWithUnknownCommand() {
        // Arrange
        val mentionees = listOf(LineWebhookEvent.Mentionee(0, 5, botId))
        val event = createMessageEvent(sourceType = SourceType.GROUP, text = "some unknown command", mentionees = mentionees)
        val body = createWebhookJson(event)
        val expectedReply = "Sorry, I don't understand that command."
        every { messageProvider.getMessage(MessageKeys.REPLY_UNKNOWN_COMMAND) } returns expectedReply


        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(timeout = 5000) { lineClient.reply("replyToken", expectedReply) }
    }

    @Test
    fun testHandleWebhook_noReplyOnNonTextMessage() {
        // Arrange
        val event = createMessageEvent(messageType = MessageType.IMAGE)
        val body = createWebhookJson(event)

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(exactly = 0) { lineClient.reply(any(), any()) }
    }

    @Test
    fun testHandleWebhook_noReplyOnGroupMessageWithoutMention() {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.GROUP)
        val body = createWebhookJson(event)

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(exactly = 0) { lineClient.reply(any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["user id", "user_id", "my id", "ユーザーID"])
    fun testHandleWebhook_replyWithUserId(keyword: String) {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.USER, text = keyword)
        val body = createWebhookJson(event)
        val expectedReply = "Your User ID is U_USER_ID."
        every { messageProvider.getMessage(MessageKeys.REPLY_USER_ID, "U_USER_ID") } returns expectedReply

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(timeout = 5000) { lineClient.reply("replyToken", expectedReply) }
    }

    @Test
    fun testHandleWebhook_replyWithGroupId() {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.GROUP, text = "group_id", mentionees = listOf(LineWebhookEvent.Mentionee(0, 5, botId)))
        val body = createWebhookJson(event)
        val expectedReply = "Your Group ID is G_GROUP_ID."
        every { messageProvider.getMessage(MessageKeys.REPLY_GROUP_ID, "G_GROUP_ID") } returns expectedReply

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(timeout = 5000) { lineClient.reply("replyToken", expectedReply) }
    }

    @Test
    fun testHandleWebhook_replyWithSchedule() {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.USER, text = "schedule")
        val body = createWebhookJson(event)
        val expectedReply = "This is the schedule."
        every { weeklyScheduleProvider.provideMessage() } returns expectedReply

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify(timeout = 5000) { lineClient.reply("replyToken", expectedReply) }
    }

    @Test
    fun testExecutePush_pushNotifications() {
        // Arrange
        every { appDataSource.fetchDataByRange("push") } returns listOf(listOf("header"), listOf("user1"), listOf("user2"))
        every { weeklyScheduleProvider.provideMessage() } returns "Weekly Schedule"
        every { lineClient.push(any(), any()) } returns Result.success(Unit)

        // Act
        service.executePush()

        // Assert
        verify { lineClient.push("user1", "Weekly Schedule") }
        verify { lineClient.push("user2", "Weekly Schedule") }
    }

    @Test
    fun testExecutePush_noRecipients() {
        // Arrange
        every { appDataSource.fetchDataByRange("push") } returns listOf(listOf("header"))
        every { weeklyScheduleProvider.provideMessage() } returns "Weekly Schedule"

        // Act
        service.executePush()

        // Assert
        verify(exactly = 0) { lineClient.push(any(), any()) }
    }

    @Test
    fun testExecutePush_nullMessage() {
        // Arrange
        every { appDataSource.fetchDataByRange("push") } returns listOf(listOf("header"), listOf("user1"))
        every { weeklyScheduleProvider.provideMessage() } returns null

        // Act
        service.executePush()

        // Assert
        verify(exactly = 0) { lineClient.push(any(), any()) }
    }

    private fun createWebhookJson(vararg events: LineWebhookEvent.Event): String {
        val webhook = LineWebhookEvent(botId, events.toList())
        return json.encodeToString(webhook)
    }

    private fun createMessageEvent(
        sourceType: SourceType = SourceType.USER,
        messageType: MessageType = MessageType.TEXT,
        text: String = "hello",
        mentionees: List<LineWebhookEvent.Mentionee> = emptyList()
    ): LineWebhookEvent.Event {
        val source = LineWebhookEvent.Source(
            _type = sourceType.value,
            userId = "U_USER_ID",
            groupId = if (sourceType == SourceType.GROUP) "G_GROUP_ID" else null
        )
        val message = LineWebhookEvent.Message(
            id = "msg1",
            _type = messageType.value,
            text = text,
            mention = if (mentionees.isNotEmpty()) LineWebhookEvent.Mention(mentionees) else null
        )
        return LineWebhookEvent.Event(
            _type = EventType.MESSAGE.value,
            replyToken = "replyToken",
            source = source,
            timestamp = 1234567890,
            mode = "active",
            webhookEventId = "webhookEventId",
            deliveryContext = LineWebhookEvent.DeliveryContext(false),
            message = message
        )
    }
}
