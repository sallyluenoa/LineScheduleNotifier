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
import org.fog_rock.lineschedulenotifier.domain.provider.WeeklyScheduleProvider
import org.fog_rock.lineschedulenotifier.domain.repository.ApplicationDataSource
import org.fog_rock.lineschedulenotifier.domain.repository.ScheduleDataSource
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LineBotServiceTest {

    private lateinit var appDataRepo: ApplicationDataSource
    private lateinit var scheduleRepo: ScheduleDataSource
    private lateinit var weeklyScheduleProvider: WeeklyScheduleProvider
    private lateinit var lineClient: LineClient
    private lateinit var verifier: SignatureVerifier
    private lateinit var service: LineBotService

    private val botId = "U_BOT_ID"
    private val signature = "signature"
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @BeforeEach
    fun setUp() {
        appDataRepo = mockk(relaxed = true)
        scheduleRepo = mockk(relaxed = true)
        weeklyScheduleProvider = mockk(relaxed = true)
        lineClient = mockk(relaxed = true)
        verifier = mockk(relaxed = true)
        every { verifier.verify(any(), any()) } returns true
        service = LineBotService(appDataRepo, scheduleRepo, weeklyScheduleProvider, lineClient, verifier)
    }

    private fun createWebhookJson(event: LineWebhookEvent.Event): String {
        val webhook = LineWebhookEvent(botId, listOf(event))
        return json.encodeToString(webhook)
    }

    private fun createMessageEvent(
        sourceType: SourceType = SourceType.USER,
        userId: String = "U_USER_ID",
        groupId: String? = null,
        messageType: MessageType = MessageType.TEXT,
        text: String = "hello",
        mentionees: List<LineWebhookEvent.Mentionee> = emptyList()
    ): LineWebhookEvent.Event {
        val source = LineWebhookEvent.Source(
            _type = sourceType.value,
            userId = userId,
            groupId = groupId
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

    @Test
    fun testHandleWebhook_userMessage() {
        // Arrange
        val event = createMessageEvent(sourceType = SourceType.USER, userId = "user1")
        val body = createWebhookJson(event)
        every { appDataRepo.fetchDataByRange("webhook") } returns listOf(listOf("Reply Message"))
        every { lineClient.reply(any(), any()) } returns Result.success(Unit)

        // Act
        val result = service.handleWebhook(body, signature)

        // Assert
        assertTrue(result.isSuccess)
        verify(timeout = 1000) { lineClient.reply("replyToken", "Reply Message") }
    }

    @Test
    fun testHandleWebhook_groupMention() {
        // Arrange
        val mentionees = listOf(LineWebhookEvent.Mentionee(0, 5, botId))
        val event = createMessageEvent(
            sourceType = SourceType.GROUP,
            groupId = "group1",
            mentionees = mentionees
        )
        val body = createWebhookJson(event)
        every { appDataRepo.fetchDataByRange("webhook") } returns listOf(listOf("Reply Message"))
        every { lineClient.reply(any(), any()) } returns Result.success(Unit)

        // Act
        val result = service.handleWebhook(body, signature)

        // Assert
        assertTrue(result.isSuccess)
        verify(timeout = 1000) { lineClient.reply("replyToken", "Reply Message") }
    }

    // Other tests remain unchanged as they do not depend on the repository mocks.
}
