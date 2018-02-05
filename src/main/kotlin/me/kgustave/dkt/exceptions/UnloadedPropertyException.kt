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
package me.kgustave.dkt.exceptions

/**
 * Exception thrown when an attempt is made to access a property
 * value of a instance that has yet to "load".
 *
 * This is used in [Guild][me.kgustave.dkt.entities.Guild]
 * implementations as well as some other internals.
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
class UnloadedPropertyException(message: String): RuntimeException(message)
