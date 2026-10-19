package id.my.ikhsanheriyawan.outing.logic

import id.my.ikhsanheriyawan.outing.data.Schedule
import id.my.ikhsanheriyawan.outing.data.ScheduleEvent
import id.my.ikhsanheriyawan.outing.data.ScheduleReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineTest {
    private val event1 = ScheduleEvent(
        id = "e1",
        start = 1_000_000L,
        end = 2_000_000L,
        title = "Sesi 1",
    )
    private val event2 = ScheduleEvent(
        id = "e2",
        start = 3_000_000L,
        end = 4_000_000L,
        title = "Sesi 2",
    )
    private val reminder = ScheduleReminder(
        id = "r1",
        at = 2_500_000L,
        title = "Jeda",
    )
    private val schedule = Schedule(events = listOf(event1, event2), reminders = listOf(reminder))

    @Test
    fun currentEventInsideRange() {
        val current = Timeline.currentEvent(schedule, nowMillis = 1_500_000L)
        assertEquals("e1", current?.id)
    }

    @Test
    fun currentEventBeforeAnyRange() {
        assertNull(Timeline.currentEvent(schedule, nowMillis = 500_000L))
    }

    @Test
    fun currentEventBetweenEvents() {
        assertNull(Timeline.currentEvent(schedule, nowMillis = 2_200_000L))
    }

    @Test
    fun currentEventAfterAllEvents() {
        assertNull(Timeline.currentEvent(schedule, nowMillis = 5_000_000L))
    }

    @Test
    fun nextItemBeforeAnyEvent() {
        val next = Timeline.nextItem(schedule, nowMillis = 500_000L)
        assertEquals("e1", next?.id)
    }

    @Test
    fun nextItemBetweenEventsPicksReminder() {
        val next = Timeline.nextItem(schedule, nowMillis = 2_100_000L)
        assertEquals("r1", next?.id)
    }

    @Test
    fun nextItemAfterReminderPicksSecondEvent() {
        val next = Timeline.nextItem(schedule, nowMillis = 2_600_000L)
        assertEquals("e2", next?.id)
    }

    @Test
    fun nextItemAfterAllEvents() {
        assertNull(Timeline.nextItem(schedule, nowMillis = 5_000_000L))
    }

    @Test
    fun itemsAreSortedByTime() {
        val items = Timeline.items(schedule)
        assertEquals(listOf("e1", "r1", "e2"), items.map(TimelineItem::id))
    }
}
