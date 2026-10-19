package id.my.ikhsanheriyawan.outing.logic

import id.my.ikhsanheriyawan.outing.data.Schedule
import id.my.ikhsanheriyawan.outing.data.ScheduleEvent
import id.my.ikhsanheriyawan.outing.data.ScheduleReminder

private const val MILLIS_PER_MINUTE = 60_000L
const val PAST_EVENT_TRIGGER_DELAY_MILLIS = 3_000L

data class AlarmPayload(
    val id: String,
    val triggerAtMillis: Long,
    val title: String,
    val text: String,
    val big: String,
    val whenMillis: Long,
)

object AlarmPlanner {
    fun plan(schedule: Schedule, nowMillis: Long): List<AlarmPayload> = buildList {
        schedule.events
            .asSequence()
            .filter { it.notify }
            .mapNotNull { eventPayload(it, schedule.defaultLeadMinutes, nowMillis) }
            .forEach(::add)
        schedule.reminders
            .asSequence()
            .mapNotNull { reminderPayload(it, nowMillis) }
            .forEach(::add)
    }

    fun testPayload(triggerAtMillis: Long): AlarmPayload = AlarmPayload(
        id = "test-$triggerAtMillis",
        triggerAtMillis = triggerAtMillis,
        title = "Tes notifikasi",
        text = "Notifikasi alarm berhasil dijadwalkan.",
        big = "Tes notifikasi 1 menit dari aplikasi Outing Garut.",
        whenMillis = triggerAtMillis,
    )

    private fun eventPayload(
        event: ScheduleEvent,
        defaultLeadMinutes: Int,
        nowMillis: Long,
    ): AlarmPayload? {
        val triggerAtMillis = event.start - (event.leadMinutes ?: defaultLeadMinutes) * MILLIS_PER_MINUTE
        val scheduledAtMillis = when {
            triggerAtMillis >= nowMillis -> triggerAtMillis
            event.start > nowMillis -> nowMillis + PAST_EVENT_TRIGGER_DELAY_MILLIS
            else -> return null
        }
        return AlarmPayload(
            id = event.id,
            triggerAtMillis = scheduledAtMillis,
            title = "${TimeFormat.hour(event.start)} · ${event.title}",
            text = eventSummary(event),
            big = eventDetails(event),
            whenMillis = event.start,
        )
    }

    private fun reminderPayload(reminder: ScheduleReminder, nowMillis: Long): AlarmPayload? {
        if (reminder.at < nowMillis) return null
        return AlarmPayload(
            id = reminder.id,
            triggerAtMillis = reminder.at,
            title = reminder.title,
            text = reminder.body,
            big = reminder.body,
            whenMillis = reminder.at,
        )
    }

    private fun eventSummary(event: ScheduleEvent): String {
        val detail = listOf(event.location, event.dresscode).filter { it.isNotBlank() }.joinToString(" · ")
        return detail.ifBlank { "Mulai ${TimeFormat.hour(event.start)}" }
    }

    private fun eventDetails(event: ScheduleEvent): String = buildList {
        if (event.location.isNotBlank()) add("📍 ${event.location}")
        if (event.dresscode.isNotBlank()) add("👕 ${event.dresscode}")
        add("🕐 ${TimeFormat.range(event.start, event.end)}")
        if (event.notes.isNotBlank()) add(event.notes)
    }.joinToString("\n")
}
