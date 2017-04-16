lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "net.physalis",
      scalaVersion := "2.12.1",
      crossScalaVersions := Seq("2.11.8", "2.12.1"),
      version := "0.18",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
      resolvers ++= Seq(
        "typesafe" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo <<= (version) { version: String =>
        val local = Path("target/publish")
        val path = local / (if (version.trim.endsWith("SNAPSHOT")) "snapshots" else "releases")
        Some(Resolver.file("Github Pages", path)(Patterns(true, Resolver.mavenStyleBasePattern)))
      },
      publishMavenStyle := true
    )),
    name := "shirahae-sql",
    libraryDependencies := Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "com.github.nscala-time" %% "nscala-time" % "2.16.0",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      "org.hsqldb" % "hsqldb" % "2.2.9" % "test",
      "ch.qos.logback" % "logback-classic" % "1.0.12" % "test",
      "org.codehaus.groovy" % "groovy" % "2.1.3" % "test"
    )
  )

