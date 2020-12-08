//package topgun.cmdline.ClassLoader;
//
//import jdk.jfr.consumer.RecordedClass;
//
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class SourceFileFinder {
//    Map<String, ClassLoaderInfo> classInfoForAllClassLoaders = new HashMap<>();
//    public static void main(String[] arg){
//        String jarpath = "C:\\Users\\Dufie\\Desktop.gradle-launcher-6.6.1.jar";
//        try {
//            URL u = new URL("jar", "", jarpath + "!/");
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//
//
//    }
//
//    public ClassInfo classInfo(String classPath, RecordedClass clazz){
//        if(!classInfoForAllClassLoaders.containsKey(classPath)){
//            ClassInfo newClassInfo = (new ClassLoaderInfo()).lookup(clazz);
//            classInfoForAllClassLoaders.put(clazz.getName(), newClassInfo);
//        }
//    }
//
//    public void getOrElseUpdate(String path, RecordedClass clazz){
//        if(!classInfoForAllClassLoaders.containsKey(path)){
//            ClassInfo newClassInfo = new ClassLoaderInfo().lookup(clazz);
//            classInfoForAllClassLoaders.put(clazz.getName(), newClassInfo);
//        }
//    }
//}
