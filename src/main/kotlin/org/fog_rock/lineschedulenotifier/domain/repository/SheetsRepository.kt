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

package org.fog_rock.frlineagent.sampleapp.domain.repository

/**
 * Interface for accessing Google Sheets data.
 */
interface SheetsRepository {
    /**
     * Fetches data from a specified range in the spreadsheet.
     *
     * @param range The A1 notation of the range to fetch.
     * @return A list of rows, where each row is a list of cell values.
     */
    fun fetchSheetData(range: String): List<List<Any>>
}
