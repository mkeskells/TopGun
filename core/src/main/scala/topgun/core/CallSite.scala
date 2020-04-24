package topgun.core

import scala.collection.mutable

object CallSite {
  private val all = new mutable.HashMap[CallSite, CallSite]()
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
