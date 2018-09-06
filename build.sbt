name := "limiter"

version := "2018.09"

scalaVersion := "2.12.6"
val akkaVersion = "2.5.14"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws, guice,

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.mockito" % "mockito-all" % "2.0.2-beta" % Test,
  "net.jadler" % "jadler-all" % "1.3.0" % Test
)

routesImport += "conversions.Binders._"

coverageExcludedPackages := "<empty>;Reverse.*;router.*;controllers.javascript;play.api.*;views.html.*"

herokuAppName in Compile := "limiter-be"
