package topgun.cmdline

import java.io.File
import java.net.URL
import java.util

import jdk.jfr.consumer.{RecordedClass, RecordedEvent, RecordedFrame, RecordingFile}
import topgun.core.CallSite

import scala.jdk.CollectionConverters._

class FileParser(file: File, cmdLine: JfrParseCommandLine, totals: Totals, configuration: Configuration) {

  import configuration.{includeStack, includeThread}

  def parse(): Unit = {

    println(s"*** File $file")
    var classPaths = new util.ArrayList[URL]

    def preProcessing(): String = {

      var FoundAllocationInNewTlabEnabled = false
      var FoundAllocationOutsideTlabEnabled = false
      var FoundMethodProfilingSampleEnabled = false
      var isWindow: Option[Boolean] = None
      var classPath = ""

      def extractJfrSettings(event: RecordedEvent): Unit = {

        val id: Long = event.getValue("id")
        val name: String = event.getValue("name")
        val value: String = event.getValue("value")

        if (name.equals("enabled")) {
          id match {

            case EventTypeIdMapping.AllocationInNewTLAB =>
              if (value.toBoolean) FoundAllocationInNewTlabEnabled = true
              else throw new InvalidJfrSettingException(s"failed [file]${file.getName} Reason: 'Allocation in new TLAB' event not enabled")

            case EventTypeIdMapping.AllocationOutsideTlab =>
              if (value.toBoolean) FoundAllocationOutsideTlabEnabled = true
              else throw new InvalidJfrSettingException(s"failed [file]${file.getName} Reason: 'Allocation outside TLAB' event not enabled")

            case EventTypeIdMapping.MethodProfilingSample =>
              if (value.toBoolean) FoundMethodProfilingSampleEnabled = true
              else throw new InvalidJfrSettingException(s"failed [file]${file.getName} Reason: 'Method Profiling Sample' event not enabled")

            case _ =>
          }
        }
      }

      def notDoneFetchingInfo(): Boolean = {
        !FoundAllocationInNewTlabEnabled || !FoundAllocationOutsideTlabEnabled || !FoundMethodProfilingSampleEnabled ||
          isWindow.isEmpty || classPath.isEmpty
      }

      val recordingFile = new RecordingFile(file.toPath)
      while (recordingFile.hasMoreEvents && notDoneFetchingInfo()) {
        val event = recordingFile.readEvent()
        event.getEventType.getName match {

          case EventTypeNameMapping.InitialSystemProperty if classPath.isEmpty => if (event.getValue("key").toString.contains("java.class.path"))
            classPath = event.getValue("value").toString

          case EventTypeNameMapping.RecordingSetting => extractJfrSettings(event)

          case EventTypeNameMapping.OSInformation if isWindow.isEmpty =>
            isWindow = Some(event.getValue("osVersion").toString.toLowerCase.contains("win"))

          case _ =>
        }
      }
      if (notDoneFetchingInfo()) {
        throw new Exception(s"Bad jfrfile [file] ${file}")
      }
      recordingFile.close()
      ClassLoaderFactory.addIfDoesNotExist(classPath, if (isWindow.get) ";" else ":")
      classPath
    }

    val classPath = preProcessing()

    val recordingFile = new RecordingFile(file.toPath)

    while (recordingFile.hasMoreEvents) {
      val event = recordingFile.readEvent()
      event.getEventType.getName match {
        case EventTypeNameMapping.AllocationInNewTLAB => allocation(event, isTLAB = true, classPath)
        case EventTypeNameMapping.AllocationOutsideTlab => allocation(event, isTLAB = false, classPath)
        case EventTypeNameMapping.MethodProfilingSample => cpu(event, classPath)
        case EventTypeNameMapping.MethodProfilingSampleNative => cpuNative(event, classPath)
        case e =>
          totals.ignoreEvent(e)
      }
      totals.totalEvents.incrementAndGet()
    }
  }

  def addDerated(frames: Seq[CallSite], value: Long)
                (fn: (CallSite, Double) => Unit): Double = {
    val step = 1.0D / frames.size
    frames.foldLeft(1.0) {
      case (remaining, site) =>
        fn(site, remaining)
        remaining - step
    }
  }

  def allocation(event: RecordedEvent, isTLAB: Boolean, classPath: String): Unit = {
    val thread = event.getThread
    if ((thread ne null) && !includeThread(thread.getJavaName)) {
      totals.ignoredThreadAllocationEvents.incrementAndGet()
    } else {
      val distinctFrames: List[CallSite] = readFrames(event, classPath)
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

  def cpu(event: RecordedEvent, classPath: String): Unit = {
    val thread = event.getThread
    if ((thread ne null) && !includeThread(thread.getJavaName)) {
      totals.ignoredThreadCpuEvents.incrementAndGet
    } else {
      val distinctFrames: List[CallSite] = readFrames(event, classPath)
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

  def cpuNative(event: RecordedEvent, classPath: String): Unit = {
    totals.totalEventsNative.incrementAndGet()
    val thread = event.getThread
    if ((thread ne null) && !includeThread(thread.getJavaName)) {
      totals.ignoredThreadCpuEventsNative.incrementAndGet
    } else {
      val distinctFrames: List[CallSite] = readFrames(event, classPath)
      if (!includeStack(distinctFrames)) {
        totals.ignoredStackCpuEventsNative.incrementAndGet
      } else {
        totals.consumedCpuEventsNative.incrementAndGet
        val distinctUserFrames: List[CallSite] = distinctFrames.filter(_.isUserFrame)

        addDerated(distinctFrames, 1) {
          (site: CallSite, value: Double) =>
            site.nativeAllDeratedCpu.add(value)
        }
        addDerated(distinctUserFrames, 1) {
          (site: CallSite, value: Double) =>
            site.nativeUserDeratedCpu.add(value)
        }

        distinctFrames.headOption.foreach(_.nativeAllFirstCpu.incrementAndGet)
        distinctUserFrames.headOption.foreach(_.nativeUserFirstCpu.incrementAndGet)

        distinctFrames.foreach {
          _.nativeTransitiveCpu.incrementAndGet
        }
      }
    }
  }

  private def readFrames(event: RecordedEvent, classPath: String): List[CallSite] = {
    val stack = event.getStackTrace
    if (stack eq null) List() else {
      stack.getFrames.iterator.asScala.map {
        frame: RecordedFrame =>
          val method = frame.getMethod
          if (method ne null) {
            val splitIndex = method.getType.getName.lastIndexOf(".")
            val packageName = if (splitIndex != -1) method.getType.getName.substring(0, splitIndex).intern() else ""
            val className = if (splitIndex != -1) method.getType.getName.substring(splitIndex + 1).intern() else method.getType.getName.intern()

            /* The synthetic lambda apply method will call the lambda implementation method with the actual code; that method has line number info.
            so we should be able to just ignore these "$$Lambda" class frames.
            map all lambda frames to null and filter out after map*/
            if (frame.getMethod.isHidden) {
              null
            } else {
              CallSite(
                packageName,
                className,
                method.getName.intern(),
                method.getDescriptor.intern(),
                frame.getLineNumber,
                classPath.intern()
              )
            }
          } else {
            CallSite("NoMethodInFrame_", "NoMethodInFrame_", "NoMethodInFrame_", "NoMethodInFrame_", frame.getLineNumber, classPath.intern())
          }
      }.filter(_ ne null) // $Lambda frames are mapped to null and filtered out.
        .distinct.takeWhile {
        !_.isIgnorableTopFrame
      }.toList
    }
  }
}
