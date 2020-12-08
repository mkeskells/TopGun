package topgun.cmdline.records;


//import org.junit.Assert;


import org.junit.Assert;
import org.junit.Test;

public class AsmRecordTest {
    String path = "C:\\Users\\Dufie\\Desktop\\helloword\\out";
    String productionPath = "production\\helloword\\com\\company\\Main";
    AsmRecord record = new AsmRecord(path);

    @Test
    public void testConstructRecord() {
        record.constructRecord(path);
        record.peek();
        ClassInfo defaultObj = new ClassInfo(null, null, null);
//        getting extracted classInfo object
        ClassInfo recorderClass = record.getRecord(productionPath);
//        comparing the returned recordedClass based on map keys.
        Assert.assertNotEquals(recorderClass, defaultObj);
    }
}