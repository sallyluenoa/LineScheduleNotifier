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

package org.fog_rock.lineschedulenotifier.domain.service.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ReplyTriggerTest {

    @Nested
    @DisplayName("USER_ID Trigger")
    inner class UserIdTrigger {
        @ParameterizedTest
        @ValueSource(strings = [
            "user id", "user_id", "userid", "ユーザーid", "my id", "あなたのid",
            "USER ID", "My Id", "  user id  ", "what is my user id?"
        ])
        fun testFrom_userIdKeywords_returnUserId(keyword: String) {
            assertEquals(ReplyTrigger.USER_ID, ReplyTrigger.from(keyword))
        }
    }

    @Nested
    @DisplayName("GROUP_ID Trigger")
    inner class GroupIdTrigger {
        @ParameterizedTest
        @ValueSource(strings = [
            "group id", "group_id", "groupid", "グループid",
            "GROUP ID", "Group_Id", "  group id  ", "tell me the group id"
        ])
        fun testFrom_groupIdKeywords_returnGroupId(keyword: String) {
            assertEquals(ReplyTrigger.GROUP_ID, ReplyTrigger.from(keyword))
        }
    }

    @Nested
    @DisplayName("SCHEDULE Trigger")
    inner class ScheduleTrigger {
        @ParameterizedTest
        @ValueSource(strings = ["schedule", "予定", "SCHEDULE", "　予定　"])
        fun testFrom_scheduleKeywords_returnSchedule(keyword: String) {
            assertEquals(ReplyTrigger.SCHEDULE, ReplyTrigger.from(keyword))
        }
    }

    @Test
    fun testFrom_unknownKeyword_returnNull() {
        assertNull(ReplyTrigger.from("hello world"))
    }

    @Test
    fun testFrom_emptyString_returnNull() {
        assertNull(ReplyTrigger.from(""))
    }

    @Test
    fun testFrom_multipleKeywords_returnFirstMatch() {
        // USER_ID is declared before GROUP_ID and SCHEDULE in the enum, so it should be matched first.
        assertEquals(ReplyTrigger.USER_ID, ReplyTrigger.from("show my user id and the schedule"))
    }
}
