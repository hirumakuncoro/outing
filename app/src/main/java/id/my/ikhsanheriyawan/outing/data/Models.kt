package id.my.ikhsanheriyawan.outing.data

const val DEFAULT_LEAD_MINUTES = 10

data class Schedule(
    val defaultLeadMinutes: Int = DEFAULT_LEAD_MINUTES,
    val events: List<ScheduleEvent> = emptyList(),
    val reminders: List<ScheduleReminder> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
)

data class ScheduleEvent(
    val id: String,
    val start: Long,
    val end: Long? = null,
    val title: String,
    val location: String = "",
    val dresscode: String = "",
    val notes: String = "",
    val leadMinutes: Int? = null,
    val notify: Boolean = true,
)

data class ScheduleReminder(
    val id: String,
    val at: Long,
    val title: String,
    val body: String = "",
)

data class ChecklistItem(
    val id: String,
    val group: String,
    val text: String,
)

class ScheduleParseException(message: String) : IllegalArgumentException(message)
