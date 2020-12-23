package topgun.cmdline;

public class MethodSchema {
    public String methodName;
    public String desc;
    public Integer firstInstLine;
    public Integer lastInstLine;

    public MethodSchema(String methodName, String desc, Integer firstInstLine, Integer lastInstLine) {
        this.methodName = methodName;
        this.desc = desc;
        this.firstInstLine = firstInstLine;
        this.lastInstLine = lastInstLine;
    }

    public Boolean hasLine(Integer line) {
        return (line >= firstInstLine) && (line <= lastInstLine);
    }

    @Override
    public String toString() {
        return "MethodSchema{" +
                "methodName='" + methodName + '\'' +
                ", desc='" + desc + '\'' +
                ", firstInstLine=" + firstInstLine +
                ", lastInstLine=" + lastInstLine +
                '}';
    }
}
