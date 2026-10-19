package id.my.ikhsanheriyawan.outing.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleParserTest {
    @Test
    fun parsesValidScheduleAndIgnoresHumanLabels() {
        val schedule = ScheduleParser.parse(
            """
            {
              "version": 1,
              "events": [{
                "id": "event-1",
                "start": 1792715400000,
                "title": "Briefing",
                "_wib": "Jum 23 Okt 07:30"
              }],
              "reminders": [{
                "id": "reminder-1",
                "at": 1792711800000,
                "title": "Siap"
              }],
              "checklist": [{
                "id": "item-1",
                "group": "Tas",
                "text": "Air minum"
              }]
            }
            """.trimIndent(),
        )

        assertEquals(DEFAULT_LEAD_MINUTES, schedule.defaultLeadMinutes)
        assertEquals("Briefing", schedule.events.single().title)
        assertTrue(schedule.events.single().notify)
        assertEquals("", schedule.events.single().location)
        assertEquals("Siap", schedule.reminders.single().title)
        assertEquals("Tas", schedule.checklist.single().group)
    }

    @Test
    fun rejectsUnknownVersion() {
        assertParseFails("""{"version": 2}""", "tidak didukung")
    }

    @Test
    fun rejectsDuplicateIdsAcrossSchedule() {
        assertParseFails(
            """
            {
              "version": 1,
              "events": [{"id": "same", "start": 1, "title": "Event"}],
              "reminders": [{"id": "same", "at": 2, "title": "Reminder"}]
            }
            """.trimIndent(),
            "ID duplikat",
        )
    }

    @Test
    fun rejectsEndBeforeStart() {
        assertParseFails(
            """{"version": 1, "events": [{"id": "event", "start": 10, "end": 9, "title": "Event"}]}""",
            "tidak boleh sebelum start",
        )
    }

    @Test
    fun rejectsNonNumericStart() {
        assertParseFails(
            """{"version": 1, "events": [{"id": "event", "start": "pagi", "title": "Event"}]}""",
            "harus berupa angka",
        )
    }

    private fun assertParseFails(json: String, expectedMessage: String) {
        try {
            ScheduleParser.parse(json)
        } catch (error: ScheduleParseException) {
            assertTrue(error.message.orEmpty().contains(expectedMessage))
            return
        }
        assertFalse("Parser seharusnya menolak JSON", true)
    }
}
