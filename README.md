Simplifies creating Alfred workflows using Scala 3.

---

- [Usage](#usage)
  - [Creating result rows](#creating-result-rows)
  - [Creating your Alfred App](#creating-your-alfred-app)
  - [Sending notifications](#sending-notifications)
  - [Checking for new workflow releases](#checking-for-new-workflow-releases)
  - [Accessing script environment variables](#accessing-script-environment-variables)
  - [Cache](#cache)
- [Contributors to this project](#contributors-to-this-project)

## Usage

After creating your Alfred workflow, create a `workflow.sc` file with the following content:

```scala
#!/usr/bin/env -S scala-cli shebang

//> using dep "com.alejandrohdezma::alfred4s:2.1.0"

import alfred4s.syntax.*

alfred4s.app(args) {
  case "hello" :: Nil => 
    item("Hello world!")

  case "urls" :: environment :: Nil => 
    items(item("https://example.com/1"), item("https://example.com/2"))
}
```

Then call it from your Alfred's Script Filter like:

```bash
./workflow.sc hello
```

> Remember to run `chmod + workflow.sc`!

If you want to avoid cold JVM starts you can also create a GraalVM native image
and execute the generated binary instead. See
[Packaging as GraalVM native images](https://scala-cli.virtuslab.org/docs/cookbooks/package/native-images/)
in Scala CLI documentation.

### Creating result rows

To create result rows to be shown in Alfred's main window you can use `alfred4s.items`.
Then you pass in as many `Item` as you want using `alfred4s.item`. The item's title
is mandatory, but all of the other fields are mandatory. These are the available fields
and methods:

```scala
import alfred4s.syntax.*

// Changes the title displayed in the result row
item("Original title").title("This is title that will be displayed")
```

```scala
import alfred4s.syntax.*

// A unique identifier for the item. It allows Alfred to learn about the item 
// for subsequent sorting and ordering of the user's actioned results.
//
// It is important that you use the same UID throughout subsequent executions of
// your script to take advantage of Alfred's knowledge and sorting. To show
// results in the order you return them from your script, exclude the UID field.
item("This will be the title").uid("1234")
```

```scala
import alfred4s.syntax.*

// The subtitle displayed in the result row

item("This will be the title").subtitle("This will be the subtitle")
```

```scala
import alfred4s.syntax.*

// Enables you to define what Alfred matches against when the workflow is set to
// 'Alfred Filters Results'.
//
// If match is present, it fully replaces matching on the title property.
//
// The match field is always treated as case insensitive, and intelligently
// treated as diacritic insensitive. If the search query contains a diacritic,
// the match becomes diacritic sensitive.
item("This will be the title").matching("alfred will match user input on this string")
```

```scala
import alfred4s.syntax.*

// The argument which is passed through the workflow to the connected output action.
//
// While optional, it's highly recommended that you populate arg as it's the
// string which is passed to your connected output actions. If excluded, you
// won't know which result item the user has selected.
item("This will be the title").arg("Zelda")
```

```scala
import alfred4s.syntax.*

// The icon displayed in the result row. Workflows are run from their workflow
// folder, so you can reference icons stored in your workflow relatively.
item("This will be the title").icon("icons/kratos.png")
```

```scala
import alfred4s.syntax.*

// Adds a new modifier to this item. Modifiers gives you control over how the
// modifier keys react. It can alter the looks of a result (e.g. subtitle, icon)
// and output a different arg or session variables.
item("This will be the title").mod("cmd")(alfred4s.mod.arg("false"))
```

```scala
import alfred4s.syntax.*

// Adds a new variable to this item. Variables are passed out of the Script
// Filter object and remain accessible throughout the current session as
// environment variables if the associated result item is selected in Alfred's
// results list.
item("This will be the title").variable("item_id", "1234")
```

```scala
import alfred4s.syntax.*

// Whether the item is valid or not. If an item is valid then Alfred will action
// it when the user presses return. If the item is not valid, Alfred will do
// nothing. This allows you to intelligently prevent Alfred from actioning a result
// based on the current `{query}` passed into your script.
//
// By default, Alfred assumes your item is valid.
item("This will be the title").validIf(userId.nonEmpty)
```

```scala
import alfred4s.syntax.*

// Defines the text the user will get when displaying large type with ⌘L
item("This will be the title").largetype(someLargeString)
```

```scala
import alfred4s.syntax.*

// If the item should be hidden from results or not
item("This will be the title").hideWhen(isInvalid)
```

### Creating your Alfred App

As seen in [Usage](#usage), you can use `alfred4s.app` to create a simple executable
script for your Alfred Script Filters. This method will take the arguments array
as a parameter and lets you specify your script's behaviour using a partial function
to match individual arguments:

```scala
alfred4s.app(args) {
  case "hello" :: Nil => 
    item("Hello world!")

  case "urls" :: environment :: Nil => 
    items(item("https://example.com/1"), item("https://example.com/2"))
}
```

You can indistinctly return `Item`, `Seq[Item]` or `Items` inside the main method.
You can also return `Unit`, in case you want to use the same file for the "Run Script"
components of your workflow.

### Sending notifications

This library provides a convenient `alfred4s.notify` method, that can be used as
an alternative to using Apple Script (or Alfred's workflow component) to send
notifications. The main difference is that notifications created with this method
will keep the workflow's icon. It mainly uses the same approach than
[vitorgalvao/notificator](https://github.com/vitorgalvao/notificator).

Creates a small specialised app for the workflow, so we can send MacOS
notifications with the workflow's icon.

The first time this method is called it creates an app in the workflow's cache
folder and then use it to send the provided notification.

Subsequents runs of this method will used the already provided app (unless
workflow's cache is cleared).

### Checking for new workflow releases

If your workflow is hosted on GitHub, you can use `alfred4s.appWithNewVersionCheck` 
instead of `alfred4s.app` to create your main body. This method will receive
the same arguments than `app` but will also request a GitHub owner & repository
(and a valid GitHub token) and will check if the workflow has a new version on a
schedule. By default it will run a check the first time the workflow is executed
per day. If a new version is found, it will open the latest release page so your
user can download the latest version.

```scala
alfred4s.appWithNewVersionCheck(args, "my-org", "my-workflow", gitHubToken) {
  case "hello" :: Nil => 
    item("Hello world!")

  case "urls" :: environment :: Nil => 
    items(item("https://example.com/1"), item("https://example.com/2"))
}
```

### Accessing script environment variables

Alfred offers [some useful variables](https://www.alfredapp.com/help/workflows/script-environment-variables/)
prefixed with `alfred_` with metadata and information about Alfred's instance as
well as the running workflow.

You can access these variables from `alfred4s.workflow` and `alfred4s.alfred`.

### Cache

Alfred offers [out-of-the-box caching since Alfred 5.5](https://www.alfredapp.com/help/workflows/inputs/script-filter/json/#cache).
This cache can be enabled by calling `cache` method on an `Items` instance:

```scala
import alfred4s.syntax.*
import scala.concurrent.duration.*

items(item("My item")).cache(10.minutes)
```

The TTL passed to the `cache` method must be between 5 seconds and 24 hours. The
method will throw an exception if the value is not within those boundaries.

Once a cache TTL has been established, you can enable "loosereload" by calling
`enableLooseReload`. This asks Alfred's Script Filter to try to show any cached
data first. If it's determined to be stale, the script runs in the background
and replaces results with the new data when it becomes available.

Important! Take into account this cache cannot be used in Script Filters whose
output depends on the input, since only the first output will be cached and
subsequent calls will use that value, even if the input is different. For those
cases this library offers another method for caching your filters: `alfred4s.cached`.

This method can be called to wrap any `Items` or `Seq[Item]` calculation for a
specific period of time, using a key. The data will be serialized into the
workflow's cache folder under a file named as the key and will be read in subsequent
calls until the file becomes stale:

```scala
def sayHello(name: String): Seq[Item] = ???
def getUrlsFor(environment: String): Seq[Item] = ???

alfred4s.app(args) {
  case "hello" :: name :: Nil => 
    alfred4s.cached(s"hello_$name", 5.days)(sayHello(name))

  case "urls" :: environment :: Nil => 
    items(getUrlsFor(environment)).cache(12.hours).enableLooseReload
}
```

## Contributors to this project

| <a href="https://github.com/alejandrohdezma"><img alt="alejandrohdezma" src="https://avatars.githubusercontent.com/u/9027541?v=4&s=120" width="120px" /></a> |
| :--: |
| <a href="https://github.com/alejandrohdezma"><sub><b>alejandrohdezma</b></sub></a> |
