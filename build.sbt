scalaVersion := "2.13.1"

name := "TopGun"
organization := "NA"
version := "0.1"

lazy val cmdLine = (project in file("cmdLine")).
  settings(
    inThisBuild(List(
      organization := "NA",
      scalaVersion := "2.13.1"
    )),
    name := "TopGun-cmdline",
    libraryDependencies += "args4j" % "args4j" % "2.33",
    libraryDependencies += "org.ow2.asm" % "asm" % "9.0",
    libraryDependencies += "org.ow2.asm" % "asm-tree" % "9.0",
    libraryDependencies += "junit" % "junit" % "4.13" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
  ).dependsOn(core)

lazy val core = (project in file("core")).
  settings(
    inThisBuild(List(
      organization := "NA",
      scalaVersion := "2.13.1"
    )),
    name := "TopGun-core",
    libraryDependencies += "junit" % "junit" % "4.13" % Test
  )
