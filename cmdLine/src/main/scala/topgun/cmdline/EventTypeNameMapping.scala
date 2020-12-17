package topgun.cmdline

// TODO find reference values inside JMC/JFR and replace with constants.
//  RecrodedEvent.AllocatedInNewTLAB
object EventTypeNameMapping {
  val AllocationInNewTLAB = "jdk.ObjectAllocationInNewTLAB"
  val AllocationOutsideTlab = "jdk.ObjectAllocationOutsideTLAB"
  val MethodProfilingSample = "jdk.ExecutionSample"
  val MethodProfilingSampleNative = "jdk.NativeMethodSample"
  val InitialSystemProperty = "jdk.InitialSystemProperty"
  val RecordingSetting = "jdk.ActiveSetting"
  val OSInformation = "jdk.OSInformation"
}
