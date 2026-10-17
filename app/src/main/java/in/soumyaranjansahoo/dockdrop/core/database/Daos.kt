package `in`.soumyaranjansahoo.dockdrop.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Query("SELECT * FROM sets ORDER BY createdAtMs DESC")
    fun getAllSets(): Flow<List<SetEntity>>

    @Query("SELECT * FROM sets WHERE id = :id LIMIT 1")
    suspend fun getSetById(id: String): SetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntity)

    @Update
    suspend fun updateSet(set: SetEntity)

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: String): Int

    @Query("SELECT * FROM source_folders WHERE setId = :setId")
    fun getSourceFoldersForSet(setId: String): Flow<List<SourceFolderEntity>>

    @Query("SELECT * FROM source_folders WHERE setId = :setId")
    suspend fun getSourceFoldersSync(setId: String): List<SourceFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceFolders(folders: List<SourceFolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceFolder(folder: SourceFolderEntity)

    @Query("DELETE FROM source_folders WHERE id = :folderId")
    suspend fun deleteSourceFolder(folderId: String): Int

    @Query("DELETE FROM source_folders WHERE setId = :setId")
    suspend fun deleteSourceFoldersForSet(setId: String): Int

    @Query("DELETE FROM sets")
    suspend fun deleteAllSets(): Int

    @Query("DELETE FROM source_folders")
    suspend fun deleteAllSourceFolders(): Int
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM drop_journal WHERE setId = :setId")
    suspend fun getJournalForSet(setId: String): List<JournalEntry>

    @Query("SELECT * FROM drop_journal WHERE setId = :setId AND status IN ('PLANNED', 'COPYING', 'COPIED')")
    suspend fun getPendingEntries(setId: String): List<JournalEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(entries: List<JournalEntry>)

    @Query("UPDATE drop_journal SET status = :status, errorReason = :errorReason, expectedSha256 = COALESCE(:expectedSha, expectedSha256), destinationSha256 = COALESCE(:destSha, destinationSha256), updatedAtMs = :updatedAt WHERE setId = :setId AND relativePath = :relativePath")
    suspend fun updateStatusRaw(
        setId: String,
        relativePath: String,
        status: String,
        errorReason: String?,
        expectedSha: String?,
        destSha: String?,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM drop_journal WHERE setId = :setId")
    suspend fun clearJournalForSet(setId: String): Int
}

suspend fun JournalDao.updateStatus(
    setId: String,
    relativePath: String,
    status: String,
    errorReason: String? = null,
    expectedSha: String? = null,
    destSha: String? = null,
    updatedAt: Long = System.currentTimeMillis()
): Int {
    return updateStatusRaw(setId, relativePath, status, errorReason, expectedSha, destSha, updatedAt)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts WHERE setId = :setId ORDER BY timestampMs DESC")
    fun getReceiptsForSet(setId: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts ORDER BY timestampMs DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentReceiptsSync(limit: Int): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE setId = :setId ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLatestReceipt(setId: String): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun getReceiptById(id: String): ReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)
}
