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

package org.fog_rock.frlineagent.sampleapp.infrastructure.config

import io.ktor.server.config.ApplicationConfig
import org.fog_rock.frlineagent.sampleapp.domain.config.AppConfig
import org.fog_rock.frlineagent.core.domain.config.ProviderMode

/**
 * A class that reads AppConfig from a Ktor configuration file.
 */
class KtorAppConfig(config: ApplicationConfig) : AppConfig {

    override val name: String =
        config.property("app.name").getString()

    override val secretManagerMode: ProviderMode =
        getProviderMode(config, "app.provider.secret_manager")

    override val spreadsheetMode: ProviderMode =
        getProviderMode(config, "app.provider.spreadsheet")

    override val lineApiMode: ProviderMode =
        getProviderMode(config, "app.provider.line_api")

    override val googleCloudProjectNumber: String =
        config.property("app.google_cloud.project_number").getString()

    override val googleCloudCredentialsKey: String =
        config.property("app.google_cloud.credentials_key").getString()

    override val googleSheetsSpreadsheetIdKey: String =
        config.property("app.google_sheets.spreadsheet_id_key").getString()

    override val lineBotChannelAccessTokenKey: String =
        config.property("app.line_bot.channel_access_token_key").getString()

    override val lineBotChannelSecretKey: String =
        config.property("app.line_bot.channel_secret_key").getString()

    private fun getProviderMode(config: ApplicationConfig, path: String): ProviderMode =
        ProviderMode.convert(config.propertyOrNull(path)?.getString())
}
