package `in`.soumyaranjansahoo.dockdrop.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SetEntity::class,
        SourceFolderEntity::class,
        JournalEntry::class,
        ReceiptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DropDatabase : RoomDatabase() {
    abstract fun setDao(): SetDao
    abstract fun journalDao(): JournalDao
    abstract fun receiptDao(): ReceiptDao

    companion object {
        fun build(context: Context): DropDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                DropDatabase::class.java,
                "dockdrop.db"
            ).fallbackToDestructiveMigration().build()
        }
    }
}
