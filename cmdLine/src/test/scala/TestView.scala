import org.junit.{Assert, Before, Test}
import org.kohsuke.args4j.CmdLineParser
import topgun.cmdline._
import topgun.core.CallSite
import java.nio.file.Paths

class TestView {
  val param:Array[String] = Array("--jfr", getAbsolutePath, "--outDir", getTempDirectory, "--userCodePackagePrefix", "com.tradeengine.trade.engine")
  val options = new JfrParseCommandLine
  val parser = new CmdLineParser(options)

  @Before
  def setup(): Unit = {
    parser.parseArgument(param: _*)
    options.validate(parser)
  }

  //Getting the Absolute Path for the jfr file.
  private def getAbsolutePath = {
    val res = getClass.getClassLoader.getResource("jfr_file/scala.jfr")
    val file = Paths.get(res.toURI).toFile
    file.getAbsolutePath
  }

  private def getTempDirectory = System.getProperty("java.io.tmpdir")

  @Test def testAggregation(): Unit = {
    val totals = new Totals
    new FileParser(options.jfr, options, totals, new Configuration(options)).parse()

    val callSitesList: List[CallSite] = CallSite.allSites.toList

    val pcmlv = callSitesList.filter(_.view.equals(AggregateView.PACKAGE_CLASSNAME_METHOD_LINE_VIEW))
    val pcmv = callSitesList.filter(_.view.equals(AggregateView.PACKAGE_CLASSNAME_METHOD_VIEW))
    val pcv = callSitesList.filter(_.view.equals(AggregateView.PACKAGE_CLASSNAME_VIEW))
    val pslv = callSitesList.filter(_.view.equals(AggregateView.PACKAGE_SOURCE_LINE_VIEW))
    val psmv = callSitesList.filter(_.view.equals(AggregateView.PACKAGE_SOURCE_METHOD_VIEW))
    val pv = callSitesList.filter(_.view.equals(AggregateView.PACKAGE_VIEW))

    Assert.assertEquals(1120, pcmlv.length,0.0)
    Assert.assertEquals(707.0, pcmv.length, 0.0)
    Assert.assertEquals(314.0, pcv.length, 0.0)
    Assert.assertEquals(1066.0, pslv.length, 0.0)
    Assert.assertEquals(653.0, psmv.length, 0.0)
    Assert.assertEquals(50.0, pv.length, 0.0)


    Assert.assertEquals(39483.267, pcmlv.map(_.allDeratedAllocatedBytes.get).toList.max, 0.0)
    Assert.assertEquals(70446.972, pcmv.map(_.allDeratedAllocatedBytes.get).toList.max, 0.0)
    Assert.assertEquals(162386.276, pcv.map(_.allDeratedAllocatedBytes.get).toList.max, 0.0)

    Assert.assertEquals(7082.0, pcmlv.map(_.nativeAllDeratedCpu.get).toList.max, 0.0)
    Assert.assertEquals(8726.326, pcmv.map(_.nativeAllDeratedCpu.get).toList.max, 0.0)
    Assert.assertEquals(22358.812, pcv.map(_.nativeAllDeratedCpu.get).toList.max, 0.0)
    Assert.assertEquals(7082.0, pslv.map(_.nativeAllDeratedCpu.get).toList.max,0.0)
    Assert.assertEquals(7082.0, psmv.map(_.nativeAllDeratedCpu.get).toList.max, 0.0)
    Assert.assertEquals(33257.925, pv.map(_.nativeAllDeratedCpu.get).toList.max, 0.0)


    Assert.assertEquals(877.0, pcmlv.map(_.transitiveCpu.get).toList.max, 0.0)
    Assert.assertEquals(1269.0, pcmv.map(_.transitiveCpu.get).toList.max, 0.0)
    Assert.assertEquals(4988.0, psmv.map(_.transitiveCpu.get).toList.max, 0.0)


    Assert.assertEquals(7082.0, pcmlv.map(_.nativeAllFirstCpu.get).toList.max, 0.0)
    Assert.assertEquals(7082.0, pcmv.map(_.nativeAllFirstCpu.get).toList.max, 0.0)
    Assert.assertEquals(10436.0, pv.map(_.nativeAllFirstCpu.get).toList.max, 0.0)


    Assert.assertEquals(0.0, pcmlv.map(_.nativeUserFirstCpu.get).toList.max, 0.0)
    Assert.assertEquals(0.0, pcmv.map(_.nativeUserFirstCpu.get).toList.max, 0.0)
    Assert.assertEquals(0.0, pcv.map(_.nativeUserFirstCpu.get).toList.max, 0.0)

    Assert.assertEquals(199.0, pcmlv.map(_.allFirstCpu.get).toList.max, 0.0)
    Assert.assertEquals(204.0, pcmv.map(_.allFirstCpu.get).toList.max, 0.0)
    Assert.assertEquals(420.0, pcv.map(_.allFirstCpu.get).toList.max, 0.0)

    Assert.assertEquals(380.207, pcmlv.map(_.allDeratedCpu.get).toList.max, 0.0)
    Assert.assertEquals(435.561, pslv.map(_.allDeratedCpu.get).toList.max, 0.0)
    Assert.assertEquals(2783.747, pv.map(_.allDeratedCpu.get).toList.max, 0.0)





  }
}
