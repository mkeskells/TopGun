package topgun.cmdline;

import java.util.*;
import java.util.stream.Collectors;

public class ClassSchema {
    public List<MethodSchema> methodSchemaList = new ArrayList<>();

    public void add(MethodSchema methodSchema) {
        methodSchemaList.add(methodSchema);
    }

    public void addAll(MethodSchema methodSchema) {
        methodSchemaList.add(methodSchema);
    }

    public MethodSchema getMethodByLine(Integer line) {

        var methodList = methodSchemaList.stream().filter(methodSchema -> methodSchema.hasLine(line)).collect(Collectors.toList());
        if (!methodList.isEmpty())
            return methodList.get(0);
        ListIterator<MethodSchema> listIterator = methodSchemaList.listIterator(methodSchemaList.size());
        while (listIterator.hasPrevious()) {
            var methodSchema = listIterator.previous();
            if (methodSchema.firstInstLine < line)
                return methodSchema;
        }
        return null;
    }

}
