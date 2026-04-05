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
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import org.fog_rock.frlineagent.sampleapp.domain.service.LineBotService
import org.slf4j.LoggerFactory

/**
 * Route handler for LINE Webhook requests.
 */
class WebhookRoute(
    private val service: LineBotService
) {
    private val logger = LoggerFactory.getLogger(WebhookRoute::class.java)

    companion object {
        private const val HEADER_X_LINE_SIGNATURE = "X-Line-Signature"
        private const val MESSAGE_OK = "OK"
        private const val MESSAGE_MISSING_HEADER = "Missing X-Line-Signature header."
        private const val MESSAGE_FAILED_RECEIVE_BODY = "Failed to receive request body."
        private const val MESSAGE_INVALID_SIGNATURE = "Invalid signature."
        private const val MESSAGE_INTERNAL_SERVER_ERROR = "Internal Server Error"
    }

    /**
     * Handles the webhook request.
     *
     * @param call The application call.
     */
    suspend fun handle(call: ApplicationCall) {
        val signature = call.request.header(HEADER_X_LINE_SIGNATURE) ?: run {
            logger.error(MESSAGE_MISSING_HEADER)
            call.respond(HttpStatusCode.BadRequest, MESSAGE_MISSING_HEADER)
            return
        }

        val body = try {
            call.receiveText()
        } catch (e: Exception) {
            logger.error(MESSAGE_FAILED_RECEIVE_BODY, e)
            call.respond(HttpStatusCode.InternalServerError, MESSAGE_FAILED_RECEIVE_BODY)
            return
        }

        service.handleWebhook(body, signature)
            .onSuccess {
                logger.info("Successfully handled webhook.")
                call.respond(HttpStatusCode.OK, MESSAGE_OK)
            }
            .onFailure { e ->
                if (e is SecurityException) {
                    logger.warn(MESSAGE_INVALID_SIGNATURE, e)
                    call.respond(HttpStatusCode.Unauthorized, MESSAGE_INVALID_SIGNATURE)
                } else {
                    logger.error("Failed to handle webhook.", e)
                    call.respond(HttpStatusCode.InternalServerError, MESSAGE_INTERNAL_SERVER_ERROR)
                }
            }
    }
}
