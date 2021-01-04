package topgun.cmdline

import java.io._
import topgun.core.{CallSite, ClassAllocations}

import scala.collection.mutable

object FileWriter {
  val classAllocationFields: List[(String, (CallSite, String, ClassAllocations) => Any)] = {
    List(
      ("packageName", (site: CallSite, className: String, allocations: ClassAllocations) => site.packageName),
      ("sourceFile", (site: CallSite, className: String, allocations: ClassAllocations) => site.fileName),
      ("className", (site: CallSite, className: String, allocations: ClassAllocations) => site.className),
      ("methodName", (site: CallSite, className: String, allocations: ClassAllocations) => site.methodName),
      ("desc", (site: CallSite, className: String, allocations: ClassAllocations) => site.desc),
      ("line", (site: CallSite, className: String, allocations: ClassAllocations) => site.line),
      ("toStackTrace", (site: CallSite, className: String, allocations: ClassAllocations) => site.toStackTrace),
      ("allocated", (site: CallSite, className: String, allocations: ClassAllocations) => className),
      ("class_transitiveAllocatedBytes", (site: CallSite, className: String, allocations: ClassAllocations) => allocations.transitiveAllocatedBytes.get),
      ("class_allDeratedAllocatedBytes", (site: CallSite, className: String, allocations: ClassAllocations) => allocations.allDeratedAllocatedBytes.get),
      ("class_userDeratedAllocatedBytes", (site: CallSite, className: String, allocations: ClassAllocations) => allocations.userDeratedAllocatedBytes.get),
      ("class_allLocalAllocatedBytes", (site: CallSite, className: String, allocations: ClassAllocations) => allocations.allLocalAllocatedBytes.get),
      ("class_userLocalAllocatedBytes", (site: CallSite, className: String, allocations: ClassAllocations) => allocations.userLocalAllocatedBytes.get)
    )
  }
  val files = mutable.Map[String, BufferedWriter]()

  def writeAllocationSummary(directory: File, fileName: String, lines: List[(String, ClassTotals)]): Unit = {
    val file = new File(directory, s"$fileName.csv")
    val w = new BufferedWriter(new FileWriter(file))
    val data: List[(String, (String, ClassTotals) => Any)] = {
      List(
        ("className", (className: String, totals: ClassTotals) => className),
        ("minDetected", (className: String, totals: ClassTotals) => totals.minDetected),
        ("maxDetected", (className: String, totals: ClassTotals) => totals.maxDetected),
        ("totalActualAllocation", (className: String, totals: ClassTotals) => totals.totalActualAllocation),
        ("totalExpandedAllocation", (className: String, totals: ClassTotals) => totals.totalExpandedAllocation),
        ("totalAllocationRecords", (className: String, totals: ClassTotals) => totals.totalAllocationRecords)
      )
    }
    for ((heading, _) <- data) {
      w.write(heading)
      w.write(",")
    }
    w.newLine()
    for ((site, info) <- lines) {
      for ((_, access) <- data) {
        w.write(s"${access(site, info)},")
      }
      w.newLine()
    }
    w.flush()
    w.close()

  }

  def writeFile(directory: File, fileName: String, lines: List[CallSite]): Unit = {
    val file = new File(directory, s"$fileName.csv")
    val w = new BufferedWriter(new FileWriter(file))

    val data: List[(String, (CallSite => Any))] = {
      List(
        ("packageName", (site: CallSite) => site.packageName),
        ("sourceFile", (site: CallSite) => site.fileName),
        ("className", (site: CallSite) => site.className),
        ("methodName", (site: CallSite) => site.methodName),
        ("desc", (site: CallSite) => site.desc),
        ("line", (site: CallSite) => site.line),
        ("toStackTrace", (site: CallSite) => site.toStackTrace),
        ("transitiveAllocatedBytes", (site: CallSite) => site.transitiveAllocatedBytes.get),
        ("allDeratedAllocatedBytes", (site: CallSite) => site.allDeratedAllocatedBytes.get),
        ("userDeratedAllocatedBytes", (site: CallSite) => site.userDeratedAllocatedBytes.get),
        ("allLocalAllocatedBytes", (site: CallSite) => site.allLocalAllocatedBytes.get),
        ("userLocalAllocatedBytes", (site: CallSite) => site.userLocalAllocatedBytes.get),
        ("transitiveCpu", (site: CallSite) => site.transitiveCpu.get),
        ("allDeratedCpu", (site: CallSite) => site.allDeratedCpu.get),
        ("userDeratedCpu", (site: CallSite) => site.userDeratedCpu.get),
        ("allFirstCpu", (site: CallSite) => site.allFirstCpu.get),
        ("userFirstCpu", (site: CallSite) => site.userFirstCpu.get),
        ("nativeTransitiveCpu", (site: CallSite) => site.nativeTransitiveCpu.get),
        ("nativeAllDeratedCpu", (site: CallSite) => site.nativeAllDeratedCpu.get),
        ("nativeUserDeratedCpu", (site: CallSite) => site.nativeUserDeratedCpu.get),
        ("nativeAllFirstCpu", (site: CallSite) => site.nativeAllFirstCpu.get),
        ("nativeUserFirstCpu", (site: CallSite) => site.nativeUserFirstCpu.get)
      )
    }
    for ((heading, _) <- data) {
      w.write(heading)
      w.write(",")
    }
    w.newLine()
    for (site <- lines) {
      for ((_, access) <- data) {
        w.write(s"${access(site)},")
      }
      w.newLine()
    }
    w.flush()
    w.close()
  }

  def appendFile(directory: File, fileName: String, site: CallSite, className: String, allocation: ClassAllocations): Unit = {
    val w: BufferedWriter = files.getOrElseUpdate(fileName, {
      var name = fileName.replace(':', '_').replace('/', '.').replace('$', '_')
      if (name.length > 100)
        name = name.take(100) + "..."
      val file = new File(directory, s"$name.csv")

      file.createNewFile()
      val w = new BufferedWriter(new FileWriter(file))
      for ((heading, _) <- classAllocationFields) {
        w.write(heading)
        w.write(",")
      }
      w.newLine()
      w
    })
    for ((_, access) <- classAllocationFields) {
      w.write(s"${access(site, className, allocation)},")
    }
    w.newLine()
  }

  def closeAllFiles(): Unit = {
    files.values foreach { f => f.flush(); f.close() }
    files.clear()
  }
}
