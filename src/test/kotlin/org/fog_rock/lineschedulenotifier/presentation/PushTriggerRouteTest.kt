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

package org.fog_rock.frlineagent.sampleapp.presentation

import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.fog_rock.frlineagent.sampleapp.domain.service.LineBotService
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertEquals

class PushTriggerRouteTest {

    private val service = mockk<LineBotService>()

    @Test
    fun testHandle_success() = testApplication {
        application {
            install(Koin) {
                modules(module {
                    single { service }
                })
            }
            routing {
                val route = PushTriggerRoute(service)
                post("/push") {
                    route.handle(call)
                }
            }
        }

        coEvery { service.executePush() } returns Result.success(Unit)

        client.post("/push").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("OK", bodyAsText())
        }
    }

    @Test
    fun testHandle_failure() = testApplication {
        application {
            install(Koin) {
                modules(module {
                    single { service }
                })
            }
            routing {
                val route = PushTriggerRoute(service)
                post("/push") {
                    route.handle(call)
                }
            }
        }

        coEvery { service.executePush() } returns Result.failure(RuntimeException("Error"))

        client.post("/push").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertEquals("Failed to execute push.", bodyAsText())
        }
    }
}
