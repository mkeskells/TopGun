package topgun.cmdline
import java.io.File
import scala.{Option => SOption}

import org.kohsuke.args4j._

class JfrParseCommandLine {
  @Option(name="--jfr",  metaVar="DIR/FILE", required = true,usage="the input filename or directory" )
  var jfr:File = null

  @Option(name="--outDir", metaVar="DIR", required = true,usage="the output directory. Used to generate multiple CSVs" )
  var outDir:File = null

  @Option(name="--userCodePackagePrefix", metaVar="com.foo:com.bar", required = true,usage="what packages contain user code" )
  var _packagePrefix:String = "com:org"
  lazy val packagePrefix = _packagePrefix.split(":").toList

  @Option(name="--includeThreads", required = false,usage="regex for the thread name to include", forbids = Array("--excludeThreads") )
  var includeThreads:String = null
  def includeThreadsRegex = SOption(includeThreads).map(_.r)

  @Option(name="--excludeThreads", required = false,usage="regex for the thread name to exclude", forbids = Array("--includeThreads"))
  var excludeThreads:String = null
  def excludeThreadsRegex = SOption(excludeThreads).map(_.r)

  @Option(name="--ignoreTopFrames", metaVar="FILE", required = false,usage="frames to ignore. One per line a list of frame prefix for frames that are the top of the stack" )
  var ignoreTopFrames:File = _

  @Option(name="--excludeStacksContaining", metaVar="FILE", required = false,usage="Ignore stack traces that contain these frames. One per line - a list of frame prefix" )
  var ignoreContainingFrames:File = _

  @Option(name="--requireStacksContaining", metaVar="FILE", required = false,usage="Only include stack traces that contain these frames. One per line - a list of regular expressions" )
  var requireContainingFrames:File = _

  @Option(name="--aggregate-each", required = false,usage="produce aggregate views",handler = classOf[AggregateViewOptionHandler])
  var aggregate:java.util.EnumSet[AggregateView] = _

  @Option(name="--minAllocationByClass", metaVar="INT", required = false,usage="Ignore stack traces that contain these frames. One per line a list of frame prefix" )
  var minAllocationByClass:Long = 0

  def validate(parser:CmdLineParser) :Unit = {
    if (!outDir.isDirectory) throw new CmdLineException(parser, s"--outDir ($outDir) is not a directory")
    if (!outDir.canWrite) throw new CmdLineException(parser, s"--outDir ($outDir) is not writeable")
  }

}
