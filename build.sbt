
name := "akka-stream-domain-socket-tests"

version := "1.0"

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  "Confluent Maven Repo" at "http://packages.confluent.io/maven/"
)

lazy val `domain-socket-tests` = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(Common.settings: _*)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)
  .settings (
    fork in run := true
  )
