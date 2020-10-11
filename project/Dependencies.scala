import sbt._
import Keys._
import Versions._

object Dependencies {

  object Common {
    val akkaStreams           = "com.typesafe.akka"          %% "akka-stream"                   % akkaVersion
    val alpakkaDomainSocket   = "com.lightbend.akka"         %% "akka-stream-alpakka-unix-domain-socket" % alpakkaVersion
    // val alpakkaRecordIO       = "com.lightbend.akka"         %% "akka-stream-alpakka-simple-codecs" % alpakkaVersion
    // val jffi                  = "com.github.jnr"              % "jffi"    % "1.2.23"
    val jnrUs                 = "com.github.jnr"              % "jnr-unixsocket" % "0.38.3" 
  }

  val commonDependencies: Seq[ModuleID] = Seq(
    Common.akkaStreams,
    Common.alpakkaDomainSocket,
    // Common.alpakkaRecordIO
    // Common.jffi,
    Common.jnrUs 
  )

}
