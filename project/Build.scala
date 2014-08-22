import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object TachyonBuild extends Build {

//////////////////////////////////////////////////////////////////////////////
// PROJECT INFO
//////////////////////////////////////////////////////////////////////////////

  val ORGANIZATION    = "mesosphere"
  val PROJECT_NAME    = "tachyon-mesos"
  val PROJECT_VERSION = "0.1.0-SNAPSHOT"
  val SCALA_VERSION   = "2.11.1"

//////////////////////////////////////////////////////////////////////////////
// DEPENDENCY VERSIONS
//////////////////////////////////////////////////////////////////////////////

  val MESOS_VERSION   = "0.20.0-rc2"
  val TACHYON_VERSION = "0.5.0"

//////////////////////////////////////////////////////////////////////////////
// NATIVE LIBRARY PATHS
//////////////////////////////////////////////////////////////////////////////

  val pathToMesosLibs = "/usr/local/lib"

//////////////////////////////////////////////////////////////////////////////
// PROJECTS
//////////////////////////////////////////////////////////////////////////////

  lazy val root = Project(
    id = PROJECT_NAME,
    base = file("."),
    settings = tachyonSettings
  )

//////////////////////////////////////////////////////////////////////////////
// SETTINGS
//////////////////////////////////////////////////////////////////////////////

  lazy val tachyonSettings = Project.defaultSettings ++
                             assemblySettings ++
                             basicSettings ++
                             formatSettings

  lazy val basicSettings = Seq(
    version := PROJECT_VERSION,
    organization := ORGANIZATION,
    scalaVersion := SCALA_VERSION,

    libraryDependencies ++= Seq(
      "org.apache.mesos"   % "mesos"          % MESOS_VERSION,
      "org.tachyonproject" % "tachyon-client" % TACHYON_VERSION
    ),

    scalacOptions in Compile ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    javaOptions ++= Seq(
      "-Djava.library.path=%s:%s".format(
        sys.props("java.library.path"),
        pathToMesosLibs
      ),
      "-Dtachyon.usezookeeper=true",
      "-Dtachyon.zookeeper.address=localhost:2181" // TODO: this better!
    ),

    connectInput in run := true,

    fork in run := true,

    fork in Test := true,

    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
      {
        case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
        case x => old(x)
      }
    }
  )

  lazy val formatSettings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(IndentWithTabs, false)
      .setPreference(IndentSpaces, 2)
      .setPreference(AlignParameters, false)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(SpaceBeforeColon, false)
      .setPreference(SpaceInsideBrackets, false)
      .setPreference(SpaceInsideParentheses, false)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(FormatXml, true)
  )

}