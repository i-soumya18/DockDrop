package `in`.soumyaranjansahoo.dockdrop.core

import android.content.SharedPreferences
import `in`.soumyaranjansahoo.dockdrop.core.database.ReceiptDao
import `in`.soumyaranjansahoo.dockdrop.core.database.ReceiptEntity
import `in`.soumyaranjansahoo.dockdrop.core.database.SetDao
import `in`.soumyaranjansahoo.dockdrop.core.database.SetEntity
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.VerificationLevel
import `in`.soumyaranjansahoo.dockdrop.core.preferences.AppPreferences
import `in`.soumyaranjansahoo.dockdrop.core.preferences.ThemeMode
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.support.LogExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class Phase5PlayReadinessAndSettingsTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockSetDao: SetDao
    private lateinit var mockReceiptDao: ReceiptDao
    private lateinit var setRepository: SetRepository

    @Before
    fun setUp() {
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        mockSetDao = mock(SetDao::class.java)
        mockReceiptDao = mock(ReceiptDao::class.java)

        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)

        `when`(mockSetDao.getAllSets()).thenReturn(flowOf(emptyList()))
        `when`(mockReceiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))

        setRepository = SetRepository(mockSetDao, mockReceiptDao)
    }

    @Test
    fun testStoreListingTitleAndShortDescriptionLimits() {
        // Google Play Title constraint: max 30 chars
        val storeTitle = "DockDrop: SSD Backup & Restore"
        assertEquals("Store title must be exactly 30 characters for maximum clarity without truncation", 30, storeTitle.length)

        // Google Play Short Description constraint: max 80 chars
        val shortDesc = "Verified offline phone backup to your USB-C SSD. Plain folders, zero cloud."
        assertTrue("Short description must fit within 80 characters (was ${shortDesc.length})", shortDesc.length <= 80)
        assertTrue("Short description length is 75 chars", shortDesc.length == 75)
    }

    @Test
    fun testAppPreferencesFlowsAndPersistence() {
        `when`(mockPrefs.getBoolean(eq("has_completed_onboarding"), anyBoolean())).thenReturn(false)
        `when`(mockPrefs.getString(eq("theme_mode"), anyString())).thenReturn(ThemeMode.DARK.name)
        `when`(mockPrefs.getBoolean(eq("reduce_motion"), anyBoolean())).thenReturn(false)
        `when`(mockPrefs.getInt(eq("default_min_battery"), anyInt())).thenReturn(20)

        val appPrefs = AppPreferences(mockPrefs)

        assertFalse(appPrefs.hasCompletedOnboarding.value)
        assertEquals(ThemeMode.DARK, appPrefs.themeMode.value)
        assertFalse(appPrefs.reduceMotion.value)
        assertEquals(20, appPrefs.defaultMinBatteryPct.value)

        // Mutate onboarding
        appPrefs.setCompletedOnboarding(true)
        verify(mockEditor).putBoolean("has_completed_onboarding", true)
        assertTrue(appPrefs.hasCompletedOnboarding.value)

        // Mutate theme
        appPrefs.setThemeMode(ThemeMode.LIGHT)
        verify(mockEditor).putString("theme_mode", ThemeMode.LIGHT.name)
        assertEquals(ThemeMode.LIGHT, appPrefs.themeMode.value)

        // Mutate reduce motion
        appPrefs.setReduceMotion(true)
        verify(mockEditor).putBoolean("reduce_motion", true)
        assertTrue(appPrefs.reduceMotion.value)

        // Mutate default battery
        appPrefs.setDefaultMinBatteryPct(30)
        verify(mockEditor).putInt("default_min_battery", 30)
        assertEquals(30, appPrefs.defaultMinBatteryPct.value)
    }

    @Test
    fun testLogExporterSanitizationAndNoSecretLeakage() {
        val sampleSet = SetDefinition(
            id = "set-uuid-1",
            name = "Test Photos",
            dockVolumeUuid = "4E21-0000",
            dockUserLabel = "Samsung T7",
            destinationVaultUriString = "content://vault",
            deletionPolicy = DeletionPolicy.KEEP,
            retentionDays = 30,
            minBatteryPct = 20,
            isEncrypted = true,
            keystoreWrappedMkHex = "aabbccddeeff0011223344", // Should NOT leak in diagnostic log!
            verificationLevel = VerificationLevel.FULL_REREAD,
            createdAtMs = 1700000000000L,
            lastSealedMs = 1700003600000L,
            lastSealedFiles = 420,
            lastSealedBytes = 2500000000L,
            lastStatus = "Sealed"
        )

        val sampleReceipt = Receipt(
            id = "rcpt-1",
            setId = "set-uuid-1",
            setName = "Test Photos",
            isSealed = true,
            totalFilesVerified = 420,
            totalBytesVerified = 2500000000L,
            durationSeconds = 64,
            averageSpeedMbS = 39.1,
            verificationLevel = VerificationLevel.FULL_REREAD,
            generationNumber = 1L,
            manifestSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            keptDeletedFilesCount = 0,
            skippedFiles = emptyList(),
            timestampMs = 1700003600000L
        )

        val log = LogExporter.generateDiagnosticLog(
            packageName = "in.soumyaranjansahoo.dockdrop",
            persistedPermissions = emptyList(),
            sets = listOf(sampleSet),
            recentReceipts = listOf(sampleReceipt),
            deviceInfo = "Samsung Galaxy A14 5G (SM-A146B)",
            osInfo = "14 (SDK 34, Build AP3A.240905.015)"
        )

        // Assert log header and offline declaration
        assertTrue(log.contains("DOCKDROP DIAGNOSTIC LOG"))
        assertTrue(log.contains("Network Status: Strictly Offline (zero network permissions declared)"))
        assertTrue(log.contains("Set #1: Test Photos"))
        assertTrue(log.contains("Samsung T7"))
        assertTrue(log.contains("Receipt #1: Set 'Test Photos' · SEALED [100% Verified]"))

        // Assert NO crypto secrets or master keys leak
        assertFalse("Log must not leak keystore wrapped master key", log.contains("aabbccddeeff0011223344"))
        assertFalse("Log must not contain passphrase references", log.lowercase().contains("passphrase:"))
    }

    @Test
    fun testSetRepositoryHistoryAndWipeSets() {
        runBlocking {
        val sampleEntity = ReceiptEntity(
            id = "r-1",
            setId = "s-1",
            setName = "Set 1",
            isSealed = true,
            totalFilesVerified = 10,
            totalBytesVerified = 1024L,
            durationSeconds = 2L,
            averageSpeedMbS = 0.5,
            verificationLevel = "FULL_REREAD",
            generationNumber = 1L,
            manifestSha256 = "hash1",
            keptDeletedFilesCount = 0,
            skippedFilesJson = "[]",
            timestampMs = 1000L
        )

        `when`(mockReceiptDao.getAllReceipts()).thenReturn(flowOf(listOf(sampleEntity)))
        `when`(mockReceiptDao.getRecentReceiptsSync(10)).thenReturn(listOf(sampleEntity))

        val allReceipts = setRepository.allReceipts.first()
        assertEquals(1, allReceipts.size)
        assertEquals("Set 1", allReceipts[0].setName)
        assertEquals("hash1", allReceipts[0].manifestSha256)

        val recent = setRepository.getRecentReceiptsSync(10)
        assertEquals(1, recent.size)

        // Wipe sets
        setRepository.deleteAllSets()
        verify(mockSetDao).deleteAllSets()
        verify(mockSetDao).deleteAllSourceFolders()
        }
    }
}
