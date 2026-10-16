package `in`.soumyaranjansahoo.dockdrop.core.repository

import `in`.soumyaranjansahoo.dockdrop.core.database.DropDatabase
import `in`.soumyaranjansahoo.dockdrop.core.database.ReceiptEntity
import `in`.soumyaranjansahoo.dockdrop.core.database.SetEntity
import `in`.soumyaranjansahoo.dockdrop.core.database.SourceFolderEntity
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.database.ReceiptDao
import `in`.soumyaranjansahoo.dockdrop.core.database.SetDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class SetRepository(
    private val setDao: SetDao,
    private val receiptDao: ReceiptDao
) {
    constructor(database: DropDatabase) : this(database.setDao(), database.receiptDao())

    open val allSets: Flow<List<SetDefinition>> = setDao.getAllSets().map { list ->
        list.map { it.toDomain() }
    }

    fun getSourceFolders(setId: String): Flow<List<SourceFolder>> =
        setDao.getSourceFoldersForSet(setId).map { list -> list.map { it.toDomain() } }

    suspend fun getSourceFoldersSync(setId: String): List<SourceFolder> =
        setDao.getSourceFoldersSync(setId).map { it.toDomain() }

    suspend fun getSet(id: String): SetDefinition? =
        setDao.getSetById(id)?.toDomain()

    suspend fun saveSet(set: SetDefinition, folders: List<SourceFolder>) {
        setDao.insertSet(SetEntity.fromDomain(set))
        setDao.deleteSourceFoldersForSet(set.id)
        setDao.insertSourceFolders(folders.map { SourceFolderEntity.fromDomain(it) })
    }

    suspend fun updateSetLastStatus(
        setId: String,
        status: String,
        sealedMs: Long?,
        files: Int,
        bytes: Long
    ) {
        val existing = setDao.getSetById(setId) ?: return
        val updated = existing.copy(
            lastStatus = status,
            lastSealedMs = sealedMs ?: existing.lastSealedMs,
            lastSealedFiles = files,
            lastSealedBytes = bytes
        )
        setDao.updateSet(updated)
    }

    suspend fun deleteSet(id: String) {
        setDao.deleteSet(id)
        setDao.deleteSourceFoldersForSet(id)
    }

    suspend fun saveReceipt(receipt: Receipt) {
        receiptDao.insertReceipt(ReceiptEntity.fromDomain(receipt))
    }

    fun getReceipts(setId: String): Flow<List<Receipt>> =
        receiptDao.getReceiptsForSet(setId).map { list -> list.map { it.toDomain() } }

    suspend fun getLatestReceipt(setId: String): Receipt? =
        receiptDao.getLatestReceipt(setId)?.toDomain()
}
