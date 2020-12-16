package topgun.cmdline

import java.io.File
import java.nio.file.Files

import topgun.core.CallSite

class JfrReader(cmdLine: JfrParseCommandLine) {

  val totals = new Totals
  val configuration = new Configuration((cmdLine))

  def process() {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

    println("deleting old files")
    cmdLine.outDir.listFiles().toList.filter {
      _.getName.endsWith(".csv")
    }.foreach(file => Files.delete(file.toPath))
    val files:List[File] = if (cmdLine.jfr.isDirectory)
      cmdLine.jfr.listFiles().toList.filter(f => f.isFile && f.getName.endsWith(".jfr"))
    else List(cmdLine.jfr)

    files.foreach {file =>
      new FileParser(file, cmdLine, totals, configuration).parse()
    }

    val callSitesList: List[CallSite] = CallSite.allSites.toList
    println("writing files")
    def writeAggregationOfView(view: AggregateView): Unit = {
      val viewName = view.name().toLowerCase.replace("_","-")
      FileWriter.writeFile(cmdLine.outDir, s"${viewName}-allocations", callSitesList.filter(_.view.equals(view)).filter(_.userDeratedAllocatedBytes.get > 0).sortBy(_.userDeratedAllocatedBytes.get))
      FileWriter.writeFile(cmdLine.outDir, s"${viewName}-CPU", callSitesList.filter(_.view.equals(view)).filter(_.userDeratedCpu.get > 0).sortBy(_.userDeratedCpu.get))
      FileWriter.writeFile(cmdLine.outDir, s"${viewName}-CPU_Native", callSitesList.filter(_.view.equals(view)).filter(_.nativeUserDeratedCpu.get > 0).sortBy(_.nativeUserDeratedCpu.get))
      FileWriter.writeFile(cmdLine.outDir, s"${viewName}-ALL", callSitesList.filter(_.view.equals(view)).sortBy(_.FQN))

      println("writing class allocation files")
      var classRecord = 0D
      val inc = 100.0D / callSitesList.size
      var classReported = 10
      for (site <- callSitesList.sortBy(_.FQN)) {
        classRecord += inc
        if (classRecord > classReported) {
          println(s"$classReported %")
          classReported += 10
        }
        for ((className, allocation) <- site.classesAndAllocation
             if (totals.allocatedBytes(className) > cmdLine.minAllocationByClass);
             if (allocation.transitiveAllocatedBytes.get > 0)) {
          FileWriter.appendFile(cmdLine.outDir, s"${viewName}-allocation-$className", site, className, allocation)
        }
      }
      println()
      callSitesList.sortBy(-_.userDeratedAllocatedBytes.get).take(100) foreach {
        site => println(s"Allocation - count ${site.userDeratedAllocatedBytes.get} site ${site.toStackTrace}")
      }
      callSitesList.sortBy(-_.userDeratedCpu.get).take(100) foreach {
        site => println(s"CPU - count ${site.userDeratedCpu.get} site ${site.toStackTrace}")
      }
    }

    FileWriter.writeAllocationSummary(cmdLine.outDir, "alloc-by-class", totals.allAllocations.toList.sortBy(_._2.totalExpandedAllocation.get))
    AggregateView.values().foreach(frames => {  writeAggregationOfView(frames) })

    println("finishing")
    FileWriter.closeAllFiles
    System.out.println(s"Found                         ${totals.totalEvents} events")
    System.out.println(s"Ignored                       ${totals.ignoredEvents} events")

    System.out.println(s"Found Native                  ${totals.totalEventsNative} events")
    totals.ignoredSummary.foreach {
      case (name, value) =>
        System.out.println(s"Ignored                       ${value} $name events")
    }
    System.out.println(s"\nNon-Native")
    System.out.println(s"CPU        - consumed         ${totals.consumedCpuEvents}  events")
    System.out.println(s"CPU        - ignored (stack)  ${totals.ignoredStackCpuEvents}  events")
    System.out.println(s"CPU        - ignored (thread) ${totals.ignoredThreadCpuEvents}  events")
    System.out.println(s"Allocation - consumed         ${totals.consumedAllocationEvents}  events")
    System.out.println(s"Allocation - ignored (stack)  ${totals.ignoredStackAllocationEvents}  events")
    System.out.println(s"Allocation - ignored (thread) ${totals.ignoredThreadAllocationEvents}  events")
    System.out.println(s"\nNative")
    System.out.println(s"CPU        - consumed         ${totals.consumedCpuEventsNative}  events")
    System.out.println(s"CPU        - ignored (stack)  ${totals.ignoredStackCpuEventsNative}  events")
    System.out.println(s"CPU        - ignored (thread) ${totals.ignoredThreadCpuEventsNative}  events")
  }
}
