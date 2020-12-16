package topgun.cmdline

object EventTypeNameMapping {
  val AllocationInNewTLAB = "jdk.ObjectAllocationInNewTLAB"
  val AllocationOutsideTLAB = "jdk.ObjectAllocationOutsideTLAB"
  val MethodProfilingSample = "jdk.ExecutionSample"
  val MethodProfilingSampleNative = "jdk.NativeMethodSample"
  val InitialSystemProperty = "jdk.InitialSystemProperty"
  val RecordingSetting = "jdk.ActiveSetting"
  val OSInformation = "jdk.OSInformation"
}
