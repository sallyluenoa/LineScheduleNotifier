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

package org.fog_rock.lineschedulenotifier.plugins

import io.ktor.server.application.Application
import org.fog_rock.frlineagent.core.domain.config.ProviderMode
import org.fog_rock.lineschedulenotifier.domain.config.AppConfig
import org.fog_rock.lineschedulenotifier.domain.message.MessageProvider
import org.fog_rock.lineschedulenotifier.domain.provider.WeeklyScheduleProvider
import org.fog_rock.lineschedulenotifier.domain.repository.ApplicationDataSource
import org.fog_rock.lineschedulenotifier.domain.repository.ScheduleDataSource
import org.fog_rock.lineschedulenotifier.domain.service.LineBotService
import org.fog_rock.lineschedulenotifier.infrastructure.config.KtorAppConfig
import org.fog_rock.lineschedulenotifier.infrastructure.datasource.GoogleWorkspaceDataSource
import org.fog_rock.lineschedulenotifier.infrastructure.datasource.MockDataSource
import org.fog_rock.lineschedulenotifier.presentation.PushTriggerRoute
import org.fog_rock.lineschedulenotifier.presentation.WebhookRoute
import org.koin.dsl.binds
import org.koin.dsl.module
import java.util.Locale

/**
 * A Koin module for application-specific dependencies.
 */
fun appModule(app: Application) = module {
    single<AppConfig> { KtorAppConfig(app.environment.config) }
    single {
        MessageProvider(
            "messages.LineBotMessages",
            Locale.forLanguageTag(app.environment.config.property("app.locale.language").getString())
        )
    }
    single<ScheduleDataSource> {
        val config = get<AppConfig>()
        when (config.googleWorkspaceMode) {
            ProviderMode.CLOUD -> GoogleWorkspaceDataSource(config, get())
            ProviderMode.MOCK -> MockDataSource()
        }
    } binds arrayOf(ApplicationDataSource::class)
    single { WeeklyScheduleProvider(get(), get()) }
    single { LineBotService(get(), get(), get(), get()) }
    single { WebhookRoute(get()) }
    single { PushTriggerRoute(get()) }
}
