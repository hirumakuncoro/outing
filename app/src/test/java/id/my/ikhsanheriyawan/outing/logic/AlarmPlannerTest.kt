package id.my.ikhsanheriyawan.outing.logic

import id.my.ikhsanheriyawan.outing.data.Schedule
import id.my.ikhsanheriyawan.outing.data.ScheduleEvent
import id.my.ikhsanheriyawan.outing.data.ScheduleReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPlannerTest {
    @Test
    fun schedulesEventAtLeadTime() {
        val event = ScheduleEvent(id = "event", start = 1_800_000L, title = "Briefing")

        val result = AlarmPlanner.plan(Schedule(events = listOf(event)), nowMillis = 1L)

        assertEquals(1_200_000L, result.single().triggerAtMillis)
        assertEquals("07.30 · Briefing", result.single().title)
    }

    @Test
    fun schedulesNearFutureEventAfterThreeSecondsWhenLeadTimePassed() {
        val event = ScheduleEvent(id = "event", start = 1_000_000L, title = "Briefing")

        val result = AlarmPlanner.plan(Schedule(events = listOf(event)), nowMillis = 500_000L)

        assertEquals(503_000L, result.single().triggerAtMillis)
    }

    @Test
    fun skipsPastEventAndReminder() {
        val schedule = Schedule(
            events = listOf(ScheduleEvent(id = "event", start = 500_000L, title = "Briefing")),
            reminders = listOf(ScheduleReminder(id = "reminder", at = 500_000L, title = "Siap")),
        )

        assertTrue(AlarmPlanner.plan(schedule, nowMillis = 600_000L).isEmpty())
    }

    @Test
    fun schedulesReminderAtExactTime() {
        val reminder = ScheduleReminder(id = "reminder", at = 1_000_000L, title = "Siap", body = "Bawa tas")

        val result = AlarmPlanner.plan(Schedule(reminders = listOf(reminder)), nowMillis = 1L)

        assertEquals(1_000_000L, result.single().triggerAtMillis)
        assertEquals("Siap", result.single().title)
        assertEquals("Bawa tas", result.single().text)
    }
}
