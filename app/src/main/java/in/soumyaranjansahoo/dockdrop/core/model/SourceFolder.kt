package `in`.soumyaranjansahoo.dockdrop.core.model

data class SourceFolder(
    val id: String,
    val setId: String,
    val label: String,
    val treeUriString: String,
    val relativeSubpath: String = "",
    val estimatedBytes: Long = 0L,
    val fileCount: Int = 0
)
