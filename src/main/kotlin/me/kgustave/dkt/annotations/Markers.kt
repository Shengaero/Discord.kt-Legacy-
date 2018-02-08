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
package me.kgustave.dkt.annotations

// This file contains annotations that reflect behavior for planning
// purposes as the API develops. Most likely, they will not appear in
// the first release.

/**
 * Marks an entity as "Effectively Final", meaning
 * that it's value, albeit mutable, will be set once
 * and should remain that value for the duration of
 * it's usage.
 *
 * This is typically for lateinit properties that the
 * API provides at a later time than would be easily
 * made immutable properties of a class
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
annotation class EffectivelyFinal

/**
 * Marks an entity to signify this value will update
 * along the runtime of the API connection.
 *
 * This is typically done through event handling
 * when an entity the API caches has a property changed.
 *
 * With that, this should only be applied in the scope
 * of a **cached** entity, and non-cached entities should
 * not have this.
 *
 * Note: that properties that are not marked explicitly
 * with this annotation may be subject to descending
 * behavior as a result of their definition being related
 * and dependent on the value of a property marked with this.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
annotation class Updating

/**
 * Marks an interface found in the [entities package][me.kgustave.dkt.entities]
 * to be designated as a "trait interface".
 *
 * Trait interfaces provide a very specific set of properties and
 * functions that many Discord entities share.
 *
 * For example, both the [Category][me.kgustave.dkt.entities.Category]
 * and the [Guild][me.kgustave.dkt.entities.Guild] interfaces can be
 * classified as [ChannelHolders][me.kgustave.dkt.entities.ChannelHolder].
 *
 * Note: [Snowflake][me.kgustave.dkt.entities.Snowflake] implementations
 * are not classified under this definition, as they are so common among
 * most Discord entities that they themselves could be considered an entity
 * in their own right.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class EntityTrait

/**
 * Marks a property (typically a lateinit one) that should be initialized
 * or setup as soon as it is possible.
 *
 * These are mostly implementation specific values, not typically exposed
 * in any way by the API.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
annotation class InitializeASAP

/**
 * Marks a class or other type that is only used internally by the API.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class InternalOnly
