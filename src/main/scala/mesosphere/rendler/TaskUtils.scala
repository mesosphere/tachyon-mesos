package mesosphere.tachyon

import org.apache.mesos._
import com.google.protobuf.ByteString
import scala.collection.JavaConverters._
import java.net.URL

trait TaskUtils {

  def tachyonUrl(): URL
  def zookeeperAddress(): String

  val TASK_CPUS = 1.0
  val TASK_MEM = 256.0

  protected[this] val tachyonJavaOpts: Seq[String] = Seq(
    "-Dtachyon.usezookeeper=true",
    s"-Dtachyon.zookeeper.address=$zookeeperAddress"
  )

  protected[this] val tachyonEnvironment: Protos.Environment = {
    val vars: Map[String, String] =
      Map("TACHYON_JAVA_OPTS" -> tachyonJavaOpts.mkString(" "))

    val builder = Protos.Environment.newBuilder
    for ((key, value) <- vars) {
      val variable =
        Protos.Environment.Variable.newBuilder.setName(key).setValue(value)
      builder.addVariables(variable)
    }
    builder.build
  }

  protected[this] val tachyonUris: Seq[Protos.CommandInfo.URI] =
    Seq(tachyonUrl.toString).map { url =>
      Protos.CommandInfo.URI.newBuilder
        .setValue(url)
        .setExtract(true)
        .build
    }

  lazy val tachyonFormatAndRunLocal: Protos.CommandInfo =
    Protos.CommandInfo.newBuilder
      .setValue("./tachyon-0.5.0/bin/tachyon format && ./tachyon-0.5.0/bin/tachyon-start.sh worker Mount")
      .addAllUris(tachyonUris.asJava)
      .setEnvironment(tachyonEnvironment)
      .setUser("root")
      .build

  lazy val tachyonRunLocal: Protos.CommandInfo =
    Protos.CommandInfo.newBuilder
      .setValue("./tachyon-0.5.0/bin/tachyon-start.sh worker Mount")
      .addAllUris(tachyonUris.asJava)
      .setEnvironment(tachyonEnvironment)
      .setUser("root")
      .build

  def makeTaskPrototype(id: String, offer: Protos.Offer): Protos.TaskInfo =
    Protos.TaskInfo.newBuilder
      .setTaskId(Protos.TaskID.newBuilder.setValue(id))
      .setName("")
      .setSlaveId((offer.getSlaveId))
      .addAllResources(
        Seq(
          scalarResource("cpus", TASK_CPUS),
          scalarResource("mem", TASK_MEM)
        ).asJava
      )
      .build

  protected def scalarResource(name: String, value: Double): Protos.Resource =
    Protos.Resource.newBuilder
      .setType(Protos.Value.Type.SCALAR)
      .setName(name)
      .setScalar(Protos.Value.Scalar.newBuilder.setValue(value))
      .build

  def makeTachyonTask(
    id: String,
    format: Boolean,
    offer: Protos.Offer): Protos.TaskInfo =
    makeTaskPrototype(id, offer).toBuilder
      .setName(s"tachyon_$id")
      .setCommand(if (format) tachyonFormatAndRunLocal else tachyonRunLocal)
      .build

  def isTerminal(state: Protos.TaskState): Boolean = {
    import Protos.TaskState._
    state match {
      case TASK_FINISHED | TASK_FAILED | TASK_KILLED | TASK_LOST =>
        true
      case _ =>
        false
    }
  }

}
