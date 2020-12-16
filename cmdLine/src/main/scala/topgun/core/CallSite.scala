package topgun.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import topgun.cmdline.AggregateView

import scala.collection.mutable
import scala.util.hashing.MurmurHash3

object CallSite {
  private val USER_FRAME = 1
  private val IGNORABLE_TOP_FRAME = 2
  private val all = new mutable.HashMap[CallSite, CallSite]()
  var userFrame: CallSite => Boolean = _
  var ignorableTopFrame: CallSite => Boolean = _

  def allSites = all.keysIterator

  def configureFlags(site: CallSite): Int = {
    var flags: Int = 0
    if (userFrame(site)) flags |= USER_FRAME
    if (ignorableTopFrame(site)) flags |= IGNORABLE_TOP_FRAME

    flags
  }

  def apply(packageName: String, className: String, methodName: String, desc: String, line: Int, view: AggregateView, fileName: String): CallSite = {
    val site = new CallSite(packageName, className, methodName, desc, line, view, fileName)
    all.getOrElseUpdate(site, site)
  }
}

class CallSite private(val packageName: String, val className: String, val methodName: String, val desc: String, val line: Int, val view: AggregateView, val fileName: String) extends CallSiteInfo {

  lazy val flags = CallSite.configureFlags(this)
  val FQN = s"$packageName.$className.$methodName$desc:$line"

  override def toString = FQN

  def isUserFrame = (flags & CallSite.USER_FRAME) != 0

  def isIgnorableTopFrame = (flags & CallSite.IGNORABLE_TOP_FRAME) != 0

  def toStackTrace: String = s"at $packageName.$className.$methodName($fileName:$line)"

  override def equals(other: Any): Boolean = other match {
    case that: CallSite => (this eq that) ||
      (that canEqual this) &&
        packageName == that.packageName &&
        className == that.className &&
        methodName == that.methodName &&
        desc == that.desc &&
        view == that.view &&
        fileName == that.fileName &&
        line == that.line
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[CallSite]

  override def hashCode(): Int = {
    var hash = 99
    hash = MurmurHash3.mix(hash, packageName.hashCode)
    hash = MurmurHash3.mix(hash, className.hashCode)
    hash = MurmurHash3.mix(hash, methodName.hashCode)
    hash = MurmurHash3.mix(hash, desc.hashCode)
    hash = MurmurHash3.mixLast(hash, line)
    hash
  }
}

trait AllocationCounts {
  val transitiveAllocatedBytes = new AtomicLong

  val allDeratedAllocatedBytes = new AtomicDouble
  val userDeratedAllocatedBytes = new AtomicDouble

  val allLocalAllocatedBytes = new AtomicLong
  val userLocalAllocatedBytes = new AtomicLong
}

trait CpuCounts {
  val transitiveCpu = new AtomicLong

  val allDeratedCpu = new AtomicDouble
  val userDeratedCpu = new AtomicDouble

  val allFirstCpu = new AtomicLong
  val userFirstCpu = new AtomicLong

  //  NATIVE
  val nativeTransitiveCpu = new AtomicLong

  val nativeAllDeratedCpu = new AtomicDouble
  val nativeUserDeratedCpu = new AtomicDouble

  val nativeAllFirstCpu = new AtomicLong
  val nativeUserFirstCpu = new AtomicLong
}

class ClassAllocations extends AllocationCounts

trait CallSiteInfo extends AllocationCounts with CpuCounts {

  import scala.jdk.CollectionConverters._

  private val _classAllocations = new ConcurrentHashMap[String, ClassAllocations]

  def classAllocations(clazz: String) = {
    _classAllocations.computeIfAbsent(clazz, _ => new ClassAllocations)
  }

  def classesAndAllocation = _classAllocations.entrySet().iterator().asScala.map(e => (e.getKey, e.getValue))

}

class AtomicDouble {
  private val scaledValue = new AtomicLong

  def add(value: Double): Unit = scaledValue.addAndGet((value * 1000).toLong)

  def get = scaledValue.get.toDouble / 1000
}
