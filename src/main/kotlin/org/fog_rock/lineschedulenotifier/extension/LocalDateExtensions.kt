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

package org.fog_rock.lineschedulenotifier.extension

import java.time.LocalDate

/**
 * Checks if this date is within the given start and end date, inclusive.
 * @param startDate The start date of the period.
 * @param endDate The end date of the period.
 * @return True if this date is between or equal to the start and end dates.
 */
fun LocalDate.isBetween(startDate: LocalDate, endDate: LocalDate): Boolean =
    !this.isBefore(startDate) && !this.isAfter(endDate)
