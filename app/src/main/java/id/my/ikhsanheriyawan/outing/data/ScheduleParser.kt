package id.my.ikhsanheriyawan.outing.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object ScheduleParser {
    private const val CURRENT_VERSION = 1

    fun parse(json: String): Schedule = try {
        parseRoot(JSONObject(json))
    } catch (error: JSONException) {
        throw ScheduleParseException("JSON jadwal tidak valid: ${error.message}")
    }

    private fun parseRoot(root: JSONObject): Schedule {
        if (!root.has("version")) {
            throw ScheduleParseException("Field version wajib ada.")
        }
        val version = requiredInt(root, "version", "version")
        if (version != CURRENT_VERSION) {
            throw ScheduleParseException("Version jadwal tidak didukung: $version.")
        }

        val events = parseEvents(root.optJSONArray("events") ?: JSONArray())
        val reminders = parseReminders(root.optJSONArray("reminders") ?: JSONArray())
        val checklist = parseChecklist(root.optJSONArray("checklist") ?: JSONArray())
        validateUniqueIds(events.map { it.id } + reminders.map { it.id } + checklist.map { it.id })

        return Schedule(
            defaultLeadMinutes = optionalInt(root, "defaultLeadMinutes", DEFAULT_LEAD_MINUTES, "defaultLeadMinutes"),
            events = events,
            reminders = reminders,
            checklist = checklist,
        )
    }

    private fun parseEvents(items: JSONArray): List<ScheduleEvent> = buildList {
        for (index in 0 until items.length()) {
            val item = objectAt(items, index, "events")
            val id = requiredString(item, "id", "events[$index].id")
            val start = requiredLong(item, "start", "events[$index].start")
            val end = optionalLong(item, "end", "events[$index].end")
            if (end != null && end < start) {
                throw ScheduleParseException("events[$index].end tidak boleh sebelum start.")
            }
            add(
                ScheduleEvent(
                    id = id,
                    start = start,
                    end = end,
                    title = requiredString(item, "title", "events[$index].title"),
                    location = optionalString(item, "location", "events[$index].location"),
                    dresscode = optionalString(item, "dresscode", "events[$index].dresscode"),
                    notes = optionalString(item, "notes", "events[$index].notes"),
                    leadMinutes = optionalIntOrNull(item, "leadMinutes", "events[$index].leadMinutes"),
                    notify = optionalBoolean(item, "notify", true, "events[$index].notify"),
                ),
            )
        }
    }

    private fun parseReminders(items: JSONArray): List<ScheduleReminder> = buildList {
        for (index in 0 until items.length()) {
            val item = objectAt(items, index, "reminders")
            add(
                ScheduleReminder(
                    id = requiredString(item, "id", "reminders[$index].id"),
                    at = requiredLong(item, "at", "reminders[$index].at"),
                    title = requiredString(item, "title", "reminders[$index].title"),
                    body = optionalString(item, "body", "reminders[$index].body"),
                ),
            )
        }
    }

    private fun parseChecklist(items: JSONArray): List<ChecklistItem> = buildList {
        for (index in 0 until items.length()) {
            val item = objectAt(items, index, "checklist")
            add(
                ChecklistItem(
                    id = requiredString(item, "id", "checklist[$index].id"),
                    group = requiredString(item, "group", "checklist[$index].group"),
                    text = requiredString(item, "text", "checklist[$index].text"),
                ),
            )
        }
    }

    private fun validateUniqueIds(ids: List<String>) {
        val duplicate = ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
        if (duplicate != null) {
            throw ScheduleParseException("ID duplikat: ${duplicate.key}.")
        }
    }

    private fun objectAt(items: JSONArray, index: Int, name: String): JSONObject =
        items.optJSONObject(index) ?: throw ScheduleParseException("$name[$index] harus berupa objek.")

    private fun requiredString(item: JSONObject, key: String, field: String): String {
        if (!item.has(key) || item.isNull(key)) {
            throw ScheduleParseException("Field $field wajib ada.")
        }
        val value = item.opt(key)
        if (value !is String || value.isBlank()) {
            throw ScheduleParseException("Field $field harus berupa teks yang tidak kosong.")
        }
        return value
    }

    private fun optionalString(item: JSONObject, key: String, field: String): String {
        if (!item.has(key) || item.isNull(key)) return ""
        val value = item.opt(key)
        if (value !is String) throw ScheduleParseException("Field $field harus berupa teks.")
        return value
    }

    private fun requiredLong(item: JSONObject, key: String, field: String): Long {
        if (!item.has(key) || item.isNull(key)) {
            throw ScheduleParseException("Field $field wajib ada.")
        }
        return numberAsLong(item.opt(key), field)
    }

    private fun optionalLong(item: JSONObject, key: String, field: String): Long? {
        if (!item.has(key) || item.isNull(key)) return null
        return numberAsLong(item.opt(key), field)
    }

    private fun requiredInt(item: JSONObject, key: String, field: String): Int {
        if (!item.has(key) || item.isNull(key)) {
            throw ScheduleParseException("Field $field wajib ada.")
        }
        return numberAsInt(item.opt(key), field)
    }

    private fun optionalInt(item: JSONObject, key: String, default: Int, field: String): Int {
        if (!item.has(key) || item.isNull(key)) return default
        return numberAsInt(item.opt(key), field)
    }

    private fun optionalIntOrNull(item: JSONObject, key: String, field: String): Int? {
        if (!item.has(key) || item.isNull(key)) return null
        return numberAsInt(item.opt(key), field)
    }

    private fun optionalBoolean(item: JSONObject, key: String, default: Boolean, field: String): Boolean {
        if (!item.has(key) || item.isNull(key)) return default
        val value = item.opt(key)
        if (value !is Boolean) throw ScheduleParseException("Field $field harus berupa true atau false.")
        return value
    }

    private fun numberAsLong(value: Any?, field: String): Long {
        if (value !is Number) throw ScheduleParseException("Field $field harus berupa angka.")
        val number = value.toDouble()
        if (!number.isFinite() || number != number.toLong().toDouble()) {
            throw ScheduleParseException("Field $field harus berupa angka bulat.")
        }
        return number.toLong()
    }

    private fun numberAsInt(value: Any?, field: String): Int {
        val number = numberAsLong(value, field)
        if (number !in Int.MIN_VALUE..Int.MAX_VALUE) {
            throw ScheduleParseException("Field $field di luar rentang angka bulat.")
        }
        return number.toInt()
    }
}
