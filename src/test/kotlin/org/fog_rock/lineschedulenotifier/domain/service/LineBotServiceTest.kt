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

import io.mockk.clearAllMocks
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
import org.fog_rock.lineschedulenotifier.domain.provider.WeeklyScheduleProvider
import org.fog_rock.lineschedulenotifier.domain.repository.ApplicationDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LineBotServiceTest {

    private lateinit var appDataSource: ApplicationDataSource
    private lateinit var weeklyScheduleProvider: WeeklyScheduleProvider
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
        lineClient = mockk(relaxed = true)
        verifier = mockk(relaxed = true) {
            every { verify(any(), any()) } returns true
        }
        service = LineBotService(appDataSource, weeklyScheduleProvider, lineClient, verifier)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    private fun createWebhookJson(vararg events: LineWebhookEvent.Event): String {
        val webhook = LineWebhookEvent(botId, events.toList())
        return json.encodeToString(webhook)
    }

    private fun createMessageEvent(
        sourceType: SourceType = SourceType.USER,
        messageType: MessageType = MessageType.TEXT,
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
            text = "hello",
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

    @Test
    fun testHandleWebhook_replyToUserMessage() {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.USER)
        val body = createWebhookJson(event)
        every { appDataSource.fetchDataByRange("webhook") } returns listOf(listOf("Reply Message"))

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify { lineClient.reply("replyToken", "Reply Message") }
    }

    @Test
    fun testHandleWebhook_replyToGroupMention() {
        // Arrange
        val mentionees = listOf(LineWebhookEvent.Mentionee(0, 5, botId))
        val event = createMessageEvent(sourceType = SourceType.GROUP, mentionees = mentionees)
        val body = createWebhookJson(event)
        every { appDataSource.fetchDataByRange("webhook") } returns listOf(listOf("Reply Message"))

        // Act
        service.handleWebhook(body, signature)

        // Assert
        verify { lineClient.reply("replyToken", "Reply Message") }
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
}
