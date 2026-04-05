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

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.ByteArrayInputStream
import org.fog_rock.frlineagent.sampleapp.domain.config.AppConfig
import org.fog_rock.frlineagent.core.domain.repository.SecretProvider
import org.fog_rock.frlineagent.sampleapp.domain.repository.SheetsRepository
import org.slf4j.LoggerFactory

internal class GoogleSheetsCloudRepository(
    private val config: AppConfig,
    private val secretManagerProvider: SecretProvider
) : SheetsRepository {

    private val logger = LoggerFactory.getLogger(GoogleSheetsCloudRepository::class.java)

    private val sheetsService: Sheets by lazy {
        val credentialsJson = secretManagerProvider.getSecret(config.googleCloudCredentialsKey)
        val jsonFactory = GsonFactory.getDefaultInstance()
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(credentialsJson.toByteArray()))
            .createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY))

        Sheets.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName(config.name)
            .build()
    }

    override fun fetchSheetData(range: String): List<List<Any>> =
        try {
            val spreadsheetId = secretManagerProvider.getSecret(config.googleSheetsSpreadsheetIdKey)

            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()

            response.getValues() ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to fetch data from Google Sheets. Range: $range", e)
            emptyList()
        }
}
