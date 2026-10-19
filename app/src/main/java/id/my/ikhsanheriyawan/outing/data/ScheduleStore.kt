package id.my.ikhsanheriyawan.outing.data

import android.content.Context
import java.io.File
import java.io.IOException

object ScheduleStore {
    private const val FILE_NAME = "schedule.json"
    private const val TEMP_FILE_NAME = "schedule.json.tmp"

    fun read(context: Context): Schedule? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return ScheduleParser.parse(file.readText())
    }

    @Throws(IOException::class)
    fun replace(context: Context, content: String) {
        ScheduleParser.parse(content)

        val activeFile = File(context.filesDir, FILE_NAME)
        val tempFile = File(context.filesDir, TEMP_FILE_NAME)
        tempFile.outputStream().use { output ->
            output.write(content.toByteArray())
            output.fd.sync()
        }
        if (!tempFile.renameTo(activeFile)) {
            tempFile.delete()
            throw IOException("Gagal menyimpan jadwal baru.")
        }
    }
}
