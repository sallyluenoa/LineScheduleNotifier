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

package org.fog_rock.lineschedulenotifier.domain.message

import java.util.Locale
import java.util.ResourceBundle

/**
 * A provider for retrieving localized messages from resource bundles.
 *
 * @param baseName The base name of the resource bundle.
 * @param locale The locale for which to retrieve messages.
 */
class MessageProvider(
    private val baseName: String,
    private val locale: Locale,
) {
    private val resourceBundle: ResourceBundle by lazy {
        ResourceBundle.getBundle(baseName, locale)
    }

    /**
     * Retrieves a message for the given key.
     *
     * @param key The key of the message.
     * @return The message string.
     */
    fun getMessage(key: String): String = resourceBundle.getString(key)

    /**
     * Retrieves and formats a message for the given key and arguments.
     *
     * @param key The key of the message.
     * @param args The arguments to format into the message.
     * @return The formatted message string.
     */
    fun getMessage(key: String, vararg args: Any?): String = String.format(getMessage(key), *args)
}
