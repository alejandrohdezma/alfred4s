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
  .settings(libraryDependencies += "org.typelevel" %% "mouse" % "1.2.3")
  .settings(libraryDependencies += "org.scala-lang" %% "toolkit" % "0.2.1")
