/*
 * Copyright 2024-2025 Alejandro Hernández <https://github.com/alejandrohdezma>
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

package alfred4s.syntax

import scala.annotation.targetName

import alfred4s.exceptions.Failure
import alfred4s.models.Item
import alfred4s.models.Items
import alfred4s.models.Mod
import ujson.Value

extension (value: ujson.Value)

  /** Returns an optional key from this JSON value. */
  def ?(key: String): Option[Value] = value.obj.get(key)

extension (value: Option[ujson.Value])

  /** Allows chaining calls to retrieve optional values. */
  def ??(key: String): Option[Value] = value.flatMap(_.obj.get(key))

/** Fails the script, throwing an [[alfred4s.exceptions.Failure]], that will be captured by [[app]] and appropiately
  * returned to the Alfred window as an "error" item.
  */
def fail(title: String, subtitle: String = "") = throw Failure(title, subtitle)

/** Creates Alfred's result set. Every case within [[app]] should either return this or [[Unit]]. */
def items(items: Item*) = Items(items)

/** Creates Alfred's result set. Every case within [[app]] should either return this or [[Unit]]. */
@targetName("seqToItems") def items(items: Seq[Item]) = Items(items)

/** Starts creating an item's modifier. */
def mod = Mod()

/** Starts creating a result row displayed in Alfred. */
def item(title: String): Item = Item(title)
