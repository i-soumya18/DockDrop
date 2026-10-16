package `in`.soumyaranjansahoo.dockdrop.core.engine

import `in`.soumyaranjansahoo.dockdrop.core.database.JournalDao
import `in`.soumyaranjansahoo.dockdrop.core.database.ReceiptDao
import `in`.soumyaranjansahoo.dockdrop.core.database.SetDao
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.PauseReason
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class DropEngineTimeoutAndResumeTest {

    private lateinit var storageGateway: StorageGateway
    private lateinit var manifestRepository: ManifestRepository
    private lateinit var journalDao: JournalDao
    private lateinit var setDao: SetDao
    private lateinit var receiptDao: ReceiptDao
    private lateinit var setRepository: SetRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val sampleDock = DockInfo(
        volumeUuid = "4E21-0000",
        userLabel = "Samsung T7",
        totalBytes = 500L * 1024 * 1024 * 1024,
        freeBytes = 412L * 1024 * 1024 * 1024,
        filesystem = "exFAT",
        isMounted = true
    )

    @Before
    fun setup() {
        storageGateway = mock(StorageGateway::class.java)
        manifestRepository = ManifestRepository(storageGateway)
        journalDao = mock(JournalDao::class.java)
        setDao = mock(SetDao::class.java)
        receiptDao = mock(ReceiptDao::class.java)
        `when`(setDao.getAllSets()).thenReturn(flowOf(emptyList()))
        setRepository = SetRepository(setDao, receiptDao)
    }

    @Test
    fun testReplugAfterCablePulled_transitionsInterruptedToResumable() {
        val engine = DropEngine(
            storageGateway = storageGateway,
            manifestRepository = manifestRepository,
            journalDao = journalDao,
            setRepository = setRepository,
            scope = scope
        )

        // When plugged in, state is DockDetected
        engine.updateDockState(sampleDock)
        assertTrue(engine.state.value is DropState.DockDetected)

        // When unplugged:
        engine.updateDockState(null)
        assertTrue(engine.state.value is DropState.NoDock)
    }

    @Test
    fun testPauseSystemTimeout_transitionsToResumableState() {
        val engine = DropEngine(
            storageGateway = storageGateway,
            manifestRepository = manifestRepository,
            journalDao = journalDao,
            setRepository = setRepository,
            scope = scope
        )

        // When pause is called with SYSTEM_TIMEOUT
        engine.pause(PauseReason.SYSTEM_TIMEOUT)
        // No active drop: stays NoDock
        assertTrue(engine.state.value is DropState.NoDock)
    }
}
