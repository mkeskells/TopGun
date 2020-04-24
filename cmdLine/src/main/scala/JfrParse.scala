import java.io.File

import com.jrockit.mc.common._
import com.jrockit.mc.flightrecorder.FlightRecordingLoader
import com.jrockit.mc.flightrecorder.internal.model._
import com.jrockit.mc.flightrecorder.spi.IEvent

import scala.collection.mutable
import scala.jdk.CollectionConverters._


object JfrParse extends App{

    val callSites = mutable.HashMap[CallSite, CallSiteInfo]()

    val recording = FlightRecordingLoader.loadFile(new File("S:\\recording.jfr"))
    for (t <- recording.getEventTypes.asScala) {
      println(t.getName)
    }
    val view = recording.createView
    var count = 0

    def allocation(event: IEvent) : Unit = {
      println(event)
    }
    def cpu(event: IEvent) : Unit = {
      val stack = event.getValue("(stackTrace)").asInstanceOf[FLRStackTrace]
      val thread = event.getValue("(thread)").asInstanceOf[FLRThread]

      val fullFrames: List[CallSite] = stack.getFrames.asScala.toList map {
        frame: IMCFrame =>
          val method = frame.getMethod
          CallSite(method.getPackageName, method.getClassName, method.getMethodName, method.getFormalDescriptor, frame.getFrameLineNumber)
      }
      val distinctFramesAndUsage = fullFrames.distinct map {f => (f, callSites.getOrElseUpdate(f, new CallSiteInfo))}

      distinctFramesAndUsage.foreach {
        case (frame, usage) =>
          usage.cpuTicks += 1
      }
      println(event)
    }

    for (event <- view.asScala) {
      event.getEventType.getName match {
        case "Allocation in new TLAB" => allocation(event)
        case "Allocation outside TLAB" => allocation(event)
        case "Method Profiling Sample" =>cpu(event)
        case _ =>
      }
      count += 1
    }
    callSites.toList.sortBy( - _._2.cpuTicks) foreach {
      case (site, usage) => println(s"count ${usage.cpuTicks} site $site")
    }
    System.out.println("Fount " + count + " events")

  }
  object CallSite {
    val all = new mutable.HashMap[CallSite, CallSite]()
    def apply(packageName: String, className: String, methodName: String, desc: String, line: Int): CallSite = {
      val site = new CallSite(packageName.intern, className.intern, methodName.intern, desc.intern, line)
      all.getOrElseUpdate(site, site)
    }

  }
  case class CallSite private(packageName:String, className: String, methodName:String, desc: String, line:Int)

  class CallSiteInfo {
    var cpuTicks = 0
  }
  class CpuTicks {
    var totalTransitive = 0
    var totalLocal = 0
    var totalAttributed = 0
  }
