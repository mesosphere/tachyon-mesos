package mesosphere.tachyon

import org.apache.mesos
import mesos._

import scala.collection.JavaConverters._
import scala.collection.mutable
import java.net.URL

class TachyonScheduler(
  val tachyonUrl: URL,
  val zookeeperAddress: String)
    extends mesos.Scheduler
    with TaskUtils {

  private[this] var tasksCreated = 0
  private[this] var tasksRunning = 0
  private[this] val workers = mutable.Set[String]()

  def disconnected(driver: SchedulerDriver): Unit =
    println("Disconnected from the Mesos master...")

  def error(driver: SchedulerDriver, msg: String): Unit =
    println(s"ERROR: [$msg]")

  def executorLost(
    driver: SchedulerDriver,
    executorId: Protos.ExecutorID,
    slaveId: Protos.SlaveID,
    status: Int): Unit =
    println(s"EXECUTOR LOST: [${executorId.getValue}]")

  def frameworkMessage(
    driver: SchedulerDriver,
    executorId: Protos.ExecutorID,
    slaveId: Protos.SlaveID,
    data: Array[Byte]): Unit = {
    println(s"Received a framework message from [${executorId.getValue}]")
  }

  def offerRescinded(
    driver: SchedulerDriver,
    offerId: Protos.OfferID): Unit =
    println(s"Offer [${offerId.getValue}] has been rescinded")

  def registered(
    driver: SchedulerDriver,
    frameworkId: Protos.FrameworkID,
    masterInfo: Protos.MasterInfo): Unit = {
    val host = masterInfo.getHostname
    val port = masterInfo.getPort
    println(s"Registered with Mesos master [$host:$port]")
  }

  def reregistered(
    driver: SchedulerDriver,
    masterInfo: Protos.MasterInfo): Unit = ???

  def resourceOffers(
    driver: SchedulerDriver,
    offers: java.util.List[Protos.Offer]): Unit = {

    for (offer <- offers.asScala) {
      println(s"Got resource offer [$offer]")

      val tasks = mutable.Buffer[Protos.TaskInfo]()
      if (!workers.contains(offer.getHostname())) {
        tasks += makeTachyonTask(s"$tasksCreated", true, offer)
        tasksCreated = tasksCreated + 1
        workers += offer.getHostname()
        driver.launchTasks(Seq(offer.getId).asJava, tasks.asJava)
      }
      else {
        driver.declineOffer(offer.getId)
      }
    }
  }

  def slaveLost(
    driver: SchedulerDriver,
    slaveId: Protos.SlaveID): Unit =
    println("SLAVE LOST: [${slaveId.getValue}]")

  def statusUpdate(
    driver: SchedulerDriver,
    taskStatus: Protos.TaskStatus): Unit = {
    val taskId = taskStatus.getTaskId.getValue
    val state = taskStatus.getState
    println(s"Task [$taskId] is in state [$state]")
    if (state == Protos.TaskState.TASK_RUNNING)
      tasksRunning = tasksRunning + 1
    else if (isTerminal(state))
      tasksRunning = math.max(0, tasksRunning - 1)
  }

}
