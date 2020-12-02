package topgun.cmdline.records;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;

public class AsmRecord {

    public static void main(String[] args) {
        String path = "/home/chempa/Desktop/ASM/example/out/production/example";
        AsmRecord.constructRecord(path);
        peek();
    }
    public static void peek(){
        System.out.println(map);
    }

    private static final HashMap<String, ClassInfo> map = new HashMap<>();
    private static String buildDirectory = "";

    private static String getPackageName(String path){
        String packageName = path.substring(buildDirectory.length()+1).replace('/','.');
        packageName = packageName.substring(0,packageName.length()-6);
        return packageName;
    }

    private static void build(String path) {
        try {
            RandomAccessFile f = new RandomAccessFile(path, "r");
            byte[] b = new byte[(int) f.length()];
            System.out.println(b.length);
            f.readFully(b);
            ClassReader reader = new ClassReader(b);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            String packageName = getPackageName(path);
            ClassInfo classInfo = new ClassInfo(path,classNode,reader);
            map.put(packageName,classInfo);
        } catch (IOException ignore) {}
    }

    public static void constructRecord(String buildDir){
        buildDirectory = buildDir;
        try (Stream<Path> paths = Files.walk(Paths.get(buildDir))) {
            paths.filter(Files::isRegularFile).map(Path::toString).filter(s -> s.endsWith(".class")).forEach(AsmRecord::build);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static ClassInfo getRecord(String key){
        return  map.getOrDefault(key,new ClassInfo(null,null,null));
    }
}
