package topgun.cmdline

// TODO find reference values inside JMC/JFR and replace with constants.
//  RecrodedEvent.AllocatedInNewTLAB
object RecordedEventTypeIdMapping {
  val AllocationInNewTLAB = 331
  val AllocationOutsideTlab = 332
  val MethodProfilingSample = 352
  val MethodProfilingSampleNative = 353
  val RecordingSetting = 2999
  val OSInformation = 337
  val InitialSystemProperty = 338

}
