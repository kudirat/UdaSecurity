module Security {
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires java.desktop;
    requires miglayout;
    requires Image;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;

}