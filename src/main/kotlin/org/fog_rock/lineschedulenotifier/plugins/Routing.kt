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

package org.fog_rock.frlineagent.sampleapp.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.fog_rock.frlineagent.sampleapp.presentation.PushTriggerRoute
import org.fog_rock.frlineagent.sampleapp.presentation.WebhookRoute
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val webhookRoute by inject<WebhookRoute>()
    val pushTriggerRoute by inject<PushTriggerRoute>()

    routing {
        post("/webhook") {
            webhookRoute.handle(call)
        }
        post("/push") {
            pushTriggerRoute.handle(call)
        }
    }
}
