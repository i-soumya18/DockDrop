package `in`.soumyaranjansahoo.dockdrop.core.restore

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.util.UUID

class RestorePlanner(
    private val storageGateway: StorageGateway,
    private val manifestRepository: ManifestRepository
) {

    suspend fun createPlan(
        vaultTreeUri: Uri,
        setId: String,
        setName: String,
        deviceLabel: String,
        selectedEntries: List<ManifestEntry>? = null,
        destination: RestoreDestination,
        conflictPolicy: ConflictPolicy = ConflictPolicy.KEEP_BOTH,
        isDrill: Boolean = false
    ): RestorePlan = withContext(Dispatchers.IO) {
        val candidates = when {
            isDrill -> {
                val activeGen = manifestRepository.loadCurrentGeneration(vaultTreeUri, setId)
                activeGen.entries.values.filter { it.status == "present" }.shuffled().take(10)
            }
            selectedEntries != null -> selectedEntries
            else -> {
                val activeGen = manifestRepository.loadCurrentGeneration(vaultTreeUri, setId)
                activeGen.entries.values.filter { it.status == "present" }
            }
        }

        // 1. Detect Permission Gaps
        val permissionGaps = mutableListOf<PermissionGap>()
        val distinctFolders = candidates.map { it.path.substringBefore("/") }.distinct()

        when (destination) {
            is RestoreDestination.OriginalFolders -> {
                for (folderLabel in distinctFolders) {
                    val uriString = destination.folderLabelToUri[folderLabel]
                    val hasPerm = if (uriString != null) {
                        try {
                            storageGateway.hasPersistedPermission(Uri.parse(uriString))
                        } catch (e: Exception) {
                            false
                        }
                    } else false

                    if (!hasPerm) {
                        val affected = candidates.filter { it.path.substringBefore("/") == folderLabel }
                        permissionGaps.add(
                            PermissionGap(
                                folderLabel = folderLabel,
                                previousUriString = uriString,
                                fileCount = affected.size,
                                totalBytes = affected.sumOf { it.sizeBytes }
                            )
                        )
                    }
                }
            }
            is RestoreDestination.ChosenFolder -> {
                val hasPerm = try {
                    storageGateway.hasPersistedPermission(Uri.parse(destination.treeUriString))
                } catch (e: Exception) {
                    false
                }
                if (!hasPerm) {
                    permissionGaps.add(
                        PermissionGap(
                            folderLabel = destination.displayLabel,
                            previousUriString = destination.treeUriString,
                            fileCount = candidates.size,
                            totalBytes = candidates.sumOf { it.sizeBytes }
                        )
                    )
                }
            }
            is RestoreDestination.DrillSandbox -> {
                // Internal cacheDir sandbox requires no SAF permissions
            }
        }

        // 2. Query Existing Files for Conflict Detection
        // Map: folderLabel -> Set of relative paths existing in that folder
        val existingFilesByFolder = mutableMapOf<String, MutableSet<String>>()

        if (destination is RestoreDestination.OriginalFolders) {
            for (folderLabel in distinctFolders) {
                val uriString = destination.folderLabelToUri[folderLabel]
                if (uriString != null && storageGateway.hasPersistedPermission(Uri.parse(uriString))) {
                    val existing = mutableSetOf<String>()
                    try {
                        storageGateway.listSourceFiles(Uri.parse(uriString)).collect { info ->
                            existing.add(info.relativePath)
                        }
                    } catch (e: Exception) {
                        // ignore or proceed with empty
                    }
                    existingFilesByFolder[folderLabel] = existing
                }
            }
        } else if (destination is RestoreDestination.ChosenFolder) {
            val existing = mutableSetOf<String>()
            try {
                if (storageGateway.hasPersistedPermission(Uri.parse(destination.treeUriString))) {
                    storageGateway.listSourceFiles(Uri.parse(destination.treeUriString)).collect { info ->
                        existing.add(info.relativePath)
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            existingFilesByFolder["__chosen__"] = existing
        }

        // 3. Resolve item conflicts and non-colliding paths
        var conflictCount = 0
        val allItems = mutableListOf<RestoreItem>()

        for (entry in candidates) {
            val folderLabel = entry.path.substringBefore("/")
            val relPathInFolder = entry.path.substringAfter("/", missingDelimiterValue = entry.path)

            val vaultRelPath = if (entry.status == "deleted" && !entry.deletedPath.isNullOrEmpty()) {
                entry.deletedPath
            } else {
                "$setName/${entry.path}"
            }

            val existingSet = when (destination) {
                is RestoreDestination.OriginalFolders -> existingFilesByFolder[folderLabel] ?: emptySet()
                is RestoreDestination.ChosenFolder -> existingFilesByFolder["__chosen__"] ?: emptySet()
                is RestoreDestination.DrillSandbox -> emptySet()
            }

            // In ChosenFolder mode, prefix folderLabel to keep folders distinct
            val targetRelCandidate = if (destination is RestoreDestination.ChosenFolder) {
                "${folderLabel}/${relPathInFolder}"
            } else {
                relPathInFolder
            }

            val hasConflict = existingSet.contains(targetRelCandidate)
            if (hasConflict) {
                conflictCount++
            }

            val resolvedTargetRelPath = if (hasConflict && conflictPolicy == ConflictPolicy.KEEP_BOTH) {
                generateNonCollidingPath(targetRelCandidate, existingSet)
            } else {
                targetRelCandidate
            }

            allItems.add(
                RestoreItem(
                    manifestEntry = entry,
                    sourceFolderLabel = folderLabel,
                    relativePathInFolder = relPathInFolder,
                    vaultRelativePath = vaultRelPath,
                    hasConflict = hasConflict,
                    resolvedTargetRelPath = resolvedTargetRelPath
                )
            )
        }

        // 4. Calculate transfer items (excluding skipped conflicts)
        val itemsToTransfer = if (conflictPolicy == ConflictPolicy.SKIP) {
            allItems.filter { !it.hasConflict }
        } else {
            allItems
        }

        val totalBytesToCopy = itemsToTransfer.sumOf { it.manifestEntry.sizeBytes }
        val phoneFree = storageGateway.phoneFreeBytes()
        val hasEnoughSpace = isDrill || (phoneFree > totalBytesToCopy + 64L * 1024 * 1024)

        RestorePlan(
            id = UUID.randomUUID().toString(),
            setId = setId,
            setName = setName,
            deviceLabel = deviceLabel,
            vaultTreeUriString = vaultTreeUri.toString(),
            destination = destination,
            conflictPolicy = conflictPolicy,
            allCandidateItems = allItems,
            itemsToTransfer = itemsToTransfer,
            totalFilesSelected = candidates.size,
            totalFilesToCopy = itemsToTransfer.size,
            totalBytesToCopy = totalBytesToCopy,
            conflictCount = conflictCount,
            permissionGaps = permissionGaps,
            phoneFreeBytes = phoneFree,
            hasEnoughSpace = hasEnoughSpace,
            isDrill = isDrill
        )
    }

    private fun generateNonCollidingPath(originalPath: String, existingSet: Set<String>): String {
        val dir = originalPath.substringBeforeLast("/", missingDelimiterValue = "")
        val fileName = originalPath.substringAfterLast("/")
        val stem = fileName.substringBeforeLast(".", missingDelimiterValue = fileName)
        val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""

        var counter = 1
        var candidateName = "$stem ($counter)$ext"
        var candidateRel = if (dir.isEmpty()) candidateName else "$dir/$candidateName"

        while (existingSet.contains(candidateRel)) {
            counter++
            candidateName = "$stem ($counter)$ext"
            candidateRel = if (dir.isEmpty()) candidateName else "$dir/$candidateName"
        }

        return candidateRel
    }

    fun filterEntries(
        entries: List<ManifestEntry>,
        filter: RestoreFilter,
        query: String = ""
    ): List<ManifestEntry> {
        return entries.filter { entry ->
            val matchesQuery = if (query.isBlank()) true else {
                entry.path.contains(query, ignoreCase = true)
            }

            val ext = entry.path.substringAfterLast(".", "").lowercase()
            val isPhoto = ext in listOf("jpg", "jpeg", "png", "heic", "webp", "dng")
            val isVideo = ext in listOf("mp4", "mov", "mkv", "avi")
            val isDoc = ext in listOf("pdf", "doc", "docx", "txt", "xlsx", "zip")

            val matchesFilter = when (filter) {
                RestoreFilter.ALL -> entry.status == "present"
                RestoreFilter.PHOTOS -> entry.status == "present" && isPhoto
                RestoreFilter.VIDEOS -> entry.status == "present" && isVideo
                RestoreFilter.DOCUMENTS -> entry.status == "present" && isDoc
                RestoreFilter.DELETED -> entry.status == "deleted"
            }

            matchesQuery && matchesFilter
        }
    }
}
