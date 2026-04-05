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

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.fog_rock.frlineagent.sampleapp.domain.service.LineBotService
import org.slf4j.LoggerFactory

/**
 * Route handler for push trigger requests.
 */
class PushTriggerRoute(
    private val service: LineBotService
) {
    private val logger = LoggerFactory.getLogger(PushTriggerRoute::class.java)

    companion object {
        private const val MESSAGE_OK = "OK"
        private const val MESSAGE_FAILED = "Failed to execute push."
    }

    /**
     * Handles the push trigger request.
     *
     * @param call The application call.
     */
    suspend fun handle(call: ApplicationCall) {
        service.executePush()
            .onSuccess {
                logger.info("Successfully executed push.")
                call.respond(HttpStatusCode.OK, MESSAGE_OK)
            }
            .onFailure { e ->
                logger.error(MESSAGE_FAILED, e)
                call.respond(HttpStatusCode.InternalServerError, MESSAGE_FAILED)
            }
    }
}
