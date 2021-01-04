package topgun.cmdline

import java.io.{BufferedReader, File, FileReader, IOException}
import java.util

import scala.collection.mutable

/**
 * The CSV files are written in an unstructured manner. The callsites are written to random files based on what type of callsite it is.
 * The JSON OBJECT contains all the callsites and it needs to be written in a structure manner so it makes much sense to convert the individual CSV files to JSON.
 */
object CsvToJsonWriter {
  def writeToJson(outDir: File): Unit = {
    val jsonFile = new File(s"${outDir.getAbsoluteFile}/html_report/assets/js/data.js")
    val fileWriter = new java.io.FileWriter(jsonFile)
    val headermap = mutable.HashMap.empty[String, String]
    var headers = ""

    def addHeader(header: String, headerDesc: String): String = {
      if (headerDesc.equals("allocation")) {
        headers = s"""$headers\"allocation\":${header}"""
      } else if (headerDesc.eq("cpu")) {
        headers = s"""$headers\"cpu\":${header}"""
      } else {
        headers = s"""$headers\"allocByClass\":${header}"""
      }
      header
    }

    fileWriter.write("var TOPGUN_DATA={")
    outDir.listFiles().toList.filter {
      _.getName.endsWith(".csv")
    }.foreach(file => {
      val key = file.getName
      val data = convertCsv(file)
      val headerDesc = if (file.getName.contains("-allocation-")) "allocation" else if (file.getName.contains("alloc-by-class")) "allocByClass" else "cpu"
      headermap.getOrElseUpdate(data.get(0), addHeader(data.get(0), headerDesc))
      val out = s"""\"$key\":${data.get(1)},"""
      fileWriter.write(out)
    })
    fileWriter.write("}")
    fileWriter.write(s"\nvar TOPGUN_HEADERS = {$headers}\n")
    fileWriter.flush()
    fileWriter.close()
  }

  /*
  * converts a csv file to a json string
  * @param file: csv file to be converted
  * @return: returns an list of String, size of list = 2
  *          index 0 of list = header of the csv file in a json array string
  *          index 1 of list = content of the csv file in a json array string
  */
  @throws[IOException]
  def convertCsv(file: File): util.List[String] = {
    val reader = new BufferedReader(new FileReader(file))
    var header: String = null
    var line = reader.readLine
    var jsonArrayString = ""
    while ( {
      line != null
    }) {
      jsonArrayString = jsonArrayString.concat(String.format("[\"%s\"],", line.replaceAll(",", "\",\"")))
      if (header == null) {
        header = jsonArrayString
        jsonArrayString = "["
      }
      line = reader.readLine
    }
    val list = new util.ArrayList[String]
    list.add(header)
    list.add(jsonArrayString + "]")
    list
  }
}
