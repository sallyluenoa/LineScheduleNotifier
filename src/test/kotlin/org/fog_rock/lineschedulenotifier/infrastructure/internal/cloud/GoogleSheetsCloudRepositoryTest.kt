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

package org.fog_rock.frlineagent.sampleapp.infrastructure.internal.cloud

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.fog_rock.frlineagent.sampleapp.domain.config.AppConfig
import org.fog_rock.frlineagent.core.domain.repository.SecretProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GoogleSheetsCloudRepositoryTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Note: Testing GoogleSheetsCloudRepository thoroughly requires mocking the Google Sheets API client library,
    // which can be complex due to its builder pattern and static methods.
    // For unit testing purposes, we often focus on verifying that the repository attempts to initialize the service
    // and handles exceptions gracefully, or we rely on integration tests.
    // Here is a basic test structure that mocks the SecretProvider interactions.
    // Full mocking of Sheets.Builder and its dependencies is omitted for brevity but would follow a similar pattern
    // using mockkConstructor or similar techniques if strict unit testing of the library interaction is required.

    @Test
    fun testFetchSheetData_exceptionHandling() {
        val appConfig = mockk<AppConfig>()
        val secretProvider = mockk<SecretProvider>()

        every { appConfig.googleCloudCredentialsKey } returns "creds-key"
        every { appConfig.name } returns "TestApp"
        // Mocking getSecret to throw an exception to simulate failure during initialization or execution
        every { secretProvider.getSecret("creds-key") } throws RuntimeException("Secret fetch failed")

        val repository = GoogleSheetsCloudRepository(appConfig, secretProvider)

        // Since initialization is lazy, this call triggers the exception
        val result = repository.fetchSheetData("A1:B2")

        // Expect empty list on failure as per implementation
        Assertions.assertEquals(emptyList<List<Any>>(), result)
    }
}
