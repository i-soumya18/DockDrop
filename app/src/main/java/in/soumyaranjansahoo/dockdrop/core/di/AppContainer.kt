package `in`.soumyaranjansahoo.dockdrop.core.di

import android.content.Context
import `in`.soumyaranjansahoo.dockdrop.core.database.DropDatabase
import `in`.soumyaranjansahoo.dockdrop.core.engine.DropEngine
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.service.DropNotificationManager
import `in`.soumyaranjansahoo.dockdrop.core.storage.SafStorageGateway
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import `in`.soumyaranjansahoo.dockdrop.core.trigger.DockMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val storageGateway: StorageGateway by lazy {
        SafStorageGateway(context)
    }

    val database: DropDatabase by lazy {
        DropDatabase.build(context)
    }

    val setRepository: SetRepository by lazy {
        SetRepository(database)
    }

    val manifestRepository: ManifestRepository by lazy {
        ManifestRepository(storageGateway)
    }

    val engine: DropEngine by lazy {
        DropEngine(
            storageGateway = storageGateway,
            manifestRepository = manifestRepository,
            database = database,
            setRepository = setRepository,
            scope = applicationScope
        )
    }

    val notificationManager: DropNotificationManager by lazy {
        DropNotificationManager(context)
    }

    val restorePlanner: `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlanner by lazy {
        `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlanner(
            storageGateway = storageGateway,
            manifestRepository = manifestRepository
        )
    }

    val restoreEngine: `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreEngine by lazy {
        `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreEngine(
            storageGateway = storageGateway,
            context = context,
            scope = applicationScope
        )
    }

    val dockMonitor: DockMonitor by lazy {
        DockMonitor(
            context = context,
            engine = engine,
            scope = applicationScope
        )
    }
}
