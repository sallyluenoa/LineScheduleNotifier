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

package org.fog_rock.frlineagent.sampleapp.infrastructure.internal.mock

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MockSheetsRepositoryTest {

    @Test
    fun testFetchSheetData() {
        val repository = MockSheetsRepository()
        val result = repository.fetchSheetData("A1:B2")

        val expected = listOf(
            listOf("Header1", "Header2"),
            listOf("Value1", "Value2")
        )
        Assertions.assertEquals(expected, result)
    }
}
