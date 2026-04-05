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

package org.fog_rock.frlineagent.sampleapp.domain.config

import org.fog_rock.frlineagent.core.domain.config.CoreAppConfig
import org.fog_rock.frlineagent.core.domain.config.ProviderMode

/**
 * An interface for managing the integration mode with external services.
 */
interface AppConfig : CoreAppConfig {
    /** Mode for Spreadsheet. */
    val spreadsheetMode: ProviderMode
    /** Key for Google Credentials in Secret Manager. */
    val googleCloudCredentialsKey: String
    /** Key for Spreadsheet ID in Secret Manager. */
    val googleSheetsSpreadsheetIdKey: String
}
