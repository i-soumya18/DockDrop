package `in`.soumyaranjansahoo.dockdrop.core.policy

import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PolicyAndRetentionTest {

    private fun makeEntry(
        path: String,
        status: String = "present",
        deletedDate: String? = null,
        deletedPath: String? = null
    ): ManifestEntry = ManifestEntry(
        path = path,
        sizeBytes = 1024L,
        modifiedMs = 1700000000000L,
        sha256 = "dummy_hash",
        status = status,
        generation = 1L,
        deletedDate = deletedDate,
        deletedPath = deletedPath
    )

    @Test
    fun testKeepPolicy_movesFilesToDeletedDir_neverPrunes() {
        val entries = mapOf(
            "Photos/pic1.jpg" to makeEntry("Photos/pic1.jpg"),
            "Photos/pic2.jpg" to makeEntry("Photos/pic2.jpg"),
            "Docs/notes.txt" to makeEntry("Docs/notes.txt")
        )
        // Only pic2.jpg still on phone -> pic1.jpg and notes.txt removed
        val currentSourcePaths = setOf("Photos/pic2.jpg")

        val result = DeletionPolicyPlanner.evaluate(
            setId = "PersonalSet",
            policy = DeletionPolicy.KEEP,
            retentionDays = 30,
            mirrorConfirmed = false,
            allowMassDeleteOverride = false,
            activeManifestEntries = entries,
            currentSourcePaths = currentSourcePaths
        )

        assertTrue(result is PolicyCheckResult.Proceed)
        val plan = (result as PolicyCheckResult.Proceed).plan
        assertEquals(2, plan.filesToMoveToDeleted.size)
        assertTrue(plan.filesToDeleteFromActive.isEmpty())
        assertTrue(plan.expiredDeletedEntriesToPurge.isEmpty())
    }

    @Test
    fun testKeepNDaysPolicy_preservesRecent_prunesExpired() {
        val today = LocalDate.of(2026, 10, 16)
        val entries = mapOf(
            "Photos/active.jpg" to makeEntry("Photos/active.jpg"),
            "Photos/del_recent.jpg" to makeEntry("Photos/del_recent.jpg", status = "deleted", deletedDate = "2026-10-09"), // 7 days old
            "Photos/del_expired.jpg" to makeEntry("Photos/del_expired.jpg", status = "deleted", deletedDate = "2026-08-17")  // 60 days old
        )
        val currentSourcePaths = setOf("Photos/active.jpg")

        val result = DeletionPolicyPlanner.evaluate(
            setId = "PersonalSet",
            policy = DeletionPolicy.KEEP_N_DAYS,
            retentionDays = 30,
            mirrorConfirmed = false,
            allowMassDeleteOverride = false,
            activeManifestEntries = entries,
            currentSourcePaths = currentSourcePaths,
            todayDate = today
        )

        assertTrue(result is PolicyCheckResult.Proceed)
        val plan = (result as PolicyCheckResult.Proceed).plan
        assertEquals(1, plan.expiredDeletedEntriesToPurge.size)
        assertEquals("Photos/del_expired.jpg", plan.expiredDeletedEntriesToPurge[0].path)
    }

    @Test
    fun testMirrorPolicy_unconfirmed_refusesToRun() {
        val entries = mapOf(
            "Photos/pic1.jpg" to makeEntry("Photos/pic1.jpg"),
            "Photos/pic2.jpg" to makeEntry("Photos/pic2.jpg")
        )
        val currentSourcePaths = setOf("Photos/pic2.jpg")

        val result = DeletionPolicyPlanner.evaluate(
            setId = "PersonalSet",
            policy = DeletionPolicy.MIRROR,
            retentionDays = 30,
            mirrorConfirmed = false, // Not confirmed!
            allowMassDeleteOverride = false,
            activeManifestEntries = entries,
            currentSourcePaths = currentSourcePaths
        )

        assertTrue("Mirror mode must refuse to run without explicit confirmation flag",
            result is PolicyCheckResult.RequireMirrorConfirmation)
    }

    @Test
    fun testMirrorPolicy_confirmed_underThreshold_proceedsWithDeletion() {
        // 5 files out of 100 removed (5% <= 20%, 5 <= 500)
        val entries = (1..100).associate { "file_$it.txt" to makeEntry("file_$it.txt") }
        val currentSourcePaths = (6..100).map { "file_$it.txt" }.toSet()

        val result = DeletionPolicyPlanner.evaluate(
            setId = "PersonalSet",
            policy = DeletionPolicy.MIRROR,
            retentionDays = 30,
            mirrorConfirmed = true,
            allowMassDeleteOverride = false,
            activeManifestEntries = entries,
            currentSourcePaths = currentSourcePaths
        )

        assertTrue(result is PolicyCheckResult.Proceed)
        val plan = (result as PolicyCheckResult.Proceed).plan
        assertEquals(5, plan.filesToDeleteFromActive.size)
        assertTrue(plan.filesToMoveToDeleted.isEmpty())
    }

    @Test
    fun testMirrorPolicy_triggersGuard_whenPercentageExceeds20Percent() {
        // 21 files out of 100 removed (21% > 20%)
        val entries = (1..100).associate { "file_$it.txt" to makeEntry("file_$it.txt") }
        val currentSourcePaths = (22..100).map { "file_$it.txt" }.toSet()

        val result = DeletionPolicyPlanner.evaluate(
            setId = "PersonalSet",
            policy = DeletionPolicy.MIRROR,
            retentionDays = 30,
            mirrorConfirmed = true,
            allowMassDeleteOverride = false,
            activeManifestEntries = entries,
            currentSourcePaths = currentSourcePaths
        )

        assertTrue("Must trigger mass-delete guard when >20% files removed",
            result is PolicyCheckResult.TriggerMassDeleteGuard)
        val guard = result as PolicyCheckResult.TriggerMassDeleteGuard
        assertEquals(21, guard.candidateCount)
        assertEquals(21, guard.candidatePaths.size)
    }

    @Test
    fun testMirrorPolicy_triggersGuard_whenCountExceeds500Files() {
        // 501 files out of 5000 removed (10.02% <= 20%, but 501 > 500)
        val entries = (1..5000).associate { "file_$it.txt" to makeEntry("file_$it.txt") }
        val currentSourcePaths = (502..5000).map { "file_$it.txt" }.toSet()

        val result = DeletionPolicyPlanner.evaluate(
            setId = "PersonalSet",
            policy = DeletionPolicy.MIRROR,
            retentionDays = 30,
            mirrorConfirmed = true,
            allowMassDeleteOverride = false,
            activeManifestEntries = entries,
            currentSourcePaths = currentSourcePaths
        )

        assertTrue("Must trigger mass-delete guard when >500 files removed even if <=20%",
            result is PolicyCheckResult.TriggerMassDeleteGuard)
        val guard = result as PolicyCheckResult.TriggerMassDeleteGuard
        assertEquals(501, guard.candidateCount)
    }
}
