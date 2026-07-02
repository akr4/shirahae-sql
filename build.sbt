lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "net.physalis",
      scalaVersion := "2.13.11",
      crossScalaVersions := Seq("2.12.18", "2.13.11", "3.3.6"),
      version := "0.26",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
      resolvers ++= Seq(
        "typesafe" at "https://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo := Some("GitHub Packages" at "https://maven.pkg.github.com/akr4/shirahae-sql"),
      publishMavenStyle := true,
      credentials += Credentials(
        "GitHub Package Registry",
        "maven.pkg.github.com",
        sys.env.getOrElse("GITHUB_ACTOR", ""),
        sys.env.getOrElse("GITHUB_TOKEN", "")
      )
    )),
    name := "shirahae-sql",
    Test / fork := true,
    Test / javaOptions += "-XX:+EnableDynamicAgentLoading",
    libraryDependencies := Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6",
      "com.github.nscala-time" %% "nscala-time" % "2.32.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % "test",
      "org.mockito" % "mockito-core" % "5.14.2" % "test",
      "org.hsqldb" % "hsqldb" % "2.7.4" % "test",
      "ch.qos.logback" % "logback-classic" % "1.5.14" % "test"
    )
  )
