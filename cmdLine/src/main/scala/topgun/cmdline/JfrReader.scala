package topgun.cmdline

import com.jrockit.mc.common.IMCFrame
import com.jrockit.mc.flightrecorder.FlightRecordingLoader
import com.jrockit.mc.flightrecorder.internal.model.{FLRStackTrace, FLRThread}
import com.jrockit.mc.flightrecorder.spi.IEvent
import topgun.core.{CallSite, CallSiteInfo}

import scala.collection.mutable

import scala.jdk.CollectionConverters._

class JfrReader(cmdLine: JfrParseCommandLine) {
  val callSites = mutable.HashMap[CallSite, CallSiteInfo]()

  def process() {
    val callSites = mutable.HashMap[CallSite, CallSiteInfo]()

    var count = 0
    val files = if (cmdLine.jfr.isDirectory) cmdLine.jfr.listFiles().toList else List(cmdLine.jfr)
    files foreach { file =>
      println(s"*** File $file")
      try {
        val recording = FlightRecordingLoader.loadFile(file)
        for (t <- recording.getEventTypes.asScala) {
          println(t.getName)
        }
        val view = recording.createView

        for (event <- view.asScala) {
          event.getEventType.getName match {
            case "Allocation in new TLAB" => allocation(event)
            case "Allocation outside TLAB" => allocation(event)
            case "Method Profiling Sample" => cpu(event)
            case _ =>
          }
          count += 1
        }
      } catch {
        case e:Exception => e.printStackTrace()
      }
    }

    callSites.toList.sortBy(-_._2.cpuTicks) foreach {
      case (site, usage) => println(s"count ${usage.cpuTicks} site $site")
    }
    System.out.println("Found " + count + " events")
  }

  def allocation(event: IEvent): Unit = {
    println(event)
  }

  def cpu(event: IEvent): Unit = {
    val stack = event.getValue("(stackTrace)").asInstanceOf[FLRStackTrace]
    val thread = event.getValue("(thread)").asInstanceOf[FLRThread]

    val fullFrames: List[CallSite] = stack.getFrames.asScala.toList map {
      frame: IMCFrame =>
        val method = frame.getMethod
        CallSite(method.getPackageName, method.getClassName, method.getMethodName, method.getFormalDescriptor, frame.getFrameLineNumber)
    }
    val distinctFramesAndUsage = fullFrames.distinct map { f => (f, callSites.getOrElseUpdate(f, new CallSiteInfo)) }

    distinctFramesAndUsage.foreach {
      case (frame, usage) =>
        usage.cpuTicks += 1
    }
    distinctFramesAndUsage.find {
      case (frame, usage) => !frame.packageName.startsWith("java.") && !frame.packageName.startsWith("scala.")
    } foreach {
      (_._2.firstUserCpuTicks += 1)
    }

  }
}
