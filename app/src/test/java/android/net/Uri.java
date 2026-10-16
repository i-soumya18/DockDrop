package android.net;

import java.io.File;
import java.util.List;

public abstract class Uri implements android.os.Parcelable, Comparable<Uri> {

    public static Uri parse(String uriString) {
        if (uriString == null) return null;
        return new TestUri(uriString);
    }

    public static Uri fromFile(File file) {
        if (file == null) return null;
        return new TestUri("file://" + file.getAbsolutePath());
    }

    public abstract boolean isHierarchical();
    public abstract boolean isRelative();
    public abstract String getScheme();
    public abstract String getSchemeSpecificPart();
    public abstract String getEncodedSchemeSpecificPart();
    public abstract String getAuthority();
    public abstract String getEncodedAuthority();
    public abstract String getUserInfo();
    public abstract String getEncodedUserInfo();
    public abstract String getHost();
    public abstract int getPort();
    public abstract String getPath();
    public abstract String getEncodedPath();
    public abstract String getQuery();
    public abstract String getEncodedQuery();
    public abstract String getFragment();
    public abstract String getEncodedFragment();
    public abstract List<String> getPathSegments();
    public abstract String getLastPathSegment();
    public abstract Builder buildUpon();

    public static class Builder {
        private String uri = "";
        public Builder scheme(String scheme) { return this; }
        public Builder authority(String authority) { return this; }
        public Builder path(String path) { return this; }
        public Builder appendPath(String path) { return this; }
        public Uri build() { return new TestUri(uri); }
    }
}
