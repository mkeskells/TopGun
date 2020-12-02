package topgun.cmdline

import topgun.cmdline.records.AsmRecord

object JfrParse extends App {

  import org.kohsuke.args4j.CmdLineException
  import org.kohsuke.args4j.CmdLineParser

  val options = new JfrParseCommandLine
  val parser = new CmdLineParser(options)
  try {
    val path = "/home/chempa/github/Comet/trade-engine/build/classes/java/main";
    AsmRecord.constructRecord(path);
    println(AsmRecord.peek())
    parser.parseArgument(args: _*)
    options.validate(parser)
    new JfrReader(options).process()
  } catch {
    case e: CmdLineException =>
      // handling of wrong arguments

      System.err.println(e.getMessage)
      parser.printUsage(System.err)
  }

}