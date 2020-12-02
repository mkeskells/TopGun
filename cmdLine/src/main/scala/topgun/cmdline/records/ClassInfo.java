package topgun.cmdline.records;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassInfo {
    private final String path;
    private final ClassNode classNode;
    private final ClassReader classReader;

    public ClassInfo(String path, ClassNode classNode, ClassReader classReader) {
        this.path = path;
        this.classNode = classNode;
        this.classReader = classReader;
    }

    public String getClassName(){
        return this.classReader.getClassName().replace('/','.');
    }

    public String getSuperName(){
        return this.classReader.getSuperName().replace('/','.');
    }

    public List<String> getInterfaces(){
        return Arrays.stream(this.classReader.getInterfaces()).map(s -> s.replace('/','.')).collect(Collectors.toList());
    }

    public String getPath(){
        return this.path;
    }

    public String getSourceFileName(){
        return this.classNode.sourceFile;
    }

    public boolean isValid(){
        return this.path != null;
    }

    @Override
    public String toString() {
        return "ClassInfo{" +
                "sourceFileName=" + this.getSourceFileName() +
                ", path='" + path + '\'' +
                ", className=" + this.getClassName() +
                ", superName=" + this.getSuperName() +
                ", interfaces=" + this.getInterfaces() +
                '}';
    }
}
