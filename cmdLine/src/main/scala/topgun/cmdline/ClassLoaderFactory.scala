package topgun.cmdline

import java.net.URLClassLoader
import java.nio.file.Paths

import scala.collection.mutable

object ClassLoaderFactory {
  private val classLoaderHashMap = mutable.HashMap.empty[String, ClassLoader]
  val classLoaderInfo = new ClassLoaderInfo
  def getClassLoader(classPath: String): ClassLoader = {
    classLoaderHashMap(classPath)
  }

  def addIfDoesNotExist(classPath: String, classPathDelimiter: String): Unit = {
    classLoaderHashMap.getOrElseUpdate(classPath, new URLClassLoader(classPath.split(classPathDelimiter).map(path => Paths.get(path).toUri.toURL)))
  }
}
