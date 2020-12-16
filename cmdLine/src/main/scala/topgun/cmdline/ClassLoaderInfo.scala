package topgun.cmdline


import java.io.IOException

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

import scala.collection.mutable

class ClassLoaderInfo() {
  var allClasses = new mutable.HashMap[String, ClassInfo]()

  def lookup(className: String, classLoader: ClassLoader): ClassInfo = {
    allClasses.getOrElseUpdate(className, buildClassInfo(className, classLoader)): ClassInfo
  }

  private def buildClassInfo(className: String, classLoader: ClassLoader): ClassInfo = {
    val resourceName = className.replace(".", "/") + ".class"
    val iStream = classLoader.getResourceAsStream(resourceName)
    if (iStream == null) {
      return ClassInfo(className, "super not found", "sourceFile not found", List())
    }
    val reader = new ClassReader(iStream)
    val classNode = new ClassNode()
    // specify no parsing options
    reader.accept(classNode, 0)
    iStream.close()
    val classNameFromReader = reader.getClassName
    val sourceFile = classNode.sourceFile
    val superClass = reader.getSuperName
    val interfaces: List[String] = reader.getInterfaces.toList
    ClassInfo(classNameFromReader, superClass, sourceFile, interfaces)
  }
}
