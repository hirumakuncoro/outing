package id.my.ikhsanheriyawan.outing

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.my.ikhsanheriyawan.outing.alarm.AlarmScheduler
import id.my.ikhsanheriyawan.outing.data.ScheduleStore
import id.my.ikhsanheriyawan.outing.ui.theme.OutingGarutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmScheduler.rescheduleAll(this)
        setContent {
            OutingGarutTheme {
                ImportScreen(
                    onImportResult = ::importSchedule,
                    onScheduleTest = { AlarmScheduler.scheduleTest(this) },
                )
            }
        }
    }

    private fun importSchedule(uri: android.net.Uri) {
        val content = try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException("File tidak bisa dibaca.")
        } catch (error: Exception) {
            Toast.makeText(this, "Gagal membaca JSON: ${error.message}", Toast.LENGTH_LONG).show()
            return
        }

        try {
            ScheduleStore.replace(this, content)
            AlarmScheduler.rescheduleAll(this)
            val schedule = ScheduleStore.read(this) ?: error("Jadwal tidak tersimpan.")
            val count = schedule.events.size + schedule.reminders.size
            Toast.makeText(this, "Import berhasil: $count alarm diproses.", Toast.LENGTH_LONG).show()
        } catch (error: Exception) {
            Toast.makeText(this, "Import ditolak: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun ImportScreen(
    onImportResult: (android.net.Uri) -> Unit,
    onScheduleTest: () -> Unit,
) {
    var useMimeFallback by remember { mutableStateOf(false) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportResult(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Outing Garut",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            text = "Impor jadwal JSON untuk mulai menjadwalkan pengingat.",
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                importFile.launch(if (useMimeFallback) arrayOf("*/*") else arrayOf("application/json"))
            },
        ) {
            Text("Import JSON")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            onClick = onScheduleTest,
        ) {
            Text("Tes notifikasi 1 menit lagi")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            onClick = {
                useMimeFallback = true
                importFile.launch(arrayOf("*/*"))
            },
        ) {
            Text("Import jika pemilih file gagal")
        }
    }
}
