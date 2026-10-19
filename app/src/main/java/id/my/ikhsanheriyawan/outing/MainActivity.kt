package id.my.ikhsanheriyawan.outing

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import id.my.ikhsanheriyawan.outing.alarm.AlarmScheduler
import id.my.ikhsanheriyawan.outing.data.CheckPrefs
import id.my.ikhsanheriyawan.outing.data.Schedule
import id.my.ikhsanheriyawan.outing.data.ScheduleEvent
import id.my.ikhsanheriyawan.outing.data.ScheduleStore
import id.my.ikhsanheriyawan.outing.data.SetupPrefs
import id.my.ikhsanheriyawan.outing.logic.TimeFormat
import id.my.ikhsanheriyawan.outing.logic.Timeline
import id.my.ikhsanheriyawan.outing.logic.TimelineItem
import id.my.ikhsanheriyawan.outing.ui.theme.OutingGarutTheme

private enum class AppTab(val label: String) {
    SCHEDULE("Jadwal"),
    CHECKLIST("Checklist"),
    SETUP("Setup"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmScheduler.rescheduleAll(this)
        setContent {
            OutingGarutTheme {
                OutingApp(
                    readSchedule = { ScheduleStore.read(this) },
                    importSchedule = ::importSchedule,
                    onScheduleTest = { AlarmScheduler.scheduleTest(this) },
                )
            }
        }
    }

    private fun importSchedule(uri: Uri): Result<Schedule> = runCatching {
        val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("File tidak bisa dibaca.")
        ScheduleStore.replace(this, content)
        AlarmScheduler.rescheduleAll(this)
        ScheduleStore.read(this) ?: error("Jadwal tidak tersimpan.")
    }
}

@Composable
private fun OutingApp(
    readSchedule: () -> Schedule?,
    importSchedule: (Uri) -> Result<Schedule>,
    onScheduleTest: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AppTab.SCHEDULE) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val schedule = remember(refreshKey) { runCatching(readSchedule).getOrNull() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Outing Garut") }) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            when (tab) {
                                AppTab.SCHEDULE -> Icon(painterResource(R.drawable.ic_tab_schedule), contentDescription = null)
                                AppTab.CHECKLIST -> Icon(painterResource(R.drawable.ic_tab_checklist), contentDescription = null)
                                AppTab.SETUP -> Icon(painterResource(R.drawable.ic_tab_setup), contentDescription = null)
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        when (selectedTab) {
            AppTab.SCHEDULE -> ScheduleTab(
                modifier = Modifier.padding(paddingValues),
                schedule = schedule,
                onOpenSetup = { selectedTab = AppTab.SETUP },
            )
            AppTab.CHECKLIST -> ChecklistTab(
                modifier = Modifier.padding(paddingValues),
                schedule = schedule,
            )
            AppTab.SETUP -> SetupTab(
                modifier = Modifier.padding(paddingValues),
                onImport = importSchedule,
                onImported = {
                    refreshKey++
                    selectedTab = AppTab.SCHEDULE
                },
                onScheduleTest = onScheduleTest,
            )
        }
    }
}

@Composable
private fun ScheduleTab(
    modifier: Modifier,
    schedule: Schedule?,
    onOpenSetup: () -> Unit,
) {
    if (schedule == null) {
        EmptySchedule(modifier, onOpenSetup)
        return
    }

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedItem by remember { mutableStateOf<TimelineItem?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val current = Timeline.currentEvent(schedule, nowMillis)
    val next = Timeline.nextItem(schedule, nowMillis)
    val groupedItems = Timeline.groupedByDay(schedule)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NowNextCard(current, next, nowMillis)
        }
        groupedItems.forEach { (date, itemsForDay) ->
            item {
                Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(itemsForDay, key = TimelineItem::id) { item ->
                TimelineRow(
                    item = item,
                    isPast = item.at < nowMillis,
                    onClick = { selectedItem = item },
                )
            }
        }
    }

    if (selectedItem != null) {
        TimelineDetailDialog(selectedItem!!, onDismiss = { selectedItem = null })
    }
}

@Composable
private fun EmptySchedule(modifier: Modifier, onOpenSetup: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Belum ada jadwal. Buka tab Setup → Import JSON.")
        Button(modifier = Modifier.padding(top = 16.dp), onClick = onOpenSetup) {
            Text("Buka Setup")
        }
    }
}

@Composable
private fun NowNextCard(
    current: ScheduleEvent?,
    next: TimelineItem?,
    nowMillis: Long,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sekarang", style = MaterialTheme.typography.labelLarge)
            Text(
                current?.title ?: "Tidak ada kegiatan berlangsung.",
                style = MaterialTheme.typography.titleMedium,
            )
            if (current != null) {
                Text("${TimeFormat.range(current.start, current.end)} · ${current.location}".trimEnd(' ', '·'))
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Berikutnya", style = MaterialTheme.typography.labelLarge)
            if (next == null) {
                Text("Tidak ada agenda berikutnya.", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(next.title, style = MaterialTheme.typography.titleMedium)
                Text("${TimeFormat.hour(next.at)} · ${remainingLabel(next.at - nowMillis)}")
            }
        }
    }
}

@Composable
private fun TimelineRow(item: TimelineItem, isPast: Boolean, onClick: () -> Unit) {
    val alpha = if (isPast) 0.55f else 1f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = TimeFormat.hour(item.at),
                modifier = Modifier.width(56.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Column {
                Text(item.title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                val subtitle = when (item) {
                    is TimelineItem.Event -> item.event.location
                    is TimelineItem.Reminder -> item.reminder.body
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineDetailDialog(item: TimelineItem, onDismiss: () -> Unit) {
    val content = when (item) {
        is TimelineItem.Event -> buildString {
            append("Jam: ${TimeFormat.range(item.event.start, item.event.end)}")
            if (item.event.location.isNotBlank()) append("\nLokasi: ${item.event.location}")
            if (item.event.dresscode.isNotBlank()) append("\nDresscode: ${item.event.dresscode}")
            if (item.event.notes.isNotBlank()) append("\nCatatan: ${item.event.notes}")
        }
        is TimelineItem.Reminder -> "Jam: ${TimeFormat.hour(item.reminder.at)}\n${item.reminder.body}".trim()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = { Text(content) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun ChecklistTab(modifier: Modifier, schedule: Schedule?) {
    val context = LocalContext.current
    var checkedIds by remember(schedule) { mutableStateOf(CheckPrefs.checkedIds(context)) }
    val items = schedule?.checklist.orEmpty()

    if (items.isEmpty()) {
        EmptyMessage(modifier, "Belum ada checklist dari jadwal yang diimpor.")
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "${items.count { it.id in checkedIds }}/${items.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        items.groupBy { it.group }.forEach { (group, groupItems) ->
            item { Text(group, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(groupItems, key = { it.id }) { item ->
                val checked = item.id in checkedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            CheckPrefs.setChecked(context, item.id, !checked)
                            checkedIds = CheckPrefs.checkedIds(context)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            CheckPrefs.setChecked(context, item.id, isChecked)
                            checkedIds = CheckPrefs.checkedIds(context)
                        },
                    )
                    Text(item.text)
                }
            }
        }
    }
}

@Composable
private fun SetupTab(
    modifier: Modifier,
    onImport: (Uri) -> Result<Schedule>,
    onImported: () -> Unit,
    onScheduleTest: () -> Unit,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var manualSteps by remember(refreshKey) { mutableStateOf(SetupPrefs.completedIds(context)) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        onImport(uri).onSuccess { schedule ->
            Toast.makeText(context, "Import berhasil: ${schedule.events.size + schedule.reminders.size} alarm diproses.", Toast.LENGTH_LONG).show()
            refreshKey++
            onImported()
        }.onFailure { error ->
            Toast.makeText(context, "Import ditolak: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }
    val nextAlarm = remember(refreshKey) { AlarmScheduler.nextAlarmMillis(context) }
    val alarmCount = remember(refreshKey) { AlarmScheduler.scheduledAlarmCount(context) }
    val ignoresBatteryOptimization = remember(refreshKey) {
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(modifier = Modifier.fillMaxWidth(), onClick = { importFile.launch(arrayOf("application/json", "*/*")) }) {
                Text("Import JSON")
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Status alarm", style = MaterialTheme.typography.titleMedium)
                    Text("Terjadwal: $alarmCount")
                    Text("Berikutnya: ${nextAlarm?.let(TimeFormat::hour) ?: "Tidak ada"}")
                }
            }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onScheduleTest()
                    refreshKey++
                    Toast.makeText(context, "Tes notifikasi dijadwalkan 1 menit lagi.", Toast.LENGTH_LONG).show()
                },
            ) {
                Text("Tes notifikasi 1 menit lagi")
            }
        }
        item { Text("Setup MIUI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            SetupCheckRow(
                checked = "autostart" in manualSteps,
                text = "Autostart aktif",
                detail = "Buka pengaturan Autostart MIUI.",
                onChecked = { checked ->
                    SetupPrefs.setCompleted(context, "autostart", checked)
                    manualSteps = SetupPrefs.completedIds(context)
                },
                onAction = { openAutostartSettings(context) },
            )
        }
        item {
            SetupCheckRow(
                checked = ignoresBatteryOptimization,
                text = "Baterai: Tanpa batasan",
                detail = if (ignoresBatteryOptimization) "Status sistem: aktif." else "Minta pengecualian optimasi baterai.",
                onChecked = null,
                onAction = { requestBatteryExemption(context) },
            )
        }
        item {
            SetupCheckRow(
                checked = "recents" in manualSteps,
                text = "Kunci aplikasi di Recents",
                detail = "Tekan lama kartu aplikasi, lalu pilih ikon gembok.",
                onChecked = { checked ->
                    SetupPrefs.setCompleted(context, "recents", checked)
                    manualSteps = SetupPrefs.completedIds(context)
                },
                onAction = null,
            )
        }
        item {
            SetupCheckRow(
                checked = "notifications" in manualSteps,
                text = "Notifikasi: Urgent/Penting, pop-up, layar kunci",
                detail = "Buka pengaturan notifikasi aplikasi.",
                onChecked = { checked ->
                    SetupPrefs.setCompleted(context, "notifications", checked)
                    manualSteps = SetupPrefs.completedIds(context)
                },
                onAction = { openNotificationSettings(context) },
            )
        }
        item {
            SetupCheckRow(
                checked = "dnd" in manualSteps,
                text = "DND dimatikan atau aplikasi dikecualikan",
                detail = "Pastikan Jangan Ganggu tidak membisukan pengingat.",
                onChecked = { checked ->
                    SetupPrefs.setCompleted(context, "dnd", checked)
                    manualSteps = SetupPrefs.completedIds(context)
                },
                onAction = null,
            )
        }
        item {
            SetupCheckRow(
                checked = "swipe" in manualSteps,
                text = "Jangan swipe-kill aplikasi",
                detail = "Biarkan aplikasi tetap tersedia di Recents.",
                onChecked = { checked ->
                    SetupPrefs.setCompleted(context, "swipe", checked)
                    manualSteps = SetupPrefs.completedIds(context)
                },
                onAction = null,
            )
        }
    }
}

@Composable
private fun SetupCheckRow(
    checked: Boolean,
    text: String,
    detail: String,
    onChecked: ((Boolean) -> Unit)?,
    onAction: (() -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Column(modifier = Modifier.weight(1f)) {
                Text(text)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            if (onAction != null) {
                TextButton(onClick = onAction) { Text("Buka") }
            }
        }
    }
}

@Composable
private fun EmptyMessage(modifier: Modifier, message: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
    }
}

private fun remainingLabel(millis: Long): String {
    val minutes = (millis.coerceAtLeast(0L) + 59_999L) / 60_000L
    return if (minutes < 60) "$minutes menit lagi" else "${minutes / 60} jam ${minutes % 60} menit lagi"
}

private fun openAutostartSettings(context: Context) {
    val miuiIntent = Intent().setComponent(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    )
    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(
        Uri.fromParts("package", context.packageName, null),
    )
    runCatching { context.startActivity(miuiIntent) }
        .getOrElse { context.startActivity(fallbackIntent) }
}

private fun requestBatteryExemption(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(
        Uri.parse("package:${context.packageName}"),
    )
    runCatching { context.startActivity(intent) }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}
