package topgun.cmdline

import java.io.File
import java.util.stream.Collectors

import jdk.jfr.consumer.{RecordedClass, RecordedEvent, RecordedFrame, RecordingFile}
import topgun.core.CallSite

import scala.jdk.CollectionConverters._

class FileParser(file: File, cmdLine: JfrParseCommandLine, totals: Totals, configuration: Configuration) {

  import configuration.{includeStack, includeThread}

  def verify(recordingFile: RecordingFile): Boolean = {

    val allEventsTypes = recordingFile.readEventTypes();

    val eventTypes = allEventsTypes.stream().map(eventType => eventType.getLabel).collect(Collectors.toSet[String])

    if(!eventTypes.contains("Allocation in new TLAB")){
      throw new JfrEventNotFoundException("'Allocation in new TLAB' Event not found")
    }
    if(!eventTypes.contains("Allocation outside TLAB")){
      throw new JfrEventNotFoundException("'Allocation outside TLAB' Event not found")
    }
    if(!eventTypes.contains("Method Profiling Sample")){
      throw new JfrEventNotFoundException("'Method Profiling Sample' Event not found")
    }
    return true;
  }

  def parse(): Unit = {



    println(s"*** File $file")
    try {
      val recording = new RecordingFile(file.toPath);

      try{
        verify(recording)
      }catch{
        case e: JfrEventNotFoundException => {
          System.err.printf("Failed: [file]-%s%nReason: %s",file.getName,e.getMessage);
          System.exit(-1)
        }
      }

      val view = RecordingFile.readAllEvents(file.toPath);
      for (event <- view.asScala) {
        event.getEventType.getLabel match {
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

  def addDerated(frames: Seq[CallSite], value: Long)
                (fn: (CallSite, Double) => Unit) = {
    val step = 1.0D / frames.size
    frames.foldLeft(1.0) {
      case (remaining, (site)) =>
        fn(site, remaining)
        remaining - step
    }
  }

  def allocation(event: RecordedEvent, isTLAB: Boolean): Unit = {
    val thread = event.getThread
    if ((thread ne null) && !includeThread(thread.getJavaName)) {
      totals.ignoredThreadAllocationEvents.incrementAndGet()
    } else {
      val distinctFrames: List[CallSite] = readFrames(event)
      if (!includeStack(distinctFrames)) {
        totals.ignoredStackAllocationEvents.incrementAndGet()
      } else {
        totals.consumedAllocationEvents.incrementAndGet()
        val distinctUserFrames: List[CallSite] = distinctFrames.filter(_.isUserFrame)
        val cls = event.getValue("objectClass").asInstanceOf[RecordedClass]
        val clazz = cls.getName
        val detectedAllocation = event.getValue("allocationSize").asInstanceOf[Long]

        val allocatedBytes = if (isTLAB) {
          //we will only see an allocation of size n for
          //tlabSize/n of the allocations
          //so we record that as  tlabSize/n*n = tlabSize

          event.getValue("tlabSize").asInstanceOf[Long]
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

  def cpu(event: RecordedEvent): Unit = {
    val thread = event.getThread
    if ((thread ne null) && !includeThread(thread.getJavaName)) {
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

  private def readFrames(event: RecordedEvent): List[CallSite] = {
    val stack = event.getStackTrace

    if (stack eq null) List() else {
      stack.getFrames.iterator.asScala.map {
        frame: RecordedFrame =>
          val method = frame.getMethod
          CallSite(method.getType.getName, method.getName, method.getName, method.getDescriptor, frame.getLineNumber)
      }.distinct.takeWhile { f => !f.isIgnorableTopFrame }.toList
    }
  }
}
