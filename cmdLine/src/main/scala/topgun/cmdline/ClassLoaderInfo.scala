package topgun.cmdline

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.{ClassNode, LineNumberNode}

import scala.collection.mutable

class ClassLoaderInfo() {
  val classMethodProfiles = new mutable.HashMap[String, ClassSchema]()
  val allClasses = new mutable.HashMap[String, ClassInfo]()

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

    def profileClass(): Unit = {
      val classSchema = new ClassSchema();
      classNode.methods.stream().filter(!_.name.equals("<init>")).filter(!_.name.equals("<clinit>"))
        .forEach(method => {
          val instLines = method.instructions.toArray.filter(_.isInstanceOf[LineNumberNode]).map(_.asInstanceOf[LineNumberNode]).map(_.line)
          if (!instLines.isEmpty)
            classSchema.add(new MethodSchema(method.name, method.desc, instLines.min, instLines.max))
        })
      classMethodProfiles.put(className, classSchema)
    }

    profileClass()
    val classNameFromReader = reader.getClassName
    val sourceFile = classNode.sourceFile
    val superClass = reader.getSuperName
    val interfaces: List[String] = reader.getInterfaces.toList
    ClassInfo(classNameFromReader, superClass, sourceFile, interfaces)
  }
}
