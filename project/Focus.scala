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
	    organization := "com.github.joyfulvillage",
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
	    ),
      pomExtra := pom,
      publishTo <<= version { v: String =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },
      pomIncludeRepository := { _ => false },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots")
      )
	  )

  implicit class ProjectExtensions(val project: Project) extends AnyVal {
    def withMacroVerbose(verbose: Boolean) = {
      if (verbose)
        project.settings(scalacOptions ++= Seq("-Ymacro-debug-lite"))
      else project
    }
  }

  val pom =
    <url>https://github.com/joyfulvillage/Focus</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:joyfulvillage/Focus.git</url>
      <connection>scm:git:github.com/joyfulvillage/Focus.git</connection>
    </scm>
    <developers>
      <developer>
        <id>joyfulv@gmail.com</id>
        <name>Siu Leung Chan Victor</name>
        <url>www.github.com/joyfulvillage</url>
      </developer>
    </developers>
}
