import sbt._
import sbt.Keys._

object MyBuild extends Build {

  lazy val main = Project("shirahae-sql", file("."),
    settings = Defaults.defaultSettings ++ Seq(
      version := "0.15-SNAPSHOT",
      organization := "net.physalis",
      crossScalaVersions := Seq("2.10.2"),
      scalaVersion := "2.10.2",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
      resolvers ++= Seq(
        "typesafe" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo <<= (version) { version: String =>
        val local = Path("target/publish")
        val path = local / (if (version.trim.endsWith("SNAPSHOT")) "snapshots" else "releases")
        Some(Resolver.file("Github Pages", path)(Patterns(true, Resolver.mavenStyleBasePattern)))
      },
      publishMavenStyle := true,
      libraryDependencies := Seq(
        "org.slf4j" % "slf4j-api" % "1.7.5",
        "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
        "com.github.nscala-time" %% "nscala-time" % "0.4.2",
        "org.scalatest" %% "scalatest" % "1.9.1" % "test",
        "org.mockito" % "mockito-core" % "1.9.5" % "test",
        "org.hsqldb" % "hsqldb" % "2.2.9" % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.12" % "test",
        "org.codehaus.groovy" % "groovy" % "2.1.3" % "test"
      )
    )
  )
}

