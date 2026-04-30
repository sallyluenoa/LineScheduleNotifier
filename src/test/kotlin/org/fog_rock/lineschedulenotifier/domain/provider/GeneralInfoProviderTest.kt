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

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.fog_rock.lineschedulenotifier.domain.datasource.GeneralInfoDataSource
import org.fog_rock.lineschedulenotifier.domain.message.MessageKeys
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class GeneralInfoProviderTest {

    private lateinit var generalInfoDataSource: GeneralInfoDataSource
    private lateinit var messageProvider: MessageProvider
    private lateinit var generalInfoProvider: GeneralInfoProvider

    private val currentYearMonth = YearMonth.of(2026, 4)

    @BeforeEach
    fun setup() {
        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns currentYearMonth

        generalInfoDataSource = mockk()
        messageProvider = mockk {
            every { getMessage(MessageKeys.INFO_GENERAL_TITLE) } returns "General Information"
        }
        generalInfoProvider = GeneralInfoProvider(generalInfoDataSource, messageProvider)
    }

    @Test
    fun testProvideMessage_success() {
        // Arrange
        val data = listOf(
            listOf("Category", "Details", "Notes"),
            listOf("Category 1", "Details 1", "Notes 1"),
            listOf("Category 2", "Details 2", "")
        )
        every { generalInfoDataSource.fetchMonthlyGeneralInfoData(currentYearMonth) } returns data

        // Act
        val result = generalInfoProvider.provideMessage()

        // Assert
        val expected = """
            General Information

            【Category 1】
            Details 1
            （Notes 1）

            【Category 2】
            Details 2
        """.trimIndent()
        assertEquals(expected, result?.trim())
    }

    @Test
    fun testProvideMessage_noData() {
        // Arrange
        every { generalInfoDataSource.fetchMonthlyGeneralInfoData(currentYearMonth) } returns listOf(listOf("Header"))

        // Act
        val result = generalInfoProvider.provideMessage()

        // Assert
        assertNull(result)
    }

    @Test
    fun testProvideMessage_emptyDataSource() {
        // Arrange
        every { generalInfoDataSource.fetchMonthlyGeneralInfoData(currentYearMonth) } returns emptyList()

        // Act
        val result = generalInfoProvider.provideMessage()

        // Assert
        assertNull(result)
    }

    @Test
    fun testProvideMessage_skipsEmptyRows() {
        // Arrange
        val data = listOf(
            listOf("Category", "Details", "Notes"),
            listOf("Category 1", "Details 1", "Notes 1"),
            listOf("", " ", "  "), // Empty row
            listOf("Category 2", "Details 2", "Notes 2")
        )
        every { generalInfoDataSource.fetchMonthlyGeneralInfoData(currentYearMonth) } returns data

        // Act
        val result = generalInfoProvider.provideMessage()

        // Assert
        val expected = """
            General Information

            【Category 1】
            Details 1
            （Notes 1）

            【Category 2】
            Details 2
            （Notes 2）
        """.trimIndent()
        assertEquals(expected, result?.trim())
    }

    @Test
    fun testProvideMessage_onlyCategory() {
        // Arrange
        val data = listOf(
            listOf("Category", "Details", "Notes"),
            listOf("Category Only", "", "")
        )
        every { generalInfoDataSource.fetchMonthlyGeneralInfoData(currentYearMonth) } returns data

        // Act
        val result = generalInfoProvider.provideMessage()

        // Assert
        val expected = """
            General Information

            【Category Only】
        """.trimIndent()
        assertEquals(expected, result?.trim())
    }

    @Test
    fun testProvideMessage_allColumnsBlank_returnsNull() {
        // Arrange
        val data = listOf(
            listOf("Category", "Details", "Notes"),
            listOf("", "", "")
        )
        every { generalInfoDataSource.fetchMonthlyGeneralInfoData(currentYearMonth) } returns data

        // Act
        val result = generalInfoProvider.provideMessage()

        // Assert
        assertNull(result)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(YearMonth::class)
    }
}
