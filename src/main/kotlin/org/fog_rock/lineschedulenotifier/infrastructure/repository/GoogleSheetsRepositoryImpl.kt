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

import org.fog_rock.frlineagent.sampleapp.domain.config.AppConfig
import org.fog_rock.frlineagent.core.domain.config.ProviderMode
import org.fog_rock.frlineagent.core.domain.repository.SecretProvider
import org.fog_rock.frlineagent.sampleapp.domain.repository.SheetsRepository
import org.fog_rock.frlineagent.sampleapp.infrastructure.internal.cloud.GoogleSheetsCloudRepository
import org.fog_rock.frlineagent.sampleapp.infrastructure.internal.mock.MockSheetsRepository

class GoogleSheetsRepositoryImpl(
    config: AppConfig,
    secretManagerProvider: SecretProvider
) : SheetsRepository {

    private val repository: SheetsRepository = when (config.spreadsheetMode) {
        ProviderMode.CLOUD -> GoogleSheetsCloudRepository(config, secretManagerProvider)
        ProviderMode.MOCK -> MockSheetsRepository()
    }

    override fun fetchSheetData(range: String): List<List<Any>> = repository.fetchSheetData(range)
}
