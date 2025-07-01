ThisBuild / scalaVersion           := "3.3.6"
ThisBuild / organization           := "com.alejandrohdezma"
ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible

addCommandAlias("ci-test", "fix --check; mdoc; doc; publishLocal;")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .dependsOn(alfred4s)

lazy val alfred4s = module
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.9")
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %% "upickle" % "4.0.9")
  .settings(libraryDependencies += "com.lihaoyi" %% "upickle" % "4.2.1")
  .settings(libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.4")
  .settings(libraryDependencies += "org.typelevel" %% "mouse" % "1.3.2")
  .settings(libraryDependencies += "me.xdrop" % "fuzzywuzzy" % "1.4.0")

lazy val `alfred4s-native` = module
  .settings(name := "alfred4s")
  .enablePlugins(ScalaNativePlugin)
  .settings(sourceDirectory := (alfred4s / sourceDirectory).value)
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %%% "core" % "4.0.9")
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %%% "upickle" % "4.0.9")
  .settings(libraryDependencies += "com.lihaoyi" %%% "upickle" % "4.2.1")
  .settings(libraryDependencies += "com.lihaoyi" %%% "os-lib" % "0.11.4")
  .settings(libraryDependencies += "org.typelevel" %%% "mouse" % "1.3.2")
  .settings(libraryDependencies += "me.xdrop" % "fuzzywuzzy" % "1.4.0")
