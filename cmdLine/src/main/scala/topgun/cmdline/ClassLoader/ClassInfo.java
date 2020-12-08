package topgun.cmdline.ClassLoader;

public class ClassInfo {
    private String clazz;
    private String packageName;

    public ClassInfo(String clazz, String packageName) {
        this.clazz = clazz;
        this.packageName = packageName;
    }

    public String getClazz() {
        return clazz;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }


}
