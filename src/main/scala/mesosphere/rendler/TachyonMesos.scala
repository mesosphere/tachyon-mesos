package mesosphere.tachyon

import org.apache.mesos._
import tachyon.conf.MasterConf
import tachyon.master.TachyonMaster

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.net.{ InetSocketAddress, URL }

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

  def startTachyonMaster(): Future[Unit] =
    Future {
      val conf = MasterConf.get
      val master = new TachyonMaster(
        new InetSocketAddress(conf.HOSTNAME, conf.PORT),
        conf.WEB_PORT,
        conf.SELECTOR_THREADS,
        conf.QUEUE_SIZE_PER_SELECTOR,
        conf.SERVER_THREADS
      )
      master.start
    }

  def main(args: Array[String]): Unit = {

    if (args.length != 2) {
      printUsage()
      sys.exit(1)
    }

    val Seq(tachyonUrlString, mesosMaster, zookeeperAddress) = args.toSeq
    val tachyonUrl = new URL(tachyonUrlString)

    println(s"""
      |Tachyon-Mesos
      |=============
      |
      |tachyonUrl:       [$tachyonUrl]
      |mesosMaster:      [$mesosMaster]
      |zookeeperAddress: [$zookeeperAddress]
      |
    """.stripMargin)

    println("Starting the Tachyon Master...")
    startTachyonMaster()

    println("Starting the Tachyon Scheduler...")
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
