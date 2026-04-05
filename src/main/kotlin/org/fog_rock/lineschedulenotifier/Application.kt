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

package org.fog_rock.frlineagent.sampleapp

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.fog_rock.frlineagent.core.plugin.FRLineAgent
import org.fog_rock.frlineagent.sampleapp.domain.service.LineBotService
import org.fog_rock.frlineagent.sampleapp.infrastructure.config.KtorAppConfig
import org.fog_rock.frlineagent.sampleapp.plugins.sampleAppModule
import org.fog_rock.frlineagent.sampleapp.plugins.configureMonitoring
import org.fog_rock.frlineagent.sampleapp.plugins.configureRouting
import org.fog_rock.frlineagent.sampleapp.plugins.configureSerialization

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val config = KtorAppConfig(environment.config)

    install(FRLineAgent) {
        secretManagerMode = config.secretManagerMode
        googleCloudProjectNumber = config.googleCloudProjectNumber
        lineApiMode = config.lineApiMode
        lineBotChannelSecretKey = config.lineBotChannelSecretKey
        lineBotChannelAccessTokenKey = config.lineBotChannelAccessTokenKey
        lineBotService = LineBotService::class
        appModule = sampleAppModule(this@module)
    }

    configureMonitoring()
    configureSerialization()
    configureRouting()
}
