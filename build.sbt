lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "net.physalis",
      scalaVersion := "2.13.11",
      crossScalaVersions := Seq("2.12.18", "2.13.11"),
      version := "0.24",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
      resolvers ++= Seq(
        "typesafe" at "https://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo := {
        val local = Path("target/publish")
        val path: File = local / (if (version.toString.trim.endsWith("SNAPSHOT")) "snapshots" else "releases")
        Some(Resolver.file("Github Pages", path)(Patterns(true, Resolver.mavenStyleBasePattern)))
      },
      publishMavenStyle := true
    )),
    name := "shirahae-sql",
    libraryDependencies := Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "com.github.nscala-time" %% "nscala-time" % "2.32.0",
      "org.scalatest" %% "scalatest" % "3.2.16" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      "org.hsqldb" % "hsqldb" % "2.2.9" % "test",
      "ch.qos.logback" % "logback-classic" % "1.0.12" % "test",
      "org.codehaus.groovy" % "groovy" % "2.1.3" % "test"
    )
  )
