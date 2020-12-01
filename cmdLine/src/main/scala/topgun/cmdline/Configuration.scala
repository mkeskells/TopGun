package topgun.cmdline

import java.nio.file.Files

import topgun.core.CallSite
import scala.jdk.CollectionConverters._


class Configuration(cmdLine: JfrParseCommandLine) {

  def isUserPackage(frame: CallSite): Boolean = {
    val packageName = frame.packageName
    cmdLine.packagePrefix.exists {
      packageName.startsWith
    }
  }

  val ignoredTopFrames = if (cmdLine.ignoreTopFrames eq null) Nil else {
    Files.readAllLines(cmdLine.ignoreTopFrames.toPath).asScala.toList
  }
  CallSite.ignorableTopFrame = isIgnoredTopFrame
  CallSite.userFrame = isUserPackage

  def isIgnoredTopFrame(frame: CallSite): Boolean = {
    val fqn = frame.FQN
    ignoredTopFrames.exists {
      fqn.startsWith
    }
  }

  val includeThread: (String => Boolean) = (cmdLine.excludeThreadsRegex, cmdLine.includeThreadsRegex) match {
    case (None, None) => (_ => true)
    case (Some(ex), None) => x => !ex.matches(x)
    case (None, Some(inc)) => inc.matches
    case (_, _) => ???
  }


  val ignoredStack = if (cmdLine.ignoreContainingFrames eq null) Nil else {
    Files.readAllLines(cmdLine.ignoreContainingFrames.toPath).asScala.toList
  }
  val requireStackRegex = if (cmdLine.requireContainingFrames eq null) Nil else {
    Files.readAllLines(cmdLine.requireContainingFrames.toPath).asScala.toList.map(_.r.pattern.matcher(""))
  }

  def includeStack(stack: List[CallSite]): Boolean = {
    val ignore = if (ignoredStack.isEmpty) false
    else stack.exists { site =>
      val fqn = site.FQN
      !ignoredStack.exists {
        fqn.startsWith
      }
    }
    val include = !ignore && {
      if (requireStackRegex.isEmpty) true
      else stack.exists { site =>
        val fqn = site.FQN
        requireStackRegex.exists { matcher =>
          matcher.reset(fqn)
          matcher.matches()
        }
      }
    }
    include
  }


}
