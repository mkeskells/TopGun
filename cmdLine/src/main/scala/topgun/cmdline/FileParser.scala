package topgun.cmdline

import java.io.File
import java.nio.file.Files

import com.jrockit.mc.common.IMCFrame
import com.jrockit.mc.flightrecorder.FlightRecordingLoader
import com.jrockit.mc.flightrecorder.internal.model.{FLRStackTrace, FLRThread, FLRType}
import com.jrockit.mc.flightrecorder.spi.IEvent
import topgun.core.CallSite

import scala.jdk.CollectionConverters._

class FileParser(file: File, cmdLine: JfrParseCommandLine, totals: Totals, configuration: Configuration) {

  import configuration.{includeStack, includeThread}

  def parse(): Unit = {

    println(s"*** File $file")
    try {
      val recording = FlightRecordingLoader.loadFile(file)
      for (t <- recording.getEventTypes.asScala) {
        println(t.getName)
      }
      val view = recording.createView
      view.asScala.find(_.getEventType.getName == "GC TLAB Configuration") match {
        case Some(e) => handleTlabConfiguration(e)
        case _ => ???
      }

      for (event <- view.asScala) {
        event.getEventType.getName match {
          case "Allocation in new TLAB" => allocation(event, true)
          case "Allocation outside TLAB" => allocation(event, false)
          case "Method Profiling Sample" => cpu(event)
          case e =>
            totals.ignoreEvent(e)

          //            case "GC Configuration" =>
          //              println(event)
          //            case "GC TLAB Configuration" =>
          //              handleTlabConfiguration(event)
          case _ =>
        }
        totals.totalEvents.incrementAndGet()
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  var tlabSize = 2048L

  def handleTlabConfiguration(event: IEvent): Unit = {
    //we get this event early in the JFR file
    val usesTlab = event.getValue("usesTLABs").asInstanceOf[Boolean]
    if (usesTlab) {
      tlabSize = event.getValue("minTLABSize").asInstanceOf[Long]
    }
  }


  def addDerated(frames: Seq[CallSite], value: Long)
                (fn: (CallSite, Double) => Unit) = {
    val step = 1.0D / frames.size
    frames.foldLeft(1.0) {
      case (remaining, (site)) =>
        fn(site, remaining)
        remaining - step
    }
  }

  def allocation(event: IEvent, isTLAB: Boolean): Unit = {
    val thread = event.getValue("(thread)").asInstanceOf[FLRThread]
    if ((thread ne null) && !includeThread(thread.getName)) {
      totals.ignoredThreadAllocationEvents.incrementAndGet()
    } else {
      val distinctFrames: List[CallSite] = readFrames(event)
      if (!includeStack(distinctFrames)) {
        totals.ignoredStackAllocationEvents.incrementAndGet()
      } else {

        totals.consumedAllocationEvents.incrementAndGet()
        val distinctUserFrames: List[CallSite] = distinctFrames.filter(_.isUserFrame)
        val cls = event.getValue("class").asInstanceOf[FLRType]
        val clazz = cls.toString
        val detectedAllocation = event.getValue("allocationSize").asInstanceOf[Long]
        val allocatedBytes = if (isTLAB) {
          //we will only see an allocation of size n for
          //tlabSize/n of the allocations
          //so we record that as  tlabSize/n*n = tlabSize
          tlabSize
        } else detectedAllocation

        totals.recordClassAllocation(clazz, allocatedBytes, detectedAllocation)

        addDerated(distinctFrames, allocatedBytes) {
          (site: CallSite, value: Double) =>
            site.allDeratedAllocatedBytes.add(value)
            site.classAllocations(clazz).allDeratedAllocatedBytes.add(value)
        }
        addDerated(distinctUserFrames, allocatedBytes) {
          (site: CallSite, value: Double) =>
            site.userDeratedAllocatedBytes.add(value)
            site.classAllocations(clazz).userDeratedAllocatedBytes.add(value)
        }

        distinctFrames.headOption.foreach(_.allLocalAllocatedBytes.addAndGet(allocatedBytes))
        distinctUserFrames.headOption.foreach(_.userLocalAllocatedBytes.addAndGet(allocatedBytes))

        distinctFrames.foreach { usage =>
          usage.transitiveAllocatedBytes.addAndGet(allocatedBytes)
          usage.classAllocations(clazz).transitiveAllocatedBytes.addAndGet(allocatedBytes)
        }
      }
    }
  }

  def cpu(event: IEvent): Unit = {
    val thread = event.getValue("(thread)").asInstanceOf[FLRThread]
    if ((thread ne null) && !includeThread(thread.getName)) {
      totals.ignoredThreadCpuEvents.incrementAndGet
    } else {
      val distinctFrames: List[CallSite] = readFrames(event)
      if (!includeStack(distinctFrames)) {
        totals.ignoredStackCpuEvents.incrementAndGet
      } else {
        totals.consumedCpuEvents.incrementAndGet
        val distinctUserFrames: List[CallSite] = distinctFrames.filter(_.isUserFrame)

        addDerated(distinctFrames, 1) {
          (site: CallSite, value: Double) =>
            site.allDeratedCpu.add(value)
        }
        addDerated(distinctUserFrames, 1) {
          (site: CallSite, value: Double) =>
            site.userDeratedCpu.add(value)
        }

        distinctFrames.headOption.foreach(_.allFirstCpu.incrementAndGet)
        distinctUserFrames.headOption.foreach(_.userFirstCpu.incrementAndGet)

        distinctFrames.foreach {
          _.transitiveCpu.incrementAndGet
        }
      }
    }
  }


  private def readFrames(event: IEvent): List[CallSite] = {
    val stack = event.getValue("(stackTrace)").asInstanceOf[FLRStackTrace]

    if (stack eq null) List() else {
      stack.getFrames.iterator.asScala.map {
        frame: IMCFrame =>
          val method = frame.getMethod
          CallSite(method.getPackageName, method.getClassName, method.getMethodName, method.getFormalDescriptor, frame.getFrameLineNumber)
      }.distinct.takeWhile { f => !f.isIgnorableTopFrame }.toList
    }
  }


}
