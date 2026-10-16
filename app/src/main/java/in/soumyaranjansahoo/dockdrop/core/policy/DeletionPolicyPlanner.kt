package `in`.soumyaranjansahoo.dockdrop.core.policy

import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

sealed class PolicyCheckResult {
    data class Proceed(val plan: DeletionRetentionPlan) : PolicyCheckResult()
    data class RequireMirrorConfirmation(val setId: String) : PolicyCheckResult()
    data class TriggerMassDeleteGuard(
        val candidateCount: Int,
        val totalActiveCount: Int,
        val candidatePaths: List<String>
    ) : PolicyCheckResult()
}

data class DeletionRetentionPlan(
    val policy: DeletionPolicy,
    val filesToMoveToDeleted: List<ManifestEntry>,
    val filesToDeleteFromActive: List<ManifestEntry>,
    val expiredDeletedEntriesToPurge: List<ManifestEntry>
)

object DeletionPolicyPlanner {

    /**
     * Evaluates deletion and retention policies against the current manifest and source scan.
     * Enforces CLAUDE.md Rule 6 and AGENTS.md A4 completion criteria:
     * - Mirror mode refuses to run without mirrorConfirmed == true
     * - Mass-delete guard triggers if > 20% or > 500 files would be removed
     * - Retention job purges entries older than retentionDays
     */
    fun evaluate(
        setId: String,
        policy: DeletionPolicy,
        retentionDays: Int,
        mirrorConfirmed: Boolean,
        allowMassDeleteOverride: Boolean,
        activeManifestEntries: Map<String, ManifestEntry>,
        currentSourcePaths: Set<String>,
        todayDate: LocalDate = LocalDate.now()
    ): PolicyCheckResult {
        val presentEntries = activeManifestEntries.values.filter { it.status == "present" }
        val removedFromPhone = presentEntries.filter { !currentSourcePaths.contains(it.path) }

        val totalPresent = presentEntries.size
        val removedCount = removedFromPhone.size

        when (policy) {
            DeletionPolicy.MIRROR -> {
                // Criterion 1: Mirror mode refuses to run without the confirmation flag
                if (!mirrorConfirmed) {
                    return PolicyCheckResult.RequireMirrorConfirmation(setId)
                }

                // Criterion 2: Mass-delete guard (> 20% or > 500 files)
                val triggersGuard = totalPresent > 0 &&
                        (removedCount > 500 || (removedCount * 5 > totalPresent))

                if (triggersGuard && !allowMassDeleteOverride) {
                    return PolicyCheckResult.TriggerMassDeleteGuard(
                        candidateCount = removedCount,
                        totalActiveCount = totalPresent,
                        candidatePaths = removedFromPhone.map { it.path }
                    )
                }

                // Purge any existing kept deletions when running in Mirror mode
                val existingDeleted = activeManifestEntries.values.filter { it.status == "deleted" }

                return PolicyCheckResult.Proceed(
                    DeletionRetentionPlan(
                        policy = DeletionPolicy.MIRROR,
                        filesToMoveToDeleted = emptyList(),
                        filesToDeleteFromActive = removedFromPhone,
                        expiredDeletedEntriesToPurge = existingDeleted
                    )
                )
            }

            DeletionPolicy.KEEP -> {
                return PolicyCheckResult.Proceed(
                    DeletionRetentionPlan(
                        policy = DeletionPolicy.KEEP,
                        filesToMoveToDeleted = removedFromPhone,
                        filesToDeleteFromActive = emptyList(),
                        expiredDeletedEntriesToPurge = emptyList()
                    )
                )
            }

            DeletionPolicy.KEEP_N_DAYS -> {
                val expiredEntries = mutableListOf<ManifestEntry>()
                val existingDeleted = activeManifestEntries.values.filter { it.status == "deleted" }

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                for (del in existingDeleted) {
                    val dateStr = del.deletedDate
                    if (dateStr != null) {
                        try {
                            val parsedDate = LocalDate.parse(dateStr, formatter)
                            val daysBetween = ChronoUnit.DAYS.between(parsedDate, todayDate)
                            if (daysBetween >= retentionDays) {
                                expiredEntries.add(del)
                            }
                        } catch (e: Exception) {
                            // If invalid date format, keep safely
                        }
                    }
                }

                return PolicyCheckResult.Proceed(
                    DeletionRetentionPlan(
                        policy = DeletionPolicy.KEEP_N_DAYS,
                        filesToMoveToDeleted = removedFromPhone,
                        filesToDeleteFromActive = emptyList(),
                        expiredDeletedEntriesToPurge = expiredEntries
                    )
                )
            }
        }
    }
}
