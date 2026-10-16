package `in`.soumyaranjansahoo.dockdrop.core.service

import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.PauseReason
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SkippedFile
import `in`.soumyaranjansahoo.dockdrop.core.model.VerificationLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DropNotificationMapperTest {

    private val sampleDock = DockInfo(
        volumeUuid = "4E21-0000",
        userLabel = "Samsung T7",
        totalBytes = 500L * 1024 * 1024 * 1024,
        freeBytes = 412L * 1024 * 1024 * 1024,
        filesystem = "exFAT",
        isMounted = true
    )

    private val sampleSet = SetDefinition(
        id = "set-1",
        name = "My Phone",
        dockVolumeUuid = "4E21-0000",
        dockUserLabel = "Samsung T7",
        destinationVaultUriString = "content://com.android.externalstorage.documents/tree/4E21-0000%3ADockDrop",
        deletionPolicy = DeletionPolicy.KEEP,
        retentionDays = 30,
        isEncrypted = false,
        minBatteryPct = 20,
        verificationLevel = VerificationLevel.FULL_REREAD
    )

    private val sampleProgress = DropProgress(
        setId = "set-1",
        currentFolder = "Camera",
        folderIndex = 1,
        totalFolders = 4,
        copiedFiles = 1204,
        totalFiles = 4812,
        copiedBytes = 6100000000L,
        totalBytes = 14200000000L,
        speedMbS = 35.0,
        filesPerSec = 22.0,
        estMinutesRemaining = 7.0,
        currentFilePath = "DCIM/Camera/IMG_001.jpg"
    )

    @Test
    fun noDock_returnsNull() {
        assertNull(DropNotificationMapper.map(DropState.NoDock))
    }

    @Test
    fun dockDetected_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(DropState.DockDetected(sampleDock), defaultSetName = "My Phone")
        assertNotNull(spec)
        assertEquals("SSD detected · Samsung T7", spec!!.title)
        assertEquals("Tap to back up My Phone", spec.text)
        assertEquals(DropNotificationMapper.CHANNEL_ATTENTION, spec.channelId)
        assertEquals(1, spec.actions.size)
        assertEquals("Back up now", spec.actions.first().label)
        assertEquals(ActionType.BACK_UP_NOW, spec.actions.first().actionType)
    }

    @Test
    fun awaitingConfirm_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(
            DropState.AwaitingConfirm(sampleSet, sampleDock, 1204, 6100000000L, 7.0)
        )
        assertNotNull(spec)
        assertEquals("SSD detected · Samsung T7", spec!!.title)
        assertEquals("Tap to back up My Phone", spec.text)
        assertEquals("Back up now", spec.actions.first().label)
    }

    @Test
    fun preflight_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(DropState.Preflight)
        assertNotNull(spec)
        assertEquals("Checking SSD…", spec!!.title)
        assertEquals("Testing write access", spec.text)
        assertTrue(spec.isOngoing)
        assertTrue(spec.isIndeterminate)
    }

    @Test
    fun dropping_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(DropState.Dropping(sampleProgress))
        assertNotNull(spec)
        val pct = sampleProgress.percentage
        assertEquals("Backing up · $pct%", spec!!.title)
        assertTrue(spec.text.contains("Camera · 1 of 4"))
        assertTrue(spec.isOngoing)
        assertEquals(100, spec.maxProgress)
        assertEquals(pct, spec.progress)
        assertEquals(2, spec.actions.size)
        assertEquals("Pause", spec.actions[0].label)
        assertEquals("Stop", spec.actions[1].label)
    }

    @Test
    fun verifying_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(
            DropState.Verifying(
                currentFolder = "Camera",
                folderIndex = 1,
                totalFolders = 4,
                verifiedFiles = 500,
                totalFiles = 1000
            )
        )
        assertNotNull(spec)
        assertEquals("Verifying · 1 of 4", spec!!.title)
        assertEquals("Almost there", spec.text)
        assertTrue(spec.isOngoing)
        assertEquals("Stop", spec.actions.first().label)
    }

    @Test
    fun sealed_matchesScreensSpec() {
        val receipt = Receipt(
            id = "r1",
            setId = "set-1",
            setName = "My Phone",
            isSealed = true,
            totalFilesVerified = 1248,
            totalBytesVerified = 14200000000L,
            durationSeconds = 680,
            averageSpeedMbS = 35.0,
            verificationLevel = VerificationLevel.FULL_REREAD,
            generationNumber = 1,
            manifestSha256 = "abc",
            keptDeletedFilesCount = 0,
            skippedFiles = emptyList(),
            timestampMs = System.currentTimeMillis()
        )
        val spec = DropNotificationMapper.map(DropState.Sealed(receipt))
        assertNotNull(spec)
        assertEquals("Sealed · 1248 files verified", spec!!.title)
        assertEquals("All writes finished. Safe to unplug.", spec.text)
        assertEquals(DropNotificationMapper.CHANNEL_RESULTS, spec.channelId)
    }

    @Test
    fun partial_matchesScreensSpec() {
        val receipt = Receipt(
            id = "r2",
            setId = "set-1",
            setName = "My Phone",
            isSealed = false,
            totalFilesVerified = 1000,
            totalBytesVerified = 1000000000L,
            durationSeconds = 600,
            averageSpeedMbS = 30.0,
            verificationLevel = VerificationLevel.FULL_REREAD,
            generationNumber = 1,
            manifestSha256 = "def",
            keptDeletedFilesCount = 0,
            skippedFiles = listOf(SkippedFile("a.txt", "Camera", 100L, "Read error")),
            timestampMs = System.currentTimeMillis()
        )
        val spec = DropNotificationMapper.map(
            DropState.Partial(receipt, receipt.skippedFiles)
        )
        assertNotNull(spec)
        assertEquals("Finished with 1 skipped", spec!!.title)
        assertEquals("Tap to review", spec.text)
        assertEquals("Review", spec.actions.first().label)
    }

    @Test
    fun paused_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(
            DropState.Paused(PauseReason.USER_REQUESTED, sampleProgress)
        )
        assertNotNull(spec)
        assertEquals("Paused", spec!!.title)
        assertEquals(PauseReason.USER_REQUESTED.message, spec.text)
        assertEquals(2, spec.actions.size)
        assertEquals("Resume", spec.actions[0].label)
        assertEquals("Stop", spec.actions[1].label)
    }

    @Test
    fun interrupted_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(DropState.Interrupted(sampleProgress))
        assertNotNull(spec)
        assertEquals("Backup interrupted", spec!!.title)
        assertEquals("Plug the SSD back in to resume", spec.text)
    }

    @Test
    fun resumable_matchesScreensSpec() {
        val spec = DropNotificationMapper.map(DropState.Resumable(sampleProgress))
        assertNotNull(spec)
        assertEquals("Resume backup?", spec!!.title)
        assertEquals("${sampleProgress.percentage}% done", spec.text)
        assertEquals("Resume", spec.actions.first().label)
    }

    @Test
    fun attentionVariants_matchesScreensSpec() {
        val noSpaceSpec = DropNotificationMapper.map(
            DropState.Attention(AttentionReason.NoSpace(10_000_000_000L, 2_000_000_000L))
        )
        assertNotNull(noSpaceSpec)
        assertEquals("Backup stopped", noSpaceSpec!!.title)
        assertTrue(noSpaceSpec.text.contains("SSD is full"))

        val permLostSpec = DropNotificationMapper.map(
            DropState.Attention(AttentionReason.PermissionLost("Camera", "content://..."))
        )
        assertNotNull(permLostSpec)
        assertEquals("Needs your attention", permLostSpec!!.title)
        assertEquals("Access to Camera was removed.", permLostSpec.text)
        assertEquals("Fix", permLostSpec.actions.first().label)

        val massDeleteSpec = DropNotificationMapper.map(
            DropState.Attention(AttentionReason.MassDelete(150))
        )
        assertNotNull(massDeleteSpec)
        assertEquals("Review needed", massDeleteSpec!!.title)
        assertEquals("Mirror mode would remove 150 files from the SSD.", massDeleteSpec.text)
        assertEquals("Review", massDeleteSpec.actions.first().label)
    }
}
