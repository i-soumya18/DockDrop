package `in`.soumyaranjansahoo.dockdrop.core.model

enum class DeletionPolicy {
    KEEP,          // Recommended default: files deleted on phone stay on SSD in _deleted/
    KEEP_N_DAYS,   // Files deleted on phone kept for N days
    MIRROR         // Dangerous: files deleted on phone are purged from SSD
}
