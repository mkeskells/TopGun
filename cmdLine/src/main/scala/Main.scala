//package topgun.cmdline
//
//import topgun.core._
//import java.io.File
//
//import scala.collection.mutable
//
//object JFRParse extends App {
//
//  val callSites = mutable.HashMap[CallSite, CallSiteInfo]()
//
//  val recording = FlightRecordingLoader.loadFile(new File("S:\\recording.jfr"))
//  for (t <- recording.getEventTypes) {
//    println(t.getName)
//  }
//  val view = recording.createView
//  var count = 0
//
//  def allocation(event: IEvent): Unit = {
//    println(event)
//  }
//
//  def cpu(event: IEvent): Unit = {
//    val stack = event.getValue("(stackTrace)").asInstanceOf[FLRStackTrace]
//    val thread = event.getValue("(thread)").asInstanceOf[FLRThread]
//
//    val fullFrames: List[CallSite] = stack.getFrames.toList map {
//      frame: IMCFrame =>
//        val method = frame.getMethod
//        CallSite(method.getPackageName, method.getClassName, method.getMethodName, method.getFormalDescriptor, frame.getFrameLineNumber)
//    }
//    val distinctFramesAndUsage = fullFrames.distinct map { f => (f, callSites.getOrElseUpdate(f, new CallSiteInfo)) }
//
//    distinctFramesAndUsage.foreach {
//      case (frame, usage) =>
//        usage.cpuTicks += 1
//    }
//    println(event)
//  }
//
//  for (event <- view) {
//    event.getEventType.getName match {
//      case "Allocation in new TLAB" => allocation(event)
//      case "Allocation outside TLAB" => allocation(event)
//      case "Method Profiling Sample" => cpu(event)
//      case _ =>
//    }
//    count += 1
//  }
//  callSites.toList.sortBy(-_._2.cpuTicks) foreach {
//    case (site, usage) => println(s"count ${usage.cpuTicks} site $site")
//  }
//  System.out.println("Fount " + count + " events")
//
//}
