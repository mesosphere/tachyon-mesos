package tachyon.mesos

import org.apache.mesos._
import tachyon.conf.{ CommonConf, MasterConf }
import tachyon.UnderFileSystem
import tachyon.master.TachyonMaster
import tachyon.util.CommonUtils

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

  def startTachyonMaster(): Future[Unit] = {
    val journal = MasterConf.get.JOURNAL_FOLDER
    val name = "JOURNAL_FOLDER"
    println(s"Formatting [$name]: [$journal]")

    val ufs: Option[UnderFileSystem] = {
      val fs = UnderFileSystem.get(journal)
      if (fs.exists(journal) && !fs.delete(journal, true)) {
        println(s"Failed to remove [$name]: $journal");
        None
      }
      else if (!fs.mkdirs(journal, true)) {
        println(s"Failed to create [$name]: $journal");
        None
      }
      else {
        val prefix = MasterConf.get.FORMAT_FILE_PREFIX
        val ts = System.currentTimeMillis
        CommonUtils.touch(s"$journal$prefix$ts")
        Some(fs)
      }
    }

    if (ufs.isEmpty) {
      System.err.println(s"FATAL: Failed to create [$name]: $journal")
      System.exit(1)
    }

    val conf = MasterConf.get
    val master = new TachyonMaster(
      new InetSocketAddress("0.0.0.0", conf.PORT), // Java DNS resolution is the devil's work
      conf.WEB_PORT,
      conf.SELECTOR_THREADS,
      conf.QUEUE_SIZE_PER_SELECTOR,
      conf.SERVER_THREADS
    )
    Future { master.start }
  }

  def main(args: Array[String]): Unit = {

    if (args.length != 3) {
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
