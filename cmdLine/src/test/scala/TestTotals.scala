import org.junit.{Assert, Before, Test}
import org.kohsuke.args4j.CmdLineParser
import topgun.cmdline.{Configuration, FileParser, JfrParseCommandLine, Totals}

import java.nio.file.Paths

class TestTotals {
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
    val res = getClass.getClassLoader.getResource("jfr_files/flight_recording_1108comintellijideaMain4564_2020-11-30_12-48-18_ae084800.jfr")
    val file = Paths.get(res.toURI).toFile
    file.getAbsolutePath
  }

  private def getTempDirectory = System.getProperty("java.io.tmpdir")

  //Test for the Totals of Events
  @Test def testTotals(): Unit = {
    val totals = new Totals
    new FileParser(options.jfr, options, totals, new Configuration(options)).parse()
    Assert.assertEquals(134, totals.consumedCpuEvents.get)
    Assert.assertEquals(0, totals.ignoredThreadAllocationEvents.get)
    Assert.assertEquals(0, totals.ignoredStackAllocationEvents.get)
    Assert.assertEquals(0, totals.ignoredStackCpuEvents.get)
    Assert.assertEquals(1722, totals.consumedAllocationEvents.get)
    Assert.assertEquals(0, totals.ignoredThreadCpuEvents.get)
    Assert.assertEquals(41273, totals.ignoredEvents.get)
    Assert.assertEquals(46072, totals.totalEvents.get)
  }
}
