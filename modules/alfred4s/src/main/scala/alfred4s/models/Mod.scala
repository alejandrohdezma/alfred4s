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

package alfred4s.models

import alfred4s.syntax.*
import mouse.all.*
import upickle.default.*

/** Represents a row modifier for a given key in Alfred */
final case class Mod(
    title: Option[String] = None,
    subtitle: Option[String] = None,
    arg: Option[String] = None,
    icon: Option[String] = None,
    valid: Boolean = true,
    variables: Map[String, Option[String]] = Map()
) {

  /** The title displayed in the result row when the modifier key is hit */
  def title(value: String): Mod = copy(title = Some(value))

  /** The subtitle displayed in the result row when the modifier key is hit */
  def subtitle(value: String): Mod = copy(subtitle = Some(value))

  /** The argument which is passed through the workflow to the connected output action when the modifier key is hit.
    *
    * While optional, it's highly recommended that you populate arg as it's the string which is passed to your connected
    * output actions. If excluded, you won't know which result item the user has selected.
    */
  def arg(value: String): Mod = copy(arg = Some(value))

  /** The icon displayed in the result row when the modifier key is hit. Workflows are run from their workflow folder,
    * so you can reference icons stored in your workflow relatively.
    */
  def icon(value: String): Mod = copy(icon = Some(value))

  /** Adds a new variable to this item when the modifier key is hit. Variables are passed out of the Script Filter
    * object and remain accessible throughout the current session as environment variables if the associated result item
    * is selected in Alfred's results list.
    *
    * Beware that modifier variables do not inherit default item's variables so you'll need to re-add any variable you
    * want to keep.
    */
  def variable(key: String, value: String): Mod = copy(variables = variables + (key -> Some(value)))

  /** Adds a new variable to this item when the modifier key is hit. Variables are passed out of the Script Filter
    * object and remain accessible throughout the current session as environment variables if the associated result item
    * is selected in Alfred's results list.
    *
    * Beware that modifier variables do not inherit default item's variables so you'll need to re-add any variable you
    * want to keep.
    */
  def variable(key: String, value: Option[String]): Mod = copy(variables = variables + (key -> value))

  /** If the item is valid or not. If an item is valid then Alfred will action it when the user presses return. If the
    * item is not valid, Alfred will do nothing. This allows you to intelligently prevent Alfred from actioning a result
    * based on the current `{query}` passed into your script.
    *
    * By default, Alfred assumes your item is valid.
    */
  def validIf(value: Boolean): Mod = copy(valid = value)

}

object Mod {

  given ReadWriter[Mod] = readwriter[ujson.Value].bimap[Mod](
    mod =>
      List[(String, ujson.Value)](
        "title"     -> mod.title,
        "subtitle"  -> mod.subtitle.map(ujson.Str(_)).getOrElse(ujson.Null),
        "arg"       -> mod.arg.map(ujson.Str(_)).getOrElse(ujson.Null),
        "icon"      -> mod.icon.map(icon => ujson.Obj("path" -> icon)).getOrElse(ujson.Null),
        "variables" -> ujson.Obj.from(mod.variables.view.mapValues(_.map(ujson.Str(_)).getOrElse(ujson.Null))),
        "valid"     -> mod.valid
      ).filter(_._2 != ujson.Null) |> ujson.Obj.from,
    json =>
      Mod(
        title = json.?("title").flatMap(_.strOpt),
        subtitle = json.?("subtitle").flatMap(_.strOpt),
        arg = json.?("arg").flatMap(_.strOpt),
        icon = json.?("icon").??("path").map(_.str),
        variables = json("variables").obj.view.mapValues(_.strOpt).toMap,
        valid = json("valid").bool
      )
  )

}
