import sbt._
import sbt.Keys._

object MyBuild extends Build {

  lazy val main = Project("shirahae-sql", file("."),
    settings = Defaults.defaultSettings ++ Seq(
      version := "0.16",
      organization := "net.physalis",
      crossScalaVersions := Seq("2.11.7"),
      scalaVersion := "2.11.7",
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
        "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
        "com.github.nscala-time" %% "nscala-time" % "1.4.0",
        "org.scalatest" %% "scalatest" % "2.2.1" % "test",
        "org.mockito" % "mockito-core" % "1.9.5" % "test",
        "org.hsqldb" % "hsqldb" % "2.2.9" % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.12" % "test",
        "org.codehaus.groovy" % "groovy" % "2.1.3" % "test"
      )
    )
  )
}

