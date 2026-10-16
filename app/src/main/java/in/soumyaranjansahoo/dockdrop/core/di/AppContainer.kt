package `in`.soumyaranjansahoo.dockdrop.core.di

import android.content.Context
import `in`.soumyaranjansahoo.dockdrop.core.database.DropDatabase
import `in`.soumyaranjansahoo.dockdrop.core.engine.DropEngine
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.storage.SafStorageGateway
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
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
}
