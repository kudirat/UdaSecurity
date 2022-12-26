module Security {
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires java.desktop;
    requires miglayout;
    requires Image;
    opens com.udacity.catpoint.security.data to com.google.gson;
}