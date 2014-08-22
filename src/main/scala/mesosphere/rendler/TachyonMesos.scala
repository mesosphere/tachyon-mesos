package mesosphere.tachyon

import org.apache.mesos._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.io.File

object TachyonMesos {

  lazy val frameworkInfo: Protos.FrameworkInfo =
    Protos.FrameworkInfo.newBuilder
      .setName("TachyonMesos")
      .setFailoverTimeout(60.seconds.toMillis)
      .setCheckpoint(false) // TODO: enable
      .setUser("") // Mesos can do this for us
      .build

  def printUsage(): Unit = {
    println("""
      |Usage:
      |  run <tachyon-url> <mesos-master> <zookeeperAddress>
    """.stripMargin)
  }

  def main(args: Array[String]): Unit = {

    if (args.length != 3) {
      printUsage()
      sys.exit(1)
    }

    val Seq(tachyonUrlString, mesosMaster, zookeeperAddress) = args.toSeq
    val tachyonUrl = new java.net.URL(tachyonUrlString)

    println(s"""
      |Tachyon-Mesos
      |=============
      |
      |tachyonUrl:       [$tachyonUrl]
      |mesosMaster:      [$mesosMaster]
      |zookeeperAddress: [$zookeeperAddress]
      |
    """.stripMargin)

    val scheduler = new TachyonScheduler(tachyonUrl, zookeeperAddress)

    val driver: SchedulerDriver =
      new MesosSchedulerDriver(scheduler, frameworkInfo, mesosMaster)

    // driver.run blocks, run it in a separate thread...
    Await.ready(
      Future { driver.run },
      Duration.Inf
    )

  }
}
