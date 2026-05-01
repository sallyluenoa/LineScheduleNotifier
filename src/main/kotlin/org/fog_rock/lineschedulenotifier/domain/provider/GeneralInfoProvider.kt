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

package org.fog_rock.lineschedulenotifier.domain.provider

import org.fog_rock.lineschedulenotifier.domain.datasource.GeneralInfoDataSource
import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.slf4j.LoggerFactory
import java.time.YearMonth

/**
 * A provider class that generates a general information message.
 */
class GeneralInfoProvider(
    private val generalInfoDataSource: GeneralInfoDataSource,
    private val messageProvider: MessageProvider
) {

    private val logger = LoggerFactory.getLogger(GeneralInfoProvider::class.java)

    companion object {
        // Column indices for the general information sheet.
        private const val COL_CATEGORY = 0
        private const val COL_DETAILS = 1
        private const val COL_NOTES = 2
    }

    /**
     * Provide a general information message.
     * @return A formatted message string, or null if no information is available.
     */
    fun provideMessage(): String? {
        val sheetData = generalInfoDataSource.fetchMonthlyGeneralInfoData(YearMonth.now())
        if (sheetData.size <= 1) { // Needs at least a header and one data row
            logger.info("No general information data or only header found in sheet.")
            return null
        }

        val messageBody = StringBuilder()
        // Skip header row
        sheetData.drop(1).forEach { row ->
            val category = row.getOrNull(COL_CATEGORY)?.toString()?.trim()
            val details = row.getOrNull(COL_DETAILS)?.toString()?.trim()

            if (category.isNullOrBlank() && details.isNullOrBlank()) {
                return@forEach // Skip empty rows
            }

            if (!category.isNullOrBlank()) {
                messageBody.append("【${category}】\n")
            }
            if (!details.isNullOrBlank()) {
                messageBody.append("$details\n")
            }

            val notes = row.getOrNull(COL_NOTES)?.toString()?.trim()
            if (!notes.isNullOrBlank()) {
                messageBody.append("（${notes}）\n")
            }
            messageBody.append("\n")
        }

        if (messageBody.isEmpty()) {
            logger.info("Formatted message body is empty.")
            return null
        }

        val message = StringBuilder()
        message.append(messageProvider.getMessage(MessageKeys.INFO_GENERAL_TITLE))
        message.append("\n\n")
        message.append(messageBody.toString().trim())

        return message.toString().trim()
    }
}
