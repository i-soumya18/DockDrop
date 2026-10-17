package `in`.soumyaranjansahoo.dockdrop.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import `in`.soumyaranjansahoo.dockdrop.core.di.AppContainer
import `in`.soumyaranjansahoo.dockdrop.core.manifest.DiscoveredBackupSet
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.ui.screens.attention.AttentionSheet
import `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto.UnlockSheet
import `in`.soumyaranjansahoo.dockdrop.ui.screens.detail.SetDetailScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.drop.DropScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.home.HomeScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.onboarding.KeepAliveScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.plugin.PlugInSheet
import `in`.soumyaranjansahoo.dockdrop.ui.screens.receipt.ReceiptScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.restore.NewPhoneRestoreScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.restore.RestoreBrowserScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.restore.RestoreHubScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.restore.RestorePreviewScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.restore.RestoreProgressScreen
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.ui.screens.history.HistoryScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.onboarding.WelcomeScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.settings.SettingsScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.wizard.SetWizardScreen
import kotlinx.coroutines.launch

object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val WIZARD = "wizard"
    const val DROP = "drop"
    const val RECEIPT = "receipt"
    const val KEEP_ALIVE = "keep_alive"
    const val SET_DETAIL = "set_detail"
    const val RESTORE_HUB = "restore_hub"
    const val RESTORE_BROWSER = "restore_browser"
    const val RESTORE_PREVIEW = "restore_preview"
    const val RESTORE_PROGRESS = "restore_progress"
    const val NEW_PHONE_RESTORE = "new_phone_restore"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun AppNavGraph(
    container: AppContainer,
    currentDock: DockInfo?,
    isUsbConnecting: Boolean = false,
    showPlugInSheet: Boolean = false,
    onDismissPlugInSheet: () -> Unit = {},
    onStartDropInService: (String) -> Unit = {},
    onResumeDropInService: (String) -> Unit = {},
    onPauseDropInService: () -> Unit = {},
    onStopDropInService: () -> Unit = {},
    onStartRestoreInService: (RestorePlan) -> Unit = {},
    onStopRestoreInService: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val engine = container.engine
    val setRepository = container.setRepository
    val storageGateway = container.storageGateway
    val restorePlanner = container.restorePlanner
    val restoreEngine = container.restoreEngine
    val manifestRepository = container.manifestRepository
    val scope = rememberCoroutineScope()

    val dropState by engine.state.collectAsState()
    val restoreState by restoreEngine.state.collectAsState()
    val sets by setRepository.allSets.collectAsState(initial = emptyList())

    var activeSet by remember { mutableStateOf<SetDefinition?>(null) }
    var activeAttentionReason by remember { mutableStateOf<AttentionReason?>(null) }

    var activeRestorePlan by remember { mutableStateOf<RestorePlan?>(null) }
    var browseEntries by remember { mutableStateOf<List<ManifestEntry>>(emptyList()) }
    var targetNewPhoneBackup by remember { mutableStateOf<DiscoveredBackupSet?>(null) }
    var discoveredBackups by remember { mutableStateOf<List<DiscoveredBackupSet>>(emptyList()) }
    var unlockingBackup by remember { mutableStateOf<DiscoveredBackupSet?>(null) }
    var viewedReceipt by remember { mutableStateOf<Receipt?>(null) }

    val hasCompletedOnboarding by container.appPreferences.hasCompletedOnboarding.collectAsState()

    // If activeSet is not selected, default to the first set
    LaunchedEffect(sets) {
        if (activeSet == null && sets.isNotEmpty()) {
            activeSet = sets.first()
        }
    }

    val currentVaultUri = activeSet?.destinationVaultUriString?.let { Uri.parse(it) }
        ?: sets.firstOrNull()?.destinationVaultUriString?.let { Uri.parse(it) }
        ?: container.context.contentResolver.persistedUriPermissions.firstOrNull()?.uri

    // Discover backups whenever current dock or vault URI changes
    LaunchedEffect(currentDock, currentVaultUri) {
        if (currentVaultUri != null) {
            try {
                discoveredBackups = manifestRepository.discoverBackupsOnDrive(currentVaultUri)
            } catch (e: Exception) {
                discoveredBackups = emptyList()
            }
        } else {
            discoveredBackups = emptyList()
        }
    }

    // Automatically navigate to Receipt when state transitions to Sealed or Partial
    LaunchedEffect(dropState) {
        when (dropState) {
            is DropState.Sealed, is DropState.Partial -> {
                viewedReceipt = null
                navController.navigate(Routes.RECEIPT) {
                    launchSingleTop = true
                }
            }
            is DropState.Attention -> {
                activeAttentionReason = (dropState as DropState.Attention).reason
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (hasCompletedOnboarding) Routes.HOME else Routes.WELCOME,
        modifier = modifier
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onFinishOnboarding = {
                    container.appPreferences.setCompletedOnboarding(true)
                    navController.navigate(Routes.KEEP_ALIVE) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                dropState = dropState,
                sets = sets,
                onCreateSetClick = { navController.navigate(Routes.WIZARD) },
                onStartDropClick = { set ->
                    activeSet = set
                    onStartDropInService(set.id)
                    navController.navigate(Routes.DROP)
                },
                onNavigateToDrop = { navController.navigate(Routes.DROP) },
                onNavigateToReceipt = {
                    viewedReceipt = null
                    navController.navigate(Routes.RECEIPT)
                },
                onNavigateToRestore = { navController.navigate(Routes.RESTORE_HUB) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onOpenKeepAliveClick = { navController.navigate(Routes.KEEP_ALIVE) },
                onSetClick = { set ->
                    activeSet = set
                    navController.navigate(Routes.SET_DETAIL)
                }
            )
        }

        composable(Routes.WIZARD) {
            val defaultBatteryPct by container.appPreferences.defaultMinBatteryPct.collectAsState()
            SetWizardScreen(
                currentDock = currentDock,
                storageGateway = storageGateway,
                encryptedSetManager = container.encryptedSetManager,
                defaultMinBatteryPct = defaultBatteryPct,
                onWizardFinished = { newSet, folders ->
                    scope.launch {
                        setRepository.saveSet(newSet, folders)
                        activeSet = newSet
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Routes.DROP) {
            val set = activeSet ?: sets.firstOrNull()
            var folders by remember { mutableStateOf<List<SourceFolder>>(emptyList()) }

            LaunchedEffect(set) {
                if (set != null) {
                    folders = setRepository.getSourceFoldersSync(set.id)
                }
            }

            if (set != null) {
                DropScreen(
                    set = set,
                    sourceFolders = folders,
                    dropState = dropState,
                    onPauseClick = { onPauseDropInService() },
                    onResumeClick = { onResumeDropInService(set.id) },
                    onStopClick = { onStopDropInService() },
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                HomeScreen(
                    dropState = dropState,
                    sets = sets,
                    onCreateSetClick = { navController.navigate(Routes.WIZARD) },
                    onStartDropClick = {},
                    onNavigateToDrop = {},
                    onNavigateToReceipt = {},
                    onNavigateToRestore = { navController.navigate(Routes.RESTORE_HUB) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenKeepAliveClick = { navController.navigate(Routes.KEEP_ALIVE) }
                )
            }
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                appPreferences = container.appPreferences,
                setRepository = setRepository,
                sets = sets,
                onBackClick = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToRestore = {
                    navController.navigate(Routes.RESTORE_HUB) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToKeepAlive = { navController.navigate(Routes.KEEP_ALIVE) }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                setRepository = setRepository,
                onReceiptClick = { r ->
                    viewedReceipt = r
                    navController.navigate(Routes.RECEIPT)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.RECEIPT) {
            val receipt = viewedReceipt ?: when (dropState) {
                is DropState.Sealed -> (dropState as DropState.Sealed).receipt
                is DropState.Partial -> (dropState as DropState.Partial).receipt
                else -> null
            }

            if (receipt != null) {
                ReceiptScreen(
                    receipt = receipt,
                    onDone = {
                        viewedReceipt = null
                        navController.popBackStack()
                    },
                    onTestRestoreClick = {
                        viewedReceipt = null
                        navController.navigate(Routes.RESTORE_HUB)
                    }
                )
            } else {
                HomeScreen(
                    dropState = dropState,
                    sets = sets,
                    onCreateSetClick = { navController.navigate(Routes.WIZARD) },
                    onStartDropClick = {},
                    onNavigateToDrop = {},
                    onNavigateToReceipt = {},
                    onNavigateToRestore = { navController.navigate(Routes.RESTORE_HUB) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenKeepAliveClick = { navController.navigate(Routes.KEEP_ALIVE) }
                )
            }
        }

        composable(Routes.KEEP_ALIVE) {
            KeepAliveScreen(
                onDoneClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onRemindMeLaterClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.SET_DETAIL) {
            val set = activeSet ?: sets.firstOrNull()
            if (set != null) {
                SetDetailScreen(
                    set = set,
                    setRepository = setRepository,
                    onBack = { navController.popBackStack() },
                    onBackUpNow = {
                        onStartDropInService(set.id)
                        navController.navigate(Routes.DROP)
                    },
                    onDeleteConfirmed = {
                        activeSet = null
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(Routes.RESTORE_HUB) {
            RestoreHubScreen(
                currentDock = currentDock,
                sets = sets,
                discoveredBackups = discoveredBackups,
                restorePlanner = restorePlanner,
                sourceFoldersForSet = { setId -> setRepository.getSourceFoldersSync(setId) },
                onExecutePlan = { plan ->
                    activeRestorePlan = plan
                    onStartRestoreInService(plan)
                    navController.navigate(Routes.RESTORE_PROGRESS)
                },
                onNavigateToBrowser = { targetSet ->
                    activeSet = targetSet
                    navController.navigate(Routes.RESTORE_BROWSER)
                },
                onNavigateToCustomFolderRestore = { targetSet ->
                    activeSet = targetSet
                    browseEntries = emptyList()
                    navController.navigate(Routes.RESTORE_PREVIEW)
                },
                onNavigateToNewPhone = { backup ->
                    targetNewPhoneBackup = backup
                    navController.navigate(Routes.NEW_PHONE_RESTORE)
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                encryptedSetManager = container.encryptedSetManager,
                onPromptUnlock = { backup ->
                    unlockingBackup = backup
                }
            )
        }

        composable(Routes.RESTORE_BROWSER) {
            val targetSet = activeSet ?: sets.firstOrNull()
            if (targetSet != null) {
                RestoreBrowserScreen(
                    set = targetSet,
                    manifestRepository = manifestRepository,
                    onRestoreSelected = { selected ->
                        browseEntries = selected
                        navController.navigate(Routes.RESTORE_PREVIEW)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(Routes.RESTORE_PREVIEW) {
            val targetSet = activeSet ?: sets.firstOrNull()
            var folders by remember { mutableStateOf<List<SourceFolder>>(emptyList()) }

            LaunchedEffect(targetSet) {
                if (targetSet != null) {
                    folders = setRepository.getSourceFoldersSync(targetSet.id)
                }
            }

            if (targetSet != null) {
                RestorePreviewScreen(
                    set = targetSet,
                    sourceFolders = folders,
                    selectedEntries = browseEntries,
                    restorePlanner = restorePlanner,
                    onStartRestore = { plan ->
                        activeRestorePlan = plan
                        onStartRestoreInService(plan)
                        navController.navigate(Routes.RESTORE_PROGRESS) {
                            popUpTo(Routes.RESTORE_HUB) { inclusive = false }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(Routes.RESTORE_PROGRESS) {
            val plan = activeRestorePlan
            if (plan != null) {
                RestoreProgressScreen(
                    plan = plan,
                    restoreState = restoreState,
                    onStopClick = {
                        onStopRestoreInService()
                    },
                    onDoneClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onBackClick = {
                        navController.navigate(Routes.RESTORE_HUB) {
                            popUpTo(Routes.RESTORE_HUB) { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack(Routes.RESTORE_HUB, inclusive = false)
                }
            }
        }

        composable(Routes.NEW_PHONE_RESTORE) {
            val backup = targetNewPhoneBackup ?: discoveredBackups.firstOrNull()
            val vaultUri = currentVaultUri

            if (backup != null && vaultUri != null) {
                NewPhoneRestoreScreen(
                    backup = backup,
                    vaultTreeUri = vaultUri,
                    restorePlanner = restorePlanner,
                    setRepository = setRepository,
                    onExecutePlan = { plan ->
                        activeRestorePlan = plan
                        onStartRestoreInService(plan)
                        navController.navigate(Routes.RESTORE_PROGRESS) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    encryptedSetManager = container.encryptedSetManager,
                    onPromptUnlock = { b ->
                        unlockingBackup = b
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }

    // Modal Plug-in Sheet (SCREENS.md S04)
    if (showPlugInSheet || isUsbConnecting) {
        val matchingSet = sets.firstOrNull { it.dockVolumeUuid == currentDock?.volumeUuid } ?: sets.firstOrNull()
        PlugInSheet(
            dockInfo = currentDock,
            isUsbConnecting = isUsbConnecting,
            matchingSet = matchingSet,
            dropState = dropState,
            onStartBackup = { set ->
                onDismissPlugInSheet()
                activeSet = set
                onStartDropInService(set.id)
                navController.navigate(Routes.DROP)
            },
            onResumeBackup = { set ->
                onDismissPlugInSheet()
                activeSet = set
                onResumeDropInService(set.id)
                navController.navigate(Routes.DROP)
            },
            onSetupNewDock = {
                onDismissPlugInSheet()
                navController.navigate(Routes.WIZARD)
            },
            onDismiss = onDismissPlugInSheet
        )
    }

    // Modal Attention Bottom Sheet (SCREENS.md S11)
    activeAttentionReason?.let { reason ->
        AttentionSheet(
            reason = reason,
            onPrimaryAction = {
                activeAttentionReason = null
                when (reason) {
                    is AttentionReason.MassDelete -> {
                        engine.confirmMassDeleteAndResume()
                    }
                    is AttentionReason.NoSpace -> navController.navigate(Routes.WIZARD)
                    is AttentionReason.PermissionLost -> navController.navigate(Routes.WIZARD)
                    else -> {}
                }
            },
            onSecondaryAction = {
                activeAttentionReason = null
                if (reason is AttentionReason.MassDelete) {
                    engine.switchToKeepAndResume()
                }
            },
            onDismiss = { activeAttentionReason = null }
        )
    }

    // Modal Unlock Bottom Sheet (SCREENS.md S19)
    unlockingBackup?.let { backup ->
        val vaultUri = currentVaultUri
        if (vaultUri != null) {
            UnlockSheet(
                vaultTreeUri = vaultUri,
                setId = backup.setId,
                encryptedSetManager = container.encryptedSetManager,
                onUnlockSuccess = {
                    unlockingBackup = null
                },
                onDismiss = { unlockingBackup = null }
            )
        }
    }
}
