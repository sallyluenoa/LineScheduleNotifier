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

/**
 * An enum representing the trigger for a reply message, based on text content.
 * Each trigger is associated with a regular expression to identify relevant keywords.
 */
enum class ReplyTrigger(private val regex: Regex) {
    /** Trigger for user ID requests. */
    USER_ID(
        "user id|user_id|userid|ユーザーid|my id|あなたのid".toRegex(RegexOption.IGNORE_CASE)
    ),

    /** Trigger for group ID requests. */
    GROUP_ID(
        "group id|group_id|groupid|グループid".toRegex(RegexOption.IGNORE_CASE)
    ),

    /** Trigger for schedule requests. */
    SCHEDULE(
        "schedule|スケジュール|予定".toRegex(RegexOption.IGNORE_CASE)
    ),

    /** Trigger for general information requests. */
    GENERAL_INFO(
        "general info|info|rules|やくそく|ルール|持ち物|約束|お知らせ".toRegex(RegexOption.IGNORE_CASE)
    ),

    /** Trigger for version requests. */
    VERSION(
        "version|バージョン".toRegex(RegexOption.IGNORE_CASE)
    ),
    ;

    companion object {
        /**
         * Finds all [ReplyTrigger]s present in the given text.
         *
         * @param text The input text to match against trigger patterns.
         * @return A [Set] of all matching [ReplyTrigger]s. Returns an empty set if no matches are found.
         */
        fun fromAll(text: String): Set<ReplyTrigger> =
            entries.filter { it.regex.containsMatchIn(text) }.toSet()
    }
}
