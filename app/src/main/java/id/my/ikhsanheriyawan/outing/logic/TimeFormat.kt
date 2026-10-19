package id.my.ikhsanheriyawan.outing.logic

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private val hourFormatter = DateTimeFormatter.ofPattern("HH.mm", Locale("id", "ID"))

    fun hour(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(jakarta)
        .format(hourFormatter)

    fun range(startMillis: Long, endMillis: Long?): String = if (endMillis == null) {
        hour(startMillis)
    } else {
        "${hour(startMillis)}–${hour(endMillis)}"
    }
}
