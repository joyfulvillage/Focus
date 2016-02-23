import Focus._
import sbt.Keys._

scalaVersion := Focus.ScalaVersion

lazy val focusCore = Focus.project("focus-core", Option("focus"))
  .settings(libraryDependencies ++= Seq(
    "org.spire-math" %% "cats" % "0.3.0",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    ),
    sonatypeProfileName := "joyfulv@gmail.com",
    organization := "com.github.joyfulvillage"
  )

lazy val focusMacros = Focus.project("focus-macros", Option("macros"))
  .settings(libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _))
  .settings(libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % Focus.ScalaVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    ),
    sonatypeProfileName := "joyfulv@gmail.com",
    organization := "com.github.joyfulvillage"
  )
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full))
  .withMacroVerbose(verbose = false)
  .dependsOn(focusCore)

lazy val root = (sbt.project in file(".")).settings(
  scalaVersion := Focus.ScalaVersion,
  publishArtifact := false
).aggregate(focusCore, focusMacros)
