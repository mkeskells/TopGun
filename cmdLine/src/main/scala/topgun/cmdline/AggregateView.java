package topgun.cmdline;

import topgun.core.CallSite;

public enum AggregateView {
    PACKAGE_CLASSNAME_METHOD_LINE_VIEW,
    PACKAGE_CLASSNAME_METHOD_VIEW,
    PACKAGE_CLASSNAME_VIEW,
    PACKAGE_VIEW,
    PACKAGE_SOURCE_METHOD_VIEW,
    PACKAGE_SOURCE_LINE_VIEW;

    public CallSite convertToView(CallSite site) throws Exception {
        if (site.view() != PACKAGE_CLASSNAME_METHOD_LINE_VIEW) {
            throw new Exception(String.format("Cannot aggregate from %s }", site.view().toString()));
        }
        int maskLineValue = -2;
        switch (this) {
            case PACKAGE_CLASSNAME_METHOD_VIEW:
                return CallSite.apply(site.packageName(), site.className(), site.methodName(), "", maskLineValue, this, site.fileName());
            case PACKAGE_CLASSNAME_VIEW:
                return CallSite.apply(site.packageName(), site.className(), "", "", maskLineValue, this, site.fileName());
            case PACKAGE_VIEW:
                return CallSite.apply(site.packageName(), "", "", "", maskLineValue, this, "");
            case PACKAGE_SOURCE_METHOD_VIEW:
                return CallSite.apply(site.packageName(), "", site.methodName(), "", maskLineValue, this, site.fileName());
            case PACKAGE_SOURCE_LINE_VIEW:
                return CallSite.apply(site.packageName(), "", "", "", site.line(), this, site.fileName());
            case PACKAGE_CLASSNAME_METHOD_LINE_VIEW:
                return site;
        }
        throw new Exception("Failed to convert view");
    }
}
