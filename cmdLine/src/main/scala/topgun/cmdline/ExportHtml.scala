package topgun.cmdline

import java.io.{File, FileInputStream, IOException}
import java.nio.file.Paths
import java.util.zip.ZipInputStream

import scala.util.control.Breaks.break

object ExportHtml {
  def exportHtml(outDir: File): Unit = {
    val res = getClass.getClassLoader.getResource("html_report.zip")

    val file = Paths.get(res.toURI).toFile
    val zis = new ZipInputStream(new FileInputStream(file))
    val normalizeTarget = Paths.get(outDir.getPath + "/html_report").normalize()
    var zipEntry = zis.getNextEntry
    while (zipEntry != null) {
      val path = normalizeTarget.resolve(zipEntry.getName).normalize()
      if (path.startsWith(normalizeTarget)) {
        if (zipEntry.isDirectory) {
          java.nio.file.Files.createDirectories(path)
        } else {
          val os = java.nio.file.Files.newOutputStream(path)
          var len = 0
          val buffer = new Array[Byte](1024)
          len = zis.read(buffer)
          while (len > 0) {
            os.write(buffer, 0, len)
            len = zis.read(buffer)
          }
        }
        zipEntry = zis.getNextEntry;
      } else {
        throw new IOException("")
      }
    }
    zis.close()
  }
}
