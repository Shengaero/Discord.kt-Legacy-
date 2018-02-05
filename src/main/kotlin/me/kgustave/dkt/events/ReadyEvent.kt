/*
 * Copyright 2018 Kaidan Gustave
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
package me.kgustave.dkt.events

import me.kgustave.dkt.API

/**
 * Event that fires when the [API] is finally initialized.
 *
 * This only fires at the beginning of a session.
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
class ReadyEvent(override val api: API, override val responseNumber: Long): Event
