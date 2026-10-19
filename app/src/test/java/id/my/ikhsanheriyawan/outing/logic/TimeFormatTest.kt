package id.my.ikhsanheriyawan.outing.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun formatsUtcMidnightAsWibMorning() {
        assertEquals("07.00", TimeFormat.hour(0L))
    }

    @Test
    fun formatsRangeWithDotsAndWibZone() {
        assertEquals("06.30–08.00", TimeFormat.range(84_600_000L, 90_000_000L))
    }
}
