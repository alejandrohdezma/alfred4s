ThisBuild / scalaVersion           := "3.3.3"
ThisBuild / organization           := "com.alejandrohdezma"
ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible

addCommandAlias("ci-test", "fix --check; mdoc; doc; publishLocal;")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .dependsOn(alfred4s)

lazy val alfred4s = module
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M17")
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %% "upickle" % "4.0.0-M17")
  .settings(libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.1")
  .settings(libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.10.3")
  .settings(libraryDependencies += "org.typelevel" %% "mouse" % "1.3.2")

lazy val `alfred4s-native` = module
  .settings(name := "alfred4s")
  .enablePlugins(ScalaNativePlugin)
  .settings(sourceDirectory := (alfred4s / sourceDirectory).value)
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %%% "core" % "4.0.0-M17")
  .settings(libraryDependencies += "com.softwaremill.sttp.client4" %%% "upickle" % "4.0.0-M17")
  .settings(libraryDependencies += "com.lihaoyi" %%% "upickle" % "3.3.1")
  .settings(libraryDependencies += "com.lihaoyi" %%% "os-lib" % "0.10.3")
  .settings(libraryDependencies += "org.typelevel" %%% "mouse" % "1.3.2")
