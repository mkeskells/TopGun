package topgun.cmdline

// TODO find reference values inside JMC/JFR and replace with constants.
//  RecrodedEvent.AllocatedInNewTLAB
object EventTypeIdMapping {
  val AllocationInNewTLAB = 331
  val AllocationOutsideTlab = 332
  val MethodProfilingSample = 352
  val MethodProfilingSampleNative = 353
}
