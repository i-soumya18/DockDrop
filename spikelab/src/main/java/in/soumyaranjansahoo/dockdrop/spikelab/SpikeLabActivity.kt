package `in`.soumyaranjansahoo.dockdrop.spikelab

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SpikeLabActivity : ComponentActivity() {

    companion object {
        const val ACTION_USB_PERMISSION = "in.soumyaranjansahoo.dockdrop.spikelab.USB_PERMISSION"
        const val ACTION_ADB_CMD = "in.soumyaranjansahoo.dockdrop.spikelab.ADB_CMD"
    }

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var storageCallback: StorageManager.StorageVolumeCallback? = null

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            val dataUri = intent.dataString ?: ""
            SpikeStore.appendTriggerEvent(
                context = context,
                eventType = "RUNTIME_MEDIA:${action.substringAfterLast('.')}",
                launchMethod = "Runtime BroadcastReceiver (file://)",
                details = "uri=$dataUri",
                eventRealtimeMs = SystemClock.elapsedRealtime()
            )
        }
    }

    private val usbRuntimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val dev = extractUsbDevice(intent)
                    SpikeStore.appendTriggerEvent(
                        context = context,
                        eventType = "RUNTIME_USB:${action.substringAfterLast('.')}",
                        launchMethod = "Runtime USB Receiver",
                        details = "device=${dev?.productName ?: dev?.deviceName ?: "null"}",
                        eventRealtimeMs = SystemClock.elapsedRealtime()
                    )
                }

                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val dev = extractUsbDevice(intent)
                    val permText = "granted=$granted (${dev?.productName ?: dev?.deviceName ?: "usb"})"
                    SpikeStore.updateStatus(context, "F1 USB Permission callback: $permText — starting connectedDevice FGS")
                    val svcIntent = Intent(context, SpikeForegroundService::class.java).apply {
                        this.action = SpikeForegroundService.ACTION_START_CONNECTED_DEVICE
                        putExtra(SpikeForegroundService.EXTRA_TEST_ID, "F1")
                        putExtra(SpikeForegroundService.EXTRA_USB_PERM_STATE, permText)
                    }
                    ContextCompat.startForegroundService(context, svcIntent)
                }

                ACTION_ADB_CMD -> {
                    handleAdbCommand(intent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpikeStore.ensureLoaded(this)
        SpikeForegroundService.inspectPreviousSessionHeartbeat(this)
        inspectLaunchIntent(intent, isColdStart = savedInstanceState == null)
        registerRuntimeListeners()

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                SpikeLabScreen(
                    onRequestUsbPermissionAndStartF1 = { requestUsbPermissionForF1() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        inspectLaunchIntent(intent, isColdStart = false)
    }

    private fun inspectLaunchIntent(intent: Intent?, isColdStart: Boolean) {
        val action = intent?.action ?: return
        val sourceExtra = intent.getStringExtra("launch_source")
        if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val dev = extractUsbDevice(intent)
            SpikeStore.appendTriggerEvent(
                context = this,
                eventType = "ACTIVITY_INTENT:USB_DEVICE_ATTACHED",
                launchMethod = if (isColdStart) "Cold-start Activity via USB filter" else "onNewIntent Activity via USB filter",
                details = "device=${dev?.productName ?: dev?.deviceName ?: "unknown"} vid=0x${dev?.vendorId?.toString(16)}",
                eventRealtimeMs = SystemClock.elapsedRealtime()
            )
        } else if (sourceExtra != null) {
            SpikeStore.appendTriggerEvent(
                context = this,
                eventType = "NOTIFICATION_TAP:$sourceExtra",
                launchMethod = if (isColdStart) "Cold-start from Notification" else "Warm-launch from Notification",
                details = "source=$sourceExtra",
                eventRealtimeMs = SystemClock.elapsedRealtime()
            )
        }
    }

    private fun registerRuntimeListeners() {
        // 1. StorageManager.StorageVolumeCallback (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sm = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val cb = object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(volume: StorageVolume) {
                    val desc = volume.getDescription(this@SpikeLabActivity)
                    val uuid = volume.uuid ?: "primary"
                    val state = volume.state
                    val removable = volume.isRemovable
                    SpikeStore.appendTriggerEvent(
                        context = this@SpikeLabActivity,
                        eventType = "STORAGE_VOLUME_CALLBACK:$state",
                        launchMethod = "StorageManager.StorageVolumeCallback",
                        details = "uuid=$uuid desc='$desc' removable=$removable",
                        eventRealtimeMs = SystemClock.elapsedRealtime()
                    )
                }
            }
            storageCallback = cb
            sm.registerStorageVolumeCallback(mainExecutor, cb)
        }

        // 2. Media Mounted / Unmounted runtime receiver
        val mediaFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(
            this,
            mediaReceiver,
            mediaFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // 3. USB attach/detach + permission + ADB command runtime receiver
        val usbFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
            addAction(ACTION_ADB_CMD)
        }
        ContextCompat.registerReceiver(
            this,
            usbRuntimeReceiver,
            usbFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun requestUsbPermissionForF1() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.values.toList()
        if (devices.isEmpty()) {
            // Even if no USB host device is enumerated (e.g., before plugging SSD or if kernel claims mass storage),
            // record what UsbManager reports and test whether connectedDevice FGS starts or throws SecurityException.
            SpikeStore.updateStatus(this, "F1: UsbManager.deviceList is empty (0 devices); testing connectedDevice FGS directly")
            val svcIntent = Intent(this, SpikeForegroundService::class.java).apply {
                action = SpikeForegroundService.ACTION_START_CONNECTED_DEVICE
                putExtra(SpikeForegroundService.EXTRA_TEST_ID, "F1")
                putExtra(SpikeForegroundService.EXTRA_USB_PERM_STATE, "deviceList=empty (0 USB host devices)")
            }
            ContextCompat.startForegroundService(this, svcIntent)
            return
        }

        val target = devices.first()
        val alreadyHasPerm = usbManager.hasPermission(target)
        if (alreadyHasPerm) {
            val permState = "hasPermission=true (${target.productName ?: target.deviceName})"
            val svcIntent = Intent(this, SpikeForegroundService::class.java).apply {
                action = SpikeForegroundService.ACTION_START_CONNECTED_DEVICE
                putExtra(SpikeForegroundService.EXTRA_TEST_ID, "F1")
                putExtra(SpikeForegroundService.EXTRA_USB_PERM_STATE, permState)
            }
            ContextCompat.startForegroundService(this, svcIntent)
        } else {
            val pi = PendingIntent.getBroadcast(
                this,
                4001,
                Intent(ACTION_USB_PERMISSION).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            SpikeStore.updateStatus(this, "F1: Calling UsbManager.requestPermission for ${target.productName ?: target.deviceName}")
            usbManager.requestPermission(target, pi)
        }
    }

    private fun handleAdbCommand(intent: Intent) {
        val cmd = intent.getStringExtra("cmd") ?: return
        val treeStr = SpikeStore.state.value.hardware.activeTreeUri
        activityScope.launch {
            when (cmd) {
                "export" -> SpikeStore.exportAllReports(this@SpikeLabActivity)
                "probe" -> {
                    val tag = intent.getStringExtra("tag") ?: "P1-Cycle1"
                    val note = intent.getStringExtra("note") ?: "via ADB"
                    SpikeRunners.runPersistenceProbe(this@SpikeLabActivity, treeStr, tag, note)
                }
                "rename" -> {
                    if (treeStr.isNotBlank()) {
                        SpikeRunners.runRenameAndNameSuite(this@SpikeLabActivity, Uri.parse(treeStr))
                    }
                }
                "bench" -> {
                    if (treeStr.isNotBlank()) {
                        val testId = intent.getStringExtra("testId") ?: "B1"
                        val bufKb = intent.getIntExtra("bufKb", 1024)
                        val runs = intent.getIntExtra("runs", 3)
                        SpikeRunners.runThroughputWorkload(this@SpikeLabActivity, Uri.parse(treeStr), testId, bufKb, runs)
                    }
                }
                "list5000" -> {
                    if (treeStr.isNotBlank()) {
                        val count = intent.getIntExtra("count", 5000)
                        val runs = intent.getIntExtra("runs", 3)
                        SpikeRunners.runListingBenchmark(this@SpikeLabActivity, Uri.parse(treeStr), count, runs)
                    }
                }
            }
        }
    }

    private fun extractUsbDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    override fun onDestroy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sm = getSystemService(Context.STORAGE_SERVICE) as StorageManager
                storageCallback?.let { sm.unregisterStorageVolumeCallback(it) }
            }
            unregisterReceiver(mediaReceiver)
            unregisterReceiver(usbRuntimeReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}

@Composable
fun SpikeLabScreen(
    onRequestUsbPermissionAndStartF1: () -> Unit
) {
    val context = LocalContext.current
    val state by SpikeStore.state.collectAsState()
    val progress by SpikeRunners.progress.collectAsState()
    val fgsStatus by SpikeForegroundService.liveStatus.collectAsState()
    var selectedTab by remember { mutableIntStateOf(4) } // Default to Grantability (Stage 1) on first open

    val tabs = listOf(
        "1. Trigger (T)",
        "2. SAF & Rename (P/R)",
        "3. Throughput (B1-B4)",
        "4. Listing (B5)",
        "5. Grant (G1-G7)",
        "6. FGS Lab (F1-F4)",
        "7. Export"
    )

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        SpikeStore.updateStatus(context, "POST_NOTIFICATIONS granted=$granted")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Status Banner
            Surface(
                color = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "S22 SPIKE LAB · ${state.hardware.modelCode} (DEBUG ONLY)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            OutlinedButton(
                                onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            ) {
                                Text("Allow Notifs", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                    Text(
                        text = state.statusBanner,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF93C5FD)
                    )
                    if (progress.isRunning) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${progress.title}: ${progress.detail}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFDE047)
                        )
                        LinearProgressIndicator(
                            progress = { progress.fraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    )
                }
            }

            when (selectedTab) {
                0 -> TriggerLogTab(state)
                1 -> SafPersistenceAndRenameTab(state, progress)
                2 -> ThroughputBenchTab(state, progress)
                3 -> ListingBenchTab(state, progress)
                4 -> GrantabilityMatrixTab(state)
                5 -> ForegroundServiceLabTab(state, fgsStatus, onRequestUsbPermissionAndStartF1)
                6 -> ExportTab(state)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriggerLogTab(state: SpikeLabState) {
    val context = LocalContext.current
    val scenarios = listOf("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8")
    val dialogOptions = listOf(
        "None observed",
        "Samsung My Files notification",
        "Samsung My Files auto-opened",
        "USB App Chooser shown",
        "Always open with DockDrop worked"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text("Active Scenario Tag (select before plugging in SSD):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            scenarios.forEach { tag ->
                FilterChip(
                    selected = state.activeTriggerScenario == tag,
                    onClick = { SpikeStore.setTriggerScenario(context, tag) },
                    label = { Text(tag, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                )
            }
        }

        Text("System Dialog / Interference Note:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dialogOptions.forEach { note ->
                FilterChip(
                    selected = state.systemDialogNote == note,
                    onClick = { SpikeStore.setTriggerScenario(context, state.activeTriggerScenario, note) },
                    label = { Text(note, fontSize = 11.sp) }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Button(
                onClick = { SpikeStore.markPlugTimestamp(context, SystemClock.elapsedRealtime()) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Mark Plug-In (t=0)", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                    val vols = sm.storageVolumes.joinToString("; ") { v ->
                        "${v.getDescription(context)}[uuid=${v.uuid},state=${v.state},rem=${v.isRemovable}]"
                    }
                    val um = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    val usbs = um.deviceList.values.joinToString("; ") { d ->
                        "${d.productName ?: d.deviceName}(vid=0x${d.vendorId.toString(16)},pid=0x${d.productId.toString(16)})"
                    }
                    SpikeStore.appendTriggerEvent(
                        context = context,
                        eventType = "MANUAL_SNAPSHOT",
                        launchMethod = "User Snapshot Button",
                        details = "Volumes=[$vols] USB=[$usbs]"
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Log Volume/USB Snapshot", fontSize = 12.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = { SpikeStore.exportAllReports(context) }) {
                Text("Copy Markdown", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { SpikeStore.clearTriggerEvents(context) }) {
                Text("Clear Log (${state.triggerEvents.size})", fontSize = 12.sp)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.triggerEvents) { ev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "[${ev.scenarioTag}] ${ev.eventType} (${if (ev.latencyMs >= 0) "${ev.latencyMs}ms" else "no t=0"})",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${ev.isoTime} · State=${ev.processState} · Via=${ev.launchMethod}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Dialog=${ev.systemDialogInterfered} | ${ev.details}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SafPersistenceAndRenameTab(state: SpikeLabState, progress: BenchProgress) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val probeTags = listOf("P1-Cycle1", "P1-Cycle2", "P1-Cycle3", "P2-Reboot", "P3-ForceStop", "P4-HubPort", "P5-LabelChange")
    var selectedProbeTag by remember { mutableStateOf("P1-Cycle1") }
    var probeNote by remember { mutableStateOf("") }

    val persistedPerms = remember(state) {
        context.contentResolver.persistedUriPermissions.map {
            "${it.uri} (R=${it.isReadPermission}, W=${it.isWritePermission})"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Persisted URI Permissions (${persistedPerms.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            persistedPerms.forEach { line ->
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            if (persistedPerms.isEmpty()) {
                Text("None yet — grant SSD in Tab 5 (G7) first.", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Red)
            }
        }

        item {
            HorizontalDivider()
            Text("P1–P5: SAF Persistence Probe", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                probeTags.forEach { tag ->
                    FilterChip(
                        selected = selectedProbeTag == tag,
                        onClick = { selectedProbeTag = tag },
                        label = { Text(tag, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    )
                }
            }
            OutlinedTextField(
                value = probeNote,
                onValueChange = { probeNote = it },
                label = { Text("Optional Probe Note (e.g. 'After 2nd replug')") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        SpikeRunners.runPersistenceProbe(
                            context = context,
                            treeUriString = state.hardware.activeTreeUri,
                            scenarioTag = selectedProbeTag,
                            userNotes = probeNote
                        )
                    }
                },
                enabled = !progress.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Probe ($selectedProbeTag): List + Write/Read/Delete 1 KB")
            }
        }

        items(state.persistenceProbes) { p ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (p.inPersistedList && p.writeReadDeleteSucceeded) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "[${p.scenarioTag}] persisted=${p.inPersistedList} · list=${p.listSucceeded}(${p.childCount}) · writeReadDel=${p.writeReadDeleteSucceeded} (${p.elapsedMs}ms)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Vol=${p.volumeUuid} (${p.volumeDescription}) · ${p.notes}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            HorizontalDivider()
            Text("R1–R4: Rename & Filename Semantics on exFAT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(
                onClick = {
                    val uriStr = state.hardware.activeTreeUri
                    if (uriStr.isNotBlank()) {
                        scope.launch {
                            try {
                                SpikeRunners.runRenameAndNameSuite(context, Uri.parse(uriStr))
                            } catch (e: Exception) {
                                SpikeStore.updateStatus(context, "R-suite error: ${e.message}")
                            }
                        }
                    } else {
                        SpikeStore.updateStatus(context, "Grant SSD tree in Tab 5 (G7) first!")
                    }
                },
                enabled = !progress.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Automated R1–R4 Suite on SSD")
            }
        }

        items(state.renameResults) { r ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "${r.testId}: ${r.behaviorSummary} (${r.elapsedMs}ms)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "In:  '${r.inputName.take(60)}'\nOut: '${r.actualDisplayName.take(60)}'",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = r.details,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThroughputBenchTab(state: SpikeLabState, progress: BenchProgress) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workloads = listOf(
        "B1" to "B1: 1 GB Single Write",
        "B2" to "B2: 2,000 × 100 KB Write",
        "B3" to "B3: Mixed (200×1-20MB + 2000×100KB)",
        "B4-B1" to "B4: Read-Back B1 (1 GB)",
        "B4-B2" to "B4: Read-Back B2 (2,000 small)",
        "B4-B3" to "B4: Read-Back B3 (Mixed)"
    )
    val buffers = listOf(256, 1024, 4096)

    var selectedWorkload by remember { mutableStateOf("B1") }
    var selectedBufferKb by remember { mutableIntStateOf(1024) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Select Workload:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                workloads.forEach { (id, label) ->
                    FilterChip(
                        selected = selectedWorkload == id,
                        onClick = { selectedWorkload = id },
                        label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    )
                }
            }

            Text("Buffer Size:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buffers.forEach { kb ->
                    FilterChip(
                        selected = selectedBufferKb == kb,
                        onClick = { selectedBufferKb = kb },
                        label = { Text(if (kb >= 1024) "${kb / 1024} MB ($kb KB)" else "$kb KB", fontFamily = FontFamily.Monospace) }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val uriStr = state.hardware.activeTreeUri
                        if (uriStr.isBlank()) {
                            SpikeStore.updateStatus(context, "Grant SSD in Tab 5 (G7) first!")
                            return@Button
                        }
                        scope.launch {
                            try {
                                SpikeRunners.runThroughputWorkload(context, Uri.parse(uriStr), selectedWorkload, selectedBufferKb, iterations = 3)
                            } catch (e: Exception) {
                                SpikeStore.updateStatus(context, "Bench error: ${e.message}")
                            }
                        }
                    },
                    enabled = !progress.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run 3× (Report Median)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val uriStr = state.hardware.activeTreeUri
                        if (uriStr.isBlank()) {
                            SpikeStore.updateStatus(context, "Grant SSD in Tab 5 (G7) first!")
                            return@OutlinedButton
                        }
                        scope.launch {
                            try {
                                SpikeRunners.runThroughputWorkload(context, Uri.parse(uriStr), selectedWorkload, selectedBufferKb, iterations = 1)
                            } catch (e: Exception) {
                                SpikeStore.updateStatus(context, "Bench error: ${e.message}")
                            }
                        }
                    },
                    enabled = !progress.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run 1× Iteration", fontSize = 12.sp)
                }
            }

            if (progress.isRunning) {
                OutlinedButton(
                    onClick = { SpikeRunners.requestCancel() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Running Benchmark", color = Color.Red)
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Recorded Runs (${state.throughputRuns.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        items(state.throughputRuns.reversed()) { r ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "${r.testId} #${r.runNumber} (${r.bufferSizeKb} KB, chg=${r.isCharging}) → ${"%.2f".format(r.mbPerSec)} MB/s · ${"%.1f".format(r.filesPerSec)} files/s",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Elapsed=${r.elapsedMs / 1000.0}s · Files=${r.fileCount} · Bat=${r.batteryStartPct}%→${r.batteryEndPct}% · Temp=${r.tempStartC}→${r.tempEndC}°C · HeapΔ=${"%.1f".format(r.heapDeltaMb)}MB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingBenchTab(state: SpikeLabState, progress: BenchProgress) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "B5: List 5,000 Files — DocumentsContract Batch Cursor vs Per-File DocumentFile",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "Prepares 5,000 files in .spikelab_bench/b5_list_5000 (reusing existing files if already created), then runs 3× Batch Cursor Queries and 3× DocumentFile.listFiles() iterations.",
                fontSize = 12.sp
            )
            Button(
                onClick = {
                    val uriStr = state.hardware.activeTreeUri
                    if (uriStr.isBlank()) {
                        SpikeStore.updateStatus(context, "Grant SSD in Tab 5 (G7) first!")
                        return@Button
                    }
                    scope.launch {
                        try {
                            SpikeRunners.runListingBenchmark(context, Uri.parse(uriStr), targetCount = 5000, iterations = 3)
                        } catch (e: Exception) {
                            SpikeStore.updateStatus(context, "B5 error: ${e.message}")
                        }
                    }
                },
                enabled = !progress.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run B5 5,000-File Listing Benchmark (3× Each)")
            }

            if (progress.isRunning) {
                OutlinedButton(
                    onClick = { SpikeRunners.requestCancel() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel B5", color = Color.Red)
                }
            }
        }

        items(state.listingRuns.reversed()) { l ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "${l.method} #${l.runNumber}: ${l.fileCount} files in ${l.elapsedMs} ms",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${"%.1f".format(l.msPer1kFiles)} ms / 1,000 files · HeapΔ=${"%.2f".format(l.heapDeltaMb)} MB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GrantabilityMatrixTab(state: SpikeLabState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRecord by remember { mutableStateOf<GrantabilityRecord?>(null) }

    val treePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val rec = pendingRecord ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                val updated = SpikeRunners.evaluateGrantedTree(context, rec, uri)
                SpikeStore.updateGrantability(context, updated)
            }
        } else {
            SpikeStore.updateStatus(context, "Picker cancelled for ${rec.id} (${rec.label}) — if OS blocked 'Use this folder', tap 'Mark Blocked'")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "G1–G7: SAF Folder Picker Grantability Matrix",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "Tap 'Open Picker' to launch ACTION_OPEN_DOCUMENT_TREE pre-navigated to each target. If Android 15 grey-outs 'Use this folder' (" +
                    "Can't use this folder to protect your privacy), tap 'Mark Blocked by OS'.",
                fontSize = 12.sp
            )
        }

        items(state.grantability) { rec ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (rec.status) {
                        "GRANTED" -> Color(0xFFDCFCE7)
                        "BLOCKED_BY_PICKER" -> Color(0xFFFEF3C7)
                        "ERROR" -> Color(0xFFFEE2E2)
                        else -> Color(0xFFF1F5F9)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "${rec.id} · ${rec.label} [${rec.status}]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Hint: ${rec.initialPathHint} · List=${rec.canList}(${rec.childCount}) · Read=${rec.canReadSample}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    if (rec.grantedUri.isNotBlank()) {
                        Text(
                            text = "URI: ${rec.grantedUri}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    if (rec.notes.isNotBlank()) {
                        Text(
                            text = "Note: ${rec.notes}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Button(
                            onClick = {
                                pendingRecord = rec
                                val initialUri = buildInitialUriHint(rec.initialPathHint)
                                treePickerLauncher.launch(initialUri)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Picker", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                SpikeStore.updateGrantability(
                                    context,
                                    rec.copy(
                                        status = "BLOCKED_BY_PICKER",
                                        canList = false,
                                        canReadSample = false,
                                        notes = "OS picker blocked root ('Can't use this folder to protect your privacy'); requires sub-folder"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Blocked by OS", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun buildInitialUriHint(pathHint: String): Uri? {
    if (pathHint == "SSD_ROOT") return null
    val encodedDocId = Uri.encode("primary:$pathHint")
    return Uri.parse("content://com.android.externalstorage.documents/document/$encodedDocId")
}

@Composable
private fun ForegroundServiceLabTab(
    state: SpikeLabState,
    fgsStatus: FgsLiveStatus,
    onRequestUsbPermissionAndStartF1: () -> Unit
) {
    val context = LocalContext.current
    val pm = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isExempt by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Live FGS Status: ${if (fgsStatus.isRunning) "RUNNING (${fgsStatus.serviceType})" else "STOPPED"}",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Elapsed=${fgsStatus.elapsedSec}s · ScreenOff=${fgsStatus.screenOffSec}s · Heartbeats=${fgsStatus.heartbeats}",
                        color = Color(0xFF93C5FD),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = fgsStatus.lastMessage,
                        color = Color(0xFFFDE047),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            isExempt = pm.isIgnoringBatteryOptimizations(context.packageName)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Battery Optimization Exemption: $isExempt",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Request Exemption", fontSize = 11.sp)
                }
            }
        }

        item {
            HorizontalDivider()
            Text("F1: UsbManager.requestPermission() + connectedDevice FGS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Button(
                onClick = onRequestUsbPermissionAndStartF1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run F1: Request USB Perm & Start connectedDevice FGS", fontSize = 12.sp)
            }
        }

        item {
            Text("F2 & F4: dataSync FGS (15+ min / 10+ min Screen-Off)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val svc = Intent(context, SpikeForegroundService::class.java).apply {
                            action = SpikeForegroundService.ACTION_START_DATA_SYNC
                            putExtra(SpikeForegroundService.EXTRA_TEST_ID, "F2")
                        }
                        ContextCompat.startForegroundService(context, svc)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start F2 (dataSync)", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        val svc = Intent(context, SpikeForegroundService::class.java).apply {
                            action = SpikeForegroundService.ACTION_START_DATA_SYNC
                            putExtra(SpikeForegroundService.EXTRA_TEST_ID, "F4")
                        }
                        ContextCompat.startForegroundService(context, svc)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start F4 (Screen Off)", fontSize = 12.sp)
                }
            }
        }

        item {
            Text("F3: Forced Short Timeout → Exercise onTimeout() Checkpoint & Stop", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val svc = Intent(context, SpikeForegroundService::class.java).apply {
                            action = SpikeForegroundService.ACTION_START_DATA_SYNC
                            putExtra(SpikeForegroundService.EXTRA_TEST_ID, "F3")
                            putExtra(SpikeForegroundService.EXTRA_SHORT_TIMEOUT_SEC, 5L)
                        }
                        ContextCompat.startForegroundService(context, svc)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run F3 (5s Short Timeout)", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        val svc = Intent(context, SpikeForegroundService::class.java).apply {
                            action = SpikeForegroundService.ACTION_SIMULATE_TIMEOUT
                        }
                        context.startService(svc)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trigger onTimeout() Now", fontSize = 11.sp)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val svc = Intent(context, SpikeForegroundService::class.java).apply {
                        action = SpikeForegroundService.ACTION_STOP
                    }
                    context.startService(svc)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Running Service Cleanly & Record Result")
            }
        }

        items(state.fgsRecords) { f ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "[${f.testId}] ${f.serviceType} · started=${f.startedOk} · dur=${f.durationSec}s · timeout=${f.timeoutCallbackFired}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "USB=${f.usbPermGranted} · Exempt=${f.batteryExempt} · Checkpoint=${f.checkpointSavedMs}ms",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${f.survivedScreenOff} | ${f.notes} (${f.exitReason})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportTab(state: SpikeLabState) {
    val context = LocalContext.current
    var ssdModel by remember(state.hardware.ssdBrandModel) { mutableStateOf(state.hardware.ssdBrandModel) }
    var ssdCap by remember(state.hardware.ssdCapacity) { mutableStateOf(state.hardware.ssdCapacity) }
    var cableHub by remember(state.hardware.cableAdapterHub) { mutableStateOf(state.hardware.cableAdapterHub) }
    var previewMd by remember(state) { mutableStateOf(SpikeStore.buildMarkdownReport(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Hardware Metadata for PHASE0_RESULTS.md:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedTextField(
            value = ssdModel,
            onValueChange = {
                ssdModel = it
                SpikeStore.updateHardware(context) { hw -> hw.copy(ssdBrandModel = it) }
            },
            label = { Text("SSD Brand & Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = ssdCap,
            onValueChange = {
                ssdCap = it
                SpikeStore.updateHardware(context) { hw -> hw.copy(ssdCapacity = it) }
            },
            label = { Text("SSD Capacity & Filesystem (e.g. 1 TB exFAT)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = cableHub,
            onValueChange = {
                cableHub = it
                SpikeStore.updateHardware(context) { hw -> hw.copy(cableAdapterHub = it) }
            },
            label = { Text("Cable / Adapter / Hub Used") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    SpikeStore.exportAllReports(context)
                    previewMd = SpikeStore.buildMarkdownReport(context)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Export MD + CSV & Copy", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    val md = SpikeStore.buildMarkdownReport(context)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_SUBJECT, "PHASE0_RESULTS.md")
                        putExtra(Intent.EXTRA_TEXT, md)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share PHASE0_RESULTS.md"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Share Report", fontSize = 12.sp)
            }
        }

        HorizontalDivider()
        Text("Live Markdown Preview:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(
            text = previewMd,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(8.dp)
        )
    }
}
