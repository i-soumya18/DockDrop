package android.net

import android.os.Parcel

class TestUri(private val uriString: String = "content://fake/4E21-0000:DockDrop") : Uri() {
    override fun isHierarchical(): Boolean = true
    override fun isRelative(): Boolean = false
    override fun getScheme(): String = "content"
    override fun getSchemeSpecificPart(): String = uriString
    override fun getEncodedSchemeSpecificPart(): String = uriString
    override fun getAuthority(): String = "fake"
    override fun getEncodedAuthority(): String = "fake"
    override fun getUserInfo(): String? = null
    override fun getEncodedUserInfo(): String? = null
    override fun getHost(): String? = null
    override fun getPort(): Int = -1
    override fun getPath(): String = ""
    override fun getEncodedPath(): String = ""
    override fun getQuery(): String? = null
    override fun getEncodedQuery(): String? = null
    override fun getFragment(): String? = null
    override fun getEncodedFragment(): String? = null
    override fun getPathSegments(): List<String> = emptyList()
    override fun getLastPathSegment(): String = "DockDrop"
    override fun buildUpon(): Builder = throw UnsupportedOperationException()
    override fun toString(): String = uriString
    override fun compareTo(other: Uri?): Int = uriString.compareTo(other?.toString() ?: "")
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}
}
