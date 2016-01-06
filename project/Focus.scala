import sbt._
import sbt.Keys._

object Focus {
	val ScalaVersion = "2.11.7"

	def project(projectName: String, base: Option[String] = None) = 
	  Project(
	  	id = projectName,
	  	base = file(base.getOrElse(projectName))
	  ).settings(
	    scalaVersion := ScalaVersion,
	    name := projectName,
	    organization := "com.joyfulv",
	    scalacOptions := Seq(
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-Xfatal-warnings",
        "-Xfuture",
        "-Ywarn-dead-code",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-language:higherKinds",
        "-language:existentials"
	    )
	  )

  implicit class ProjectExtensions(val project: Project) extends AnyVal {
    def withMacroVerbose(verbose: Boolean) = {
      if (verbose)
        project.settings(scalacOptions ++= Seq("-Ymacro-debug-lite"))
      else project
    }
  }
}
