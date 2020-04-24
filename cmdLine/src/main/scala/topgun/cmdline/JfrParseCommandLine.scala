package topgun.cmdline
import java.io.File

import org.kohsuke.args4j._


class JfrParseCommandLine {
  @Option(name="--jfr", required = true,usage="the input filename" )
  var jfr:File = null

}
