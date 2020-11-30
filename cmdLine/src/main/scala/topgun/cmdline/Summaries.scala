package topgun.cmdline

import java.util.concurrent.atomic.{AtomicLong, LongAdder}

import scala.collection.mutable

class Totals {

  def allAllocations: Iterator[(String, ClassTotals)] = classTotals.iterator

  def allocatedBytes(className: String): Long =
    classTotals.get(className).map(_.totalActualAllocation.get).getOrElse(0L)


  val classTotals = new mutable.HashMap[String, ClassTotals]()

  def recordClassAllocation(clazz: String, recordedBytes: Long, actualDetectedAllocation: Long): Unit = {
    val tot = classTotals.getOrElseUpdate(clazz, new ClassTotals(clazz))

    tot.maxDetected.updateAndGet(Math.max(_, actualDetectedAllocation))
    tot.minDetected.updateAndGet(Math.min(_, actualDetectedAllocation))

    tot.totalActualAllocation.addAndGet(actualDetectedAllocation)
    tot.totalExpandedAllocation.addAndGet(recordedBytes)
    tot.totalAllocationRecords.incrementAndGet()
  }

  private val _ignoredEvents = new mutable.HashMap[String, AtomicLong]()

  def ignoreEvent(eventName: String): Unit = {
    _ignoredEvents.getOrElseUpdate(eventName, new AtomicLong).incrementAndGet()
    ignoredEvents.incrementAndGet()
  }
  def ignoredSummary: List[(String, Long)] = _ignoredEvents.map{ case (k,v) => (k, v.get())}.toList.sortBy(_._1)
  var totalEvents= new AtomicLong
  var ignoredEvents= new AtomicLong
  var consumedCpuEvents= new AtomicLong
  var ignoredThreadCpuEvents= new AtomicLong
  var ignoredStackCpuEvents= new AtomicLong
  var consumedAllocationEvents= new AtomicLong
  var ignoredThreadAllocationEvents= new AtomicLong
  var ignoredStackAllocationEvents= new AtomicLong

}
class ClassTotals(val clazz: String) {
  var minDetected= new AtomicLong(Long.MaxValue)
  var maxDetected= new AtomicLong(Long.MinValue)
  var totalActualAllocation = new AtomicLong
  var totalExpandedAllocation = new AtomicLong
  var totalAllocationRecords = new AtomicLong
}
