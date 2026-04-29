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

package org.fog_rock.lineschedulenotifier.infrastructure.datasource

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.fog_rock.frlineagent.core.domain.repository.SecretProvider
import org.fog_rock.lineschedulenotifier.domain.config.AppConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

/**
 * This test class focuses on verifying the logic within GoogleWorkspaceDataSource,
 * such as configuration handling and exception resilience, without making actual API calls.
 * Mocking final classes from the Google API library (e.g., Drive, Sheets) requires
 * `mockk-agent-jvm`, which is not included in the project's dependencies.
 * Therefore, tests do not cover the behavior of the API clients themselves.
 */
class GoogleWorkspaceDataSourceTest {

    private lateinit var config: AppConfig
    private lateinit var secretProvider: SecretProvider
    private lateinit var dataSource: GoogleWorkspaceDataSource

    @BeforeEach
    fun setup() {
        config = mockk(relaxed = true) {
            every { googleApiCredentialsKey } returns "google-api-credentials-key"
            every { googleDriveFolderIdKey } returns "folder-id-key"
            every { googleSheetsFilenameFormatKey } returns "google-sheets-filename-format-key"
        }
        secretProvider = mockk(relaxed = true) {
            every { getSecret("google-api-credentials-key") } returns "{}" // Empty JSON for credentials
            every { getSecret("folder-id-key") } returns "test-folder-id"
            every { getSecret("google-sheets-filename-format-key") } returns "test-format-yyyyMM"
        }

        dataSource = GoogleWorkspaceDataSource(config, secretProvider)
    }

    @Test
    fun testFetchDataByKey_requestsSpreadsheetId() {
        // Arrange
        val spreadsheetIdKey = "test-spreadsheet-id-key"
        
        // Act
        dataSource.fetchDataByKey(spreadsheetIdKey, "test_range")

        // Assert
        verify { secretProvider.getSecret(spreadsheetIdKey) }
    }

    @Test
    fun testFetchDataByKey_returnsEmptyListOnFailure() {
        // Arrange
        val spreadsheetIdKey = "test-spreadsheet-id-key"
        every { secretProvider.getSecret(spreadsheetIdKey) } throws RuntimeException("Test Exception")

        // Act
        val result = dataSource.fetchDataByKey(spreadsheetIdKey, "test_range")

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFetchMonthlyData_requestsFolderIdAndFilenameFormat() {
        // Arrange
        val yearMonth = YearMonth.of(2026, 4)

        // Act
        dataSource.fetchMonthlyData(yearMonth)

        // Assert
        verify { secretProvider.getSecret("folder-id-key") }
        verify { secretProvider.getSecret("google-sheets-filename-format-key") }
    }

    @Test
    fun testFetchMonthlyData_returnsEmptyListOnFolderIdFailure() {
        // Arrange
        val yearMonth = YearMonth.of(2026, 4)
        every { secretProvider.getSecret(config.googleDriveFolderIdKey) } throws RuntimeException("Test Exception")

        // Act
        val result = dataSource.fetchMonthlyData(yearMonth)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFetchMonthlyData_returnsEmptyListOnFilenameFormatFailure() {
        // Arrange
        val yearMonth = YearMonth.of(2026, 4)
        every { secretProvider.getSecret(config.googleSheetsFilenameFormatKey) } throws RuntimeException("Test Exception")

        // Act
        val result = dataSource.fetchMonthlyData(yearMonth)

        // Assert
        assertTrue(result.isEmpty())
    }
}
