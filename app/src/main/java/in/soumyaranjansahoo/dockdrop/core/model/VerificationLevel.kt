package `in`.soumyaranjansahoo.dockdrop.core.model

enum class VerificationLevel(val displayName: String) {
    FULL_REREAD("size + SHA-256 on copy + re-read"),
    ON_COPY_ONLY("size + SHA-256 on copy"),
    SIZE_ONLY("size only");

    override fun toString(): String = displayName
}
