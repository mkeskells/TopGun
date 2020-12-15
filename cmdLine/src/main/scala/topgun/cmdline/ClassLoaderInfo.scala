package topgun.cmdline


import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

import scala.collection.mutable

class ClassLoaderInfo() {
  var allClasses = new mutable.HashMap[String, ClassInfo]()

  def lookup(className: String, classLoader: ClassLoader): ClassInfo = {
    allClasses.getOrElseUpdate(className, buildClassInfo(className, classLoader)): ClassInfo
  }

  def buildClassInfo(className: String, classLoader: ClassLoader): ClassInfo = {
    val resourceName = className.replace(".", "/") + ".class"
    val iStream = classLoader.getResourceAsStream(resourceName)
    val reader = new ClassReader(iStream)
    val classNode = new ClassNode()
    // specify no parsing options.
    val parsingFlagNone = 0;
    reader.accept(classNode, parsingFlagNone)
    iStream.close()
    val classNameFromReader = reader.getClassName
    val sourceFile = classNode.sourceFile
    val superClass = reader.getSuperName
    val interfaces:List[String] = reader.getInterfaces.toList
    ClassInfo(classNameFromReader, superClass, sourceFile, interfaces)
  }
}
