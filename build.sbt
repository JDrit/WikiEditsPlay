
name := """wiki-web-ui"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  cache,
  ws,
  specs2 % Test,
  "com.typesafe.slick" % "slick_2.11" % "3.0.0",
  "com.typesafe.play" %% "play-slick" % "1.0.0",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",

  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "angularjs" % "1.4.2",
  "org.webjars" % "angular-ui-bootstrap" % "0.13.0",
  "org.webjars" % "highstock" % "2.1.4",
  "org.webjars" % "highcharts-ng" % "0.0.8",
  "org.webjars" % "angular-ui-bootstrap" % "0.13.1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
