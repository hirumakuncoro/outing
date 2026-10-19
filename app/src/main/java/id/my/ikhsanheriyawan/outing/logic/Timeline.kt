package id.my.ikhsanheriyawan.outing.logic

import id.my.ikhsanheriyawan.outing.data.Schedule
import id.my.ikhsanheriyawan.outing.data.ScheduleEvent
import id.my.ikhsanheriyawan.outing.data.ScheduleReminder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface TimelineItem {
    val id: String
    val at: Long
    val title: String

    data class Event(val event: ScheduleEvent) : TimelineItem {
        override val id = event.id
        override val at = event.start
        override val title = event.title
    }

    data class Reminder(val reminder: ScheduleReminder) : TimelineItem {
        override val id = reminder.id
        override val at = reminder.at
        override val title = reminder.title
    }
}

object Timeline {
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale("id", "ID"))

    fun items(schedule: Schedule): List<TimelineItem> = buildList {
        schedule.events.forEach { add(TimelineItem.Event(it)) }
        schedule.reminders.forEach { add(TimelineItem.Reminder(it)) }
    }.sortedBy(TimelineItem::at)

    fun currentEvent(schedule: Schedule, nowMillis: Long): ScheduleEvent? = schedule.events
        .filter { event ->
            event.start <= nowMillis && (event.end ?: event.start) > nowMillis
        }
        .maxByOrNull(ScheduleEvent::start)

    fun nextItem(schedule: Schedule, nowMillis: Long): TimelineItem? = items(schedule)
        .firstOrNull { it.at > nowMillis }

    fun dateLabel(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(jakarta)
        .format(dateFormatter)

    fun groupedByDay(schedule: Schedule): Map<String, List<TimelineItem>> = items(schedule)
        .groupBy { item -> dateLabel(item.at) }
}
