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

package org.fog_rock.frlineagent.sampleapp.infrastructure.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.fog_rock.frlineagent.sampleapp.domain.config.AppConfig
import org.fog_rock.frlineagent.core.domain.config.ProviderMode
import org.fog_rock.frlineagent.core.domain.repository.SecretProvider
import org.fog_rock.frlineagent.sampleapp.infrastructure.internal.cloud.GoogleSheetsCloudRepository
import org.fog_rock.frlineagent.sampleapp.infrastructure.internal.mock.MockSheetsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GoogleSheetsRepositoryImplTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testFetchSheetData_cloudMode() {
        val appConfig = mockk<AppConfig>()
        val secretProvider = mockk<SecretProvider>()
        every { appConfig.spreadsheetMode } returns ProviderMode.CLOUD
        every { appConfig.googleCloudCredentialsKey } returns "test-credentials-key"
        every { appConfig.googleSheetsSpreadsheetIdKey } returns "test-spreadsheet-id-key"

        mockkConstructor(GoogleSheetsCloudRepository::class)
        val expectedData = listOf(listOf("CloudData"))
        every { anyConstructed<GoogleSheetsCloudRepository>().fetchSheetData("A1:B2") } returns expectedData

        val repository = GoogleSheetsRepositoryImpl(appConfig, secretProvider)
        val result = repository.fetchSheetData("A1:B2")

        assertEquals(expectedData, result)
        verify(exactly = 1) { anyConstructed<GoogleSheetsCloudRepository>().fetchSheetData("A1:B2") }
    }

    @Test
    fun testFetchSheetData_mockMode() {
        val appConfig = mockk<AppConfig>()
        val secretProvider = mockk<SecretProvider>()
        every { appConfig.spreadsheetMode } returns ProviderMode.MOCK

        mockkConstructor(MockSheetsRepository::class)
        val expectedData = listOf(listOf("MockData"))
        every { anyConstructed<MockSheetsRepository>().fetchSheetData("A1:B2") } returns expectedData

        val repository = GoogleSheetsRepositoryImpl(appConfig, secretProvider)
        val result = repository.fetchSheetData("A1:B2")

        assertEquals(expectedData, result)
        verify(exactly = 1) { anyConstructed<MockSheetsRepository>().fetchSheetData("A1:B2") }
    }
}
