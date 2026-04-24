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

package org.fog_rock.lineschedulenotifier.domain.message

import java.util.Locale
import java.util.MissingResourceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MessageProviderTest {

    private val baseName = "messages.LineBotMessages"

    @Test
    fun testGetMessage_english() {
        val messageProvider = MessageProvider(baseName, Locale.ENGLISH)
        assertEquals(
            "Here is the schedule for this week.",
            messageProvider.getMessage(MessageKeys.SCHEDULE_WEEKLY_TITLE)
        )
    }

    @Test
    fun testGetMessage_english_withArgs() {
        val messageProvider = MessageProvider(baseName, Locale.ENGLISH)
        assertEquals(
            "Events: Test Event",
            messageProvider.getMessage(MessageKeys.SCHEDULE_WEEKLY_EVENTS, "Test Event")
        )
    }

    @Test
    fun testGetMessage_japanese() {
        val messageProvider = MessageProvider(baseName, Locale.JAPANESE)
        assertEquals(
            "今週の予定です。",
            messageProvider.getMessage(MessageKeys.SCHEDULE_WEEKLY_TITLE)
        )
    }

    @Test
    fun testGetMessage_japanese_withArgs() {
        val messageProvider = MessageProvider(baseName, Locale.JAPANESE)
        assertEquals(
            "行事: テストイベント",
            messageProvider.getMessage(MessageKeys.SCHEDULE_WEEKLY_EVENTS, "テストイベント")
        )
    }

    @Test
    fun testGetMessage_missingKey() {
        val messageProvider = MessageProvider(baseName, Locale.ENGLISH)
        assertThrows(MissingResourceException::class.java) {
            messageProvider.getMessage("unknown.key")
        }
    }
}
