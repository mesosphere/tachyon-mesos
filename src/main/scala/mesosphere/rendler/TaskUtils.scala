package mesosphere.tachyon

import org.apache.mesos._
import com.google.protobuf.ByteString
import scala.collection.JavaConverters._
import java.net.URL

trait TaskUtils {

  def tachyonUrl(): URL

  val TASK_CPUS = 1.0
  val TASK_MEM = 256.0

  protected[this] val TachyonUris: Seq[Protos.CommandInfo.URI] =
    Seq(tachyonUrl.toString).map { url =>
      Protos.CommandInfo.URI.newBuilder
        .setValue(url)
        .setExtract(true)
        .build
    }

  lazy val tachyonFormatAndRunLocal: Protos.CommandInfo =
    Protos.CommandInfo.newBuilder
      .setValue("./bin/tachyon format && ./bin/tachyon-start.sh local")
      .build

  lazy val tachyonRunLocal: Protos.CommandInfo =
    Protos.CommandInfo.newBuilder
      .setValue("./bin/tachyon-start.sh local")
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
