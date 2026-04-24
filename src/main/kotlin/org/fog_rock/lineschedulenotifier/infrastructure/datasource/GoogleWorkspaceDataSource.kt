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

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.fog_rock.frlineagent.core.domain.repository.SecretProvider
import org.fog_rock.lineschedulenotifier.domain.config.AppConfig
import org.fog_rock.lineschedulenotifier.domain.repository.ApplicationDataSource
import org.fog_rock.lineschedulenotifier.domain.repository.ScheduleDataSource
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal class GoogleWorkspaceDataSource(
    private val config: AppConfig,
    private val secretProvider: SecretProvider
) : ScheduleDataSource, ApplicationDataSource {
    private val logger = LoggerFactory.getLogger(GoogleWorkspaceDataSource::class.java)

    companion object {
        private const val DATE_FORMAT_PATTERN = "yyyyMM"
        private const val FILENAME_REPLACE_TARGET = "YYYYMM"
    }

    private val credentials by lazy {
        val credentialsJson = secretProvider.getSecret(config.googleApiCredentialsKey)
        GoogleCredentials.fromStream(ByteArrayInputStream(credentialsJson.toByteArray()))
            .createScoped(listOf(
                SheetsScopes.SPREADSHEETS_READONLY,
                DriveScopes.DRIVE_READONLY
            ))
    }

    private val driveService: Drive by lazy {
        val jsonFactory = GsonFactory.getDefaultInstance()
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        Drive.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName(config.name)
            .build()
    }

    private val sheetsService: Sheets by lazy {
        val jsonFactory = GsonFactory.getDefaultInstance()
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        Sheets.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName(config.name)
            .build()
    }

    override fun fetchDataByRange(range: String): List<List<Any>> =
        try {
            val spreadsheetId = secretProvider.getSecret(config.googleSheetsSpreadsheetIdKey)
            sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
                .getValues()
        } catch (e: Exception) {
            logger.error("Failed to fetch data from Google Sheets. Range: $range", e)
            emptyList()
        }

    override fun fetchMonthlyData(yearMonth: YearMonth): List<List<Any>> =
        try {
            val folderId = secretProvider.getSecret(config.googleDriveFolderIdKey)
            val filenameFormat = secretProvider.getSecret(config.googleSheetsFilenameFormatKey)

            val monthStr = yearMonth.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN))
            val filename = filenameFormat.replace(FILENAME_REPLACE_TARGET, monthStr)

            val fileId = findFileId(filename, folderId) ?: run {
                logger.info("File not found for month: $monthStr")
                return emptyList()
            }
            sheetsService.spreadsheets().values()
                .get(fileId, filename)
                .execute()
                .getValues()
        } catch (e: Exception) {
            logger.error("Failed to fetch scheduled data from Google Sheets for month: $yearMonth", e)
            emptyList()
        }

    private fun findFileId(name: String, folderId: String): String? =
        try {
            val query = """
                '$folderId' in parents
                and name = '$name'
                and mimeType = 'application/vnd.google-apps.spreadsheet'
                and trashed = false
                """
                .trimIndent()
                .replace("\n", " ")
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()
            result.files?.firstOrNull()?.id
        } catch (e: IOException) {
            logger.error("Failed to find file with name '$name' in folder '$folderId'.", e)
            null
        }
}
