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

package alfred4s

import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

import scala.annotation.nowarn
import scala.concurrent.duration.*
import scala.util.Try

import alfred4s.exceptions.*
import alfred4s.models.*
import alfred4s.syntax.*
import mouse.all.*
import os.Shellable
import sttp.client4.quick.*
import ujson.Value
import upickle.default.*

///////////////////////////
// Environment variables //
///////////////////////////

/** Allows direct access to the workflow's environment variables */
object workflow:

  /** Name of the running workflow */
  lazy val name = sys.env("alfred_workflow_name")

  /** Description of the running workflow */
  lazy val description = sys.env("alfred_workflow_description")

  /** Version of the running workflow */
  lazy val version = sys.env("alfred_workflow_version")

  /** Unique ID of the running workflow */
  lazy val uid = sys.env("alfred_workflow_uid")

  /** Bundle Identifier of the running workflow */
  lazy val bundleId = sys.env("alfred_workflow_bundleid")

  /** Recommended location for non-volatile workflow data:
    *
    * `~/Library/Application Support/Alfred/Workflow Data/[bundle id]`
    *
    * Note that it will only be populated if your workflow has a bundle id set.
    */
  lazy val data = os.Path(sys.env("alfred_workflow_data"))

  /** Recommended locations for volatile workflow data:
    *
    * `~/Library/Caches/com.runningwithcrayons.Alfred/Workflow Data/[bundle id]`
    *
    * Note that it will only be populated if your workflow has a bundle id set.
    */
  lazy val cache = os.Path(sys.env("alfred_workflow_cache")) <| os.makeDir.all

  /** If the user currently has the debug panel open for this workflow */
  lazy val debug = sys.env.get("alfred_debug") == Some("1")

/** Allows direct access to Alfred's environment variables */
object alfred:

  /** Find out which version the user is running. This may be useful if your workflow depends on a particular Alfred
    * version's features.
    */
  lazy val version = sys.env("alfred_version")

  /** Current theme used */
  lazy val theme = sys.env("alfred_theme")

  /** If you're creating icons on the fly, this allows you to find out the colour of the theme background */
  lazy val themeBackground = sys.env("alfred_theme_background")

  /** The colour of the selected result */
  lazy val themeSelectionBackground = sys.env("alfred_theme_selection_background")

  /** The subtext mode the user has selected in the Appearance preferences */
  lazy val themeSubtext = sys.env("alfred_theme_subtext").toInt

  /** The location of Alfred.alfredpreferences. If a user has synced their settings, this will allow you to find out
    * where their settings are regardless of sync state.
    */
  lazy val preferences = os.Path(sys.env("alfred_preferences"))

  /** Local (Mac-specific) preferences are stored within Alfred.alfredpreferences under
    * `…/preferences/local/[alfred_preferences_localhash]`
    */
  lazy val preferencesLocalHash = sys.env("alfred_preferences_localhash")

///////////
// Cache //
///////////

/** Removes the workflow's cache for a certain key */
def cleanCache(key: String) =
  os.remove.all(workflow.cache / key)

/** Removes the workflow's cache folder */
def cleanCache() =
  os.remove.all(workflow.cache)

/** Allows caching a result-set under a certain key for a certain TTL when Alfred's cache is not enough. For example
  * this method can be useful if you want to use the same "Script Filter" to return different results based on variables
  * or the previous item's argument.
  */
def cached[A: ReadWriter](key: String, ttl: FiniteDuration)(refresh: => A): A =
  val exists = os.exists(workflow.cache / key)

  val shouldRefresh = !exists || (Instant.now().toEpochMilli() - os.mtime(workflow.cache / key)) > ttl.toMillis

  if shouldRefresh then refresh <| (items => os.write.over(workflow.cache / key, write(items), createFolders = true))
  else os.read(workflow.cache / key) |> (read[A](_))

////////
// IO //
////////

/** Returns provided items to Alfred */
def out(items: Items) = (write(items, indent = 2)) |> println // scalafix:ok

/** Logs provided value and error to the Alfred workflow debugger */
def log(any: Any, throwable: Throwable = null) =
  if workflow.debug then
    val time = Instant.now().truncatedTo(SECONDS)
    Console.err.println(s"$time - $any")

    if throwable != null then throwable.printStackTrace(Console.err)

/////////
// APP //
/////////

/** Returns `true` if there is a new version available of the workflow, based on the latest release of the provided
  * GitHub repository.
  */
def hasNewVersion(owner: String, repo: String, githubToken: String, ttl: FiniteDuration): Boolean =
  val checkFile = workflow.cache / "version_checked"

  // Ensure we only check for new updates once per day
  if !os.exists(checkFile) || (Instant.now().toEpochMilli() - os.mtime(checkFile)) > ttl.toMillis then
    val response = quickRequest
      .get(uri"https://api.github.com/repos/$owner/$repo/releases/latest")
      .auth
      .bearer(githubToken)
      .header("Accept", "application/vnd.github+json")
      .header("X-GitHub-Api-Version", "2022-11-28")
      .send()

    val json = ujson.read(response.body)

    val version = json("name").str.stripPrefix("v")

    os.write.over(checkFile, "")

    workflow.version != version
  else false

/** Behaves exactly like [[app]], but once per day checks if there is a new version available of the workflow, based on
  * the latest release of the provided GitHub repository. If there is one, the release page will be open instead of the
  * normal workflow behaviour.
  */
def appWithNewVersionCheck(
    arguments: Array[String],
    owner: String,
    repo: String,
    githubToken: String,
    ttl: FiniteDuration = 1.day
)(pf: Conversion[Seq[Item], Items] ?=> PartialFunction[List[String], Unit | Items]): Unit =
  app(arguments) {
    case _ if hasNewVersion(owner, repo, githubToken, ttl) =>
      notify(
        title = "New workflow version available!",
        message = "Please install the new version. I will only remind this once per day."
      )

      os.proc("open", s"https://github.com/$owner/$repo/releases/latest").call(): @nowarn

      ()
    case args if pf.isDefinedAt(args) => pf(args)
  }

/** Main entrypoint for an Alfred "Script Filter". This method provides automatic error handling and debug logging. It
  * also handles outputing items to Alfred based on the script arguments.
  */
def app(
    arguments: Array[String]
)(pf: Conversion[Seq[Item], Items] ?=> PartialFunction[List[String], Unit | Items]): Unit =
  Try(pf.lift(arguments.toList).getOrElse(fail(s"Args not supported: `${arguments.mkString(" ")}`"))).recover {
    case e: Failure =>
      log("Handled failure ocurred", e)

      items(item(e.title).subtitle(e.subtitle).icon(icon.error))
    case e: Throwable =>
      log("An error ocurred", e)

      val sw = new StringWriter <| (writer => e.printStackTrace(new PrintWriter(writer)))

      items(
        item("An error ocurred")
          .subtitle("Press ⌘L to see the full error or check logs")
          .largetype(sw.toString()) // scalafix:ok
          .icon(icon.error)
      )
  }.foreach {
    case items: Items => out(items)
    case _: Unit      => ()
  }

///////////
// Icons //
///////////

object icon:

  def get(name: String) = s"/System/Library/CoreServices/CoreTypes.bundle/Contents/Resources/$name.icns"

  val info = "/System/Library/CoreServices/CoreTypes.bundle/Contents/Resources/ToolbarInfo.icns"

  val error = "/System/Library/CoreServices/CoreTypes.bundle/Contents/Resources/AlertStopIcon.icns"

  val alert = "/System/Library/CoreServices/CoreTypes.bundle/Contents/Resources/Actions.icns"

  val like = "/System/Library/CoreServices/CoreTypes.bundle/Contents/Resources/ToolbarFavoritesIcon.icns"

  val delete = "/System/Library/CoreServices/CoreTypes.bundle/Contents/Resources/ToolbarDeleteIcon.icns"

///////////////////
// NOTIFICATIONS //
///////////////////

/** Creates a small specialised app for the workflow, so we can send MacOS notifications with the workflow's icon.
  *
  * The first time this method is called it creates an app in the workflow's cache folder and then use it to send the
  * provided notification.
  *
  * Subsequents runs of this method will used the already provided app (unless workflow's cache is cleared).
  *
  * This method is based on https://github.com/vitorgalvao/notificator.
  */
@nowarn("msg=unused value .*")
def notify(title: String, subtitle: String = "", message: String = "", sound: String = ""): Unit =
  val name = workflow.name
    .filter(char => char.isLetterOrDigit || char == '.' || char == '_' || char == '-')

  if name.isEmpty() then fail("Workflow is missing the name!", "Can't create notificator without it")

  val app = workflow.cache / s"Notificator for $name.app"

  if !os.exists(app) then
    log(s"Creating $app...")

    val bundleId = workflow.bundleId
      .filter(char => char.isLetterOrDigit || char == '.' || char == '_' || char == '-')

    if bundleId.isEmpty() then fail("Workflow is missing the bundle_id!", "Can't create notificator without it")

    val icon = alfred.preferences / "workflows" / workflow.uid / "icon.png"

    if !os.exists(icon) then fail("Workflow is missing the icon!", "Can't create notificator without it")

    os.remove.all(app)

    val jxaScript =
      """const app = Application.currentApplication()
        |app.includeStandardAdditions = true
        |
        |const args = $.NSProcessInfo.processInfo.arguments
        |
        |app.displayNotification(args.js[1].js, {
        |  withTitle: args.js[2].js,
        |  subtitle: args.js[3].js,
        |  soundName: args.js[4].js
        |})""".stripMargin

    os.proc("osacompile", "-l", "JavaScript", "-o", app, "-e", jxaScript).call()

    val plist = app / "Contents" / "Info.plist"

    os.proc("/usr/libexec/PlistBuddy", "-c", s"add :CFBundleIdentifier string $bundleId.notificator", plist).call()
    os.proc("/usr/libexec/PlistBuddy", "-c", "add :LSUIElement bool true", plist).call()

    val iconset = app / "Contents" / "Resources" / "icon.iconset" <| os.makeDir

    // Create iconset
    for (x1, x2) <- List(16, 32, 64, 128, 256, 512).map(x => (x, x * 2)) yield {
      os.proc("sips", "--resampleHeightWidth", x1, x1, icon, "--out", iconset / s"icon_${x1}x$x1.png").call(): @nowarn
      os.proc("sips", "--resampleHeightWidth", x2, x2, icon, "--out", iconset / s"icon_${x2}x$x2@2x.png").call()
    }

    // Convert to icns
    os.proc("iconutil", "--convert", "icns", iconset, "--output", app / "Contents" / "Resources" / "applet.icns")
      .call()

    os.remove.all(iconset)

    os.proc("codesign", "--remove-signature", app).call()
    os.proc("codesign", "--sign", "-", app).call(): @nowarn

  os.proc("open", app, "--args", message, title, subtitle, sound).call(): @nowarn

///////////////
// Utilities //
///////////////

/** Split the line into tokens separated by whitespace or quotes. Perfect to use a string command in conjunction with
  * `os.proc`.
  */
def tokenize(line: String): List[String] = alfred4s.internal.tokenize(line, fail(_))
