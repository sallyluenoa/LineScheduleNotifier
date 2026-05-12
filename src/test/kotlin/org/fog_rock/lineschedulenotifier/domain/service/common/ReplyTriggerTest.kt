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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("ReplyTrigger.fromAll Tests")
class ReplyTriggerTest {

    @Test
    fun testFromAll_noTrigger_returnEmptySet() {
        assertEquals(emptySet<ReplyTrigger>(), ReplyTrigger.fromAll("hello world"))
    }

    @Test
    fun testFromAll_emptyString_returnEmptySet() {
        assertEquals(emptySet<ReplyTrigger>(), ReplyTrigger.fromAll(""))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "user id", "user_id", "userid", "ユーザーid", "my id", "あなたのid",
        "USER ID", "My Id", "  user id  ", "what is my user id?"
    ])
    fun testFromAll_onlyUserId_returnUserIdSet(keyword: String) {
        assertEquals(setOf(ReplyTrigger.USER_ID), ReplyTrigger.fromAll(keyword))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "group id", "group_id", "groupid", "グループid",
        "GROUP ID", "Group_Id", "  group id  ", "tell me the group id"
    ])
    fun testFromAll_onlyGroupId_returnGroupIdSet(keyword: String) {
        assertEquals(setOf(ReplyTrigger.GROUP_ID), ReplyTrigger.fromAll(keyword))
    }

    @ParameterizedTest
    @ValueSource(strings = ["schedule", "予定", "SCHEDULE", "　予定　"])
    fun testFromAll_onlySchedule_returnScheduleSet(keyword: String) {
        assertEquals(setOf(ReplyTrigger.SCHEDULE), ReplyTrigger.fromAll(keyword))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "general info", "info", "rules", "やくそく", "ルール", "持ち物", "約束", "お知らせ",
        "INFO", "Rules", "  やくそく  ", "tell me the rules"
    ])
    fun testFromAll_onlyGeneralInfo_returnGeneralInfoSet(keyword: String) {
        assertEquals(setOf(ReplyTrigger.GENERAL_INFO), ReplyTrigger.fromAll(keyword))
    }

    @ParameterizedTest
    @ValueSource(strings = ["version", "バージョン", "Version", "  version  ", "what is the version?"])
    fun testFromAll_onlyVersion_returnVersionSet(keyword: String) {
        assertEquals(setOf(ReplyTrigger.VERSION), ReplyTrigger.fromAll(keyword))
    }

    @Test
    fun testFromAll_allTriggers_returnAllTriggersSet() {
        val text = "show my user id, group id, the 予定, and the rules"
        val expected = setOf(ReplyTrigger.USER_ID, ReplyTrigger.GROUP_ID, ReplyTrigger.SCHEDULE, ReplyTrigger.GENERAL_INFO)
        assertEquals(expected, ReplyTrigger.fromAll(text))
    }

    @Test
    fun testFromAll_duplicateTriggers_returnUniqueTriggersSet() {
        val text = "user id, what is my USER ID?"
        val expected = setOf(ReplyTrigger.USER_ID)
        assertEquals(expected, ReplyTrigger.fromAll(text))
    }

    @Test
    fun testFromAll_mixedCaseAndKeywords_returnAllTriggersSet() {
        val text = "Show my USER ID and the SCHEDULE."
        val expected = setOf(ReplyTrigger.USER_ID, ReplyTrigger.SCHEDULE)
        assertEquals(expected, ReplyTrigger.fromAll(text))
    }
}
