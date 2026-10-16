package `in`.soumyaranjansahoo.dockdrop.ui.navigation

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
import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.ui.screens.attention.AttentionSheet
import `in`.soumyaranjansahoo.dockdrop.ui.screens.drop.DropScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.home.HomeScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.onboarding.KeepAliveScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.plugin.PlugInSheet
import `in`.soumyaranjansahoo.dockdrop.ui.screens.receipt.ReceiptScreen
import `in`.soumyaranjansahoo.dockdrop.ui.screens.wizard.SetWizardScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val WIZARD = "wizard"
    const val DROP = "drop"
    const val RECEIPT = "receipt"
    const val KEEP_ALIVE = "keep_alive"
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
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val engine = container.engine
    val setRepository = container.setRepository
    val storageGateway = container.storageGateway
    val scope = rememberCoroutineScope()

    val dropState by engine.state.collectAsState()
    val sets by setRepository.allSets.collectAsState(initial = emptyList())

    var activeSet by remember { mutableStateOf<SetDefinition?>(null) }
    var activeAttentionReason by remember { mutableStateOf<AttentionReason?>(null) }

    // If activeSet is not selected, default to the first set
    LaunchedEffect(sets) {
        if (activeSet == null && sets.isNotEmpty()) {
            activeSet = sets.first()
        }
    }

    // Automatically navigate to Receipt when state transitions to Sealed or Partial
    LaunchedEffect(dropState) {
        when (dropState) {
            is DropState.Sealed, is DropState.Partial -> {
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
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
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
                onNavigateToReceipt = { navController.navigate(Routes.RECEIPT) },
                onOpenKeepAliveClick = { navController.navigate(Routes.KEEP_ALIVE) }
            )
        }

        composable(Routes.WIZARD) {
            SetWizardScreen(
                currentDock = currentDock,
                storageGateway = storageGateway,
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
                    onOpenKeepAliveClick = { navController.navigate(Routes.KEEP_ALIVE) }
                )
            }
        }

        composable(Routes.RECEIPT) {
            val receipt = when (dropState) {
                is DropState.Sealed -> (dropState as DropState.Sealed).receipt
                is DropState.Partial -> (dropState as DropState.Partial).receipt
                else -> null
            }

            if (receipt != null) {
                ReceiptScreen(
                    receipt = receipt,
                    onDone = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
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
                    onOpenKeepAliveClick = { navController.navigate(Routes.KEEP_ALIVE) }
                )
            }
        }

        composable(Routes.KEEP_ALIVE) {
            KeepAliveScreen(
                onDoneClick = { navController.popBackStack() },
                onRemindMeLaterClick = { navController.popBackStack() }
            )
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
                    is AttentionReason.NoSpace -> navController.navigate(Routes.WIZARD)
                    is AttentionReason.PermissionLost -> navController.navigate(Routes.WIZARD)
                    else -> {}
                }
            },
            onDismiss = { activeAttentionReason = null }
        )
    }
}
