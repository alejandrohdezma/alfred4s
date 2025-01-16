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

import scala.concurrent.duration.*

import upickle.default.*

/** Describes a result-set displayed in Alfred */
final case class Items(
    items: Seq[Item] = Seq(),
    originalSort: Boolean = true,
    skipknowledge: Boolean = true,
    cache: Option[FiniteDuration] = None,
    loosereload: Boolean = false
) {

  private[this] def sortedItems = if originalSort then items else items.sortBy(_.title.toLowerCase())

  /** Instructs Alfred to sort the items by title before serializing them. */
  def enableSortingByTitle = copy(originalSort = false)

  /** Enables loose-reload, which asks the Script Filter to try to show any cached data first. If it's determined to be
    * stale, the script runs in the background and replaces results with the new data when it becomes available.
    */
  def enableLooseReload = copy(loosereload = true)

  /** Scripts which take a while to return can cache results so users see data sooner on subsequent runs.
    *
    * The Script Filter presents the results from the previous run when caching is active and hasn't expired. Because
    * the script won't execute, we recommend this option only be used with "Alfred filters results".
    *
    * @param duration
    *   cache's TTL, between 5 seconds and 24 hours
    * @return
    */
  def cache(duration: FiniteDuration) =
    require(duration >= 5.seconds, "Duration must be greater or equal than 5 seconds")
    require(duration <= 24.hours, "Duration must be less or equal than 24 hours")
    copy(cache = Some(duration))

}

object Items {

  given Conversion[Seq[Item], Items] = Items(_)

  given Conversion[Item, Items] = item => Items(Seq(item))

  given ReadWriter[Items] = readwriter[ujson.Value].bimap[Items](
    items =>
      ujson.Obj(
        "skipknowledge" -> ujson.Bool(items.skipknowledge),
        "items"         -> writeJs(items.sortedItems.filter(_.isVisible)),
        "cache" -> items.cache
          .map(duration =>
            ujson.Obj(
              "seconds"     -> ujson.Num(duration.toSeconds.toDouble),
              "loosereload" -> ujson.Bool(items.loosereload)
            )
          )
          .getOrElse(ujson.Null)
      ),
    json =>
      Items(
        items = json("items").arr.toList.map(read[Item](_)),
        skipknowledge = json.obj.get("skipknowledge").map(_.bool).getOrElse(true),
        cache = json.obj
          .get("cache")
          .flatMap(_.objOpt)
          .map(_("seconds").num.toLong)
          .map(FiniteDuration(_, java.util.concurrent.TimeUnit.SECONDS)),
        loosereload = json.obj
          .get("cache")
          .flatMap(_.objOpt)
          .map(_("loosereload").bool)
          .getOrElse(false)
      )
  )

}
