/*
 * Copyright 2024 Alejandro Hernández <https://github.com/alejandrohdezma>
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

/** Describes a result row displayed in Alfred */
final case class Item(
    title: String,
    uid: Option[String] = None,
    subtitle: Option[String] = None,
    `match`: Option[String] = None,
    arg: Option[String] = None,
    icon: Option[String] = None,
    largetype: Option[String] = None,
    mods: Map[String, Mod] = Map(),
    variables: Map[String, Option[String]] = Map(),
    valid: Boolean = true,
    isVisible: Boolean = true
) {

  /** The title displayed in the result row */
  def title(value: String): Item = copy(title = value)

  /** A unique identifier for the item. It allows Alfred to learn about the item for subsequent sorting and ordering of
    * the user's actioned results.
    *
    * It is important that you use the same UID throughout subsequent executions of your script to take advantage of
    * Alfred's knowledge and sorting. To show results in the order you return them from your script, exclude the UID
    * field.
    */
  def uid(value: String): Item = copy(uid = Some(value))

  /** The subtitle displayed in the result row. This element is optional */
  def subtitle(value: String): Item = copy(subtitle = Some(value))

  /** Enables you to define what Alfred matches against when the workflow is set to 'Alfred Filters Results'.
    *
    * If match is present, it fully replaces matching on the title property.
    *
    * The match field is always treated as case insensitive, and intelligently treated as diacritic insensitive. If the
    * search query contains a diacritic, the match becomes diacritic sensitive.
    *
    * @see
    *   https://www.alfredapp.com/help/workflows/inputs/script-filter/#alfred-filters-results
    */
  def matching(value: String): Item = copy(`match` = Some(value))

  /** The argument which is passed through the workflow to the connected output action.
    *
    * While optional, it's highly recommended that you populate arg as it's the string which is passed to your connected
    * output actions. If excluded, you won't know which result item the user has selected.
    */
  def arg(value: String): Item = copy(arg = Some(value))

  /** The icon displayed in the result row. Workflows are run from their workflow folder, so you can reference icons
    * stored in your workflow relatively.
    */
  def icon(value: String): Item = copy(icon = Some(value))

  /** Adds a new modifier to this item. Modifiers gives you control over how the modifier keys react. It can alter the
    * looks of a result (e.g. subtitle, icon) and output a different arg or session variables.
    *
    * @example
    *   ```scala
    *   alfred4s
    *     .item("My title")
    *     .arg("true")
    *     .mod("cmd")(mod.arg("false"))
    *   ```
    */
  def mod(key: "fn" | "ctrl" | "opt" | "cmd" | "shift")(mod: Mod): Item = copy(mods = mods.updated(key, mod))

  /** Adds a new variable to this item. Variables are passed out of the Script Filter object and remain accessible
    * throughout the current session as environment variables if the associated result item is selected in Alfred's
    * results list.
    */
  def variable(key: String, value: String): Item = copy(variables = variables + (key -> Some(value)))

  /** Adds a new variable to this item. Variables are passed out of the Script Filter object and remain accessible
    * throughout the current session as environment variables if the associated result item is selected in Alfred's
    * results list.
    */
  def variable(key: String, value: Option[String]): Item = copy(variables = variables + (key -> value))

  /** If the item is valid or not. If an item is valid then Alfred will action it when the user presses return. If the
    * item is not valid, Alfred will do nothing. This allows you to intelligently prevent Alfred from actioning a result
    * based on the current `{query}` passed into your script.
    *
    * By default, Alfred assumes your item is valid.
    */
  def validIf(value: Boolean): Item = copy(valid = value)

  /** Defines the text the user will get when displaying large type with ⌘L */
  def largetype(value: String): Item = copy(largetype = Some(value))

  /** If the item should be hidden from results or not */
  def hideWhen(value: Boolean): Item = copy(isVisible = !value)

}

object Item {

  given ReadWriter[Item] = readwriter[ujson.Value].bimap[Item](
    item =>
      List[(String, ujson.Value)](
        "title"     -> item.title,
        "uid"       -> item.uid.map(ujson.Str(_)).getOrElse(ujson.Null),
        "subtitle"  -> item.subtitle.map(ujson.Str(_)).getOrElse(ujson.Null),
        "match"     -> item.`match`.map(ujson.Str(_)).getOrElse(ujson.Null),
        "arg"       -> item.arg.map(ujson.Str(_)).getOrElse(ujson.Null),
        "icon"      -> item.icon.map(icon => ujson.Obj("path" -> icon)).getOrElse(ujson.Null),
        "text"      -> item.largetype.map(text => ujson.Obj("largetype" -> text)).getOrElse(ujson.Null),
        "variables" -> ujson.Obj.from(item.variables.view.mapValues(_.map(ujson.Str(_)).getOrElse(ujson.Null))),
        "mods"      -> ujson.Obj.from(item.mods.map((k, v) => k -> writeJs(v))),
        "valid"     -> item.valid,
        "visible"   -> item.isVisible
      ).filter(_._2 != ujson.Null) |> ujson.Obj.from,
    json =>
      Item(
        title = json("title").str,
        uid = json.?("uid").flatMap(_.strOpt),
        subtitle = json.?("subtitle").flatMap(_.strOpt),
        `match` = json.?("match").flatMap(_.strOpt),
        arg = json.?("arg").flatMap(_.strOpt),
        icon = json.?("icon").??("path").map(_.str),
        largetype = json.?("text").??("largetype").map(_.str),
        mods = json.obj("mods").obj.map((k, v) => k -> read[Mod](v)).toMap,
        variables = json("variables").obj.view.mapValues(_.strOpt).toMap,
        valid = json("valid").bool,
        isVisible = json("visible").bool
      )
  )

}
