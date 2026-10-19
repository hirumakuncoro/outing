package id.my.ikhsanheriyawan.outing.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import id.my.ikhsanheriyawan.outing.MainActivity
import id.my.ikhsanheriyawan.outing.data.ScheduleStore
import id.my.ikhsanheriyawan.outing.logic.AlarmPayload
import id.my.ikhsanheriyawan.outing.logic.AlarmPlanner

object AlarmScheduler {
    const val EXTRA_REQUEST_CODE = "requestCode"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_BIG = "big"
    const val EXTRA_WHEN_MILLIS = "whenMillis"

    private const val PREFERENCES_NAME = "alarms"
    private const val REQUEST_CODES_KEY = "requestCodes"

    fun rescheduleAll(context: Context) {
        cancelAll(context)
        val schedule = runCatching { ScheduleStore.read(context) }.getOrNull() ?: return
        AlarmPlanner.plan(schedule, System.currentTimeMillis()).forEach { schedule(context, it) }
    }

    fun scheduleTest(context: Context, triggerAtMillis: Long = System.currentTimeMillis() + 60_000L) {
        schedule(context, AlarmPlanner.testPayload(triggerAtMillis))
    }

    private fun cancelAll(context: Context) {
        val preferences = preferences(context)
        preferences.getStringSet(REQUEST_CODES_KEY, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .forEach { requestCode ->
                alarmManager(context).cancel(receiverPendingIntent(context, requestCode))
            }
        preferences.edit().remove(REQUEST_CODES_KEY).apply()
    }

    private fun schedule(context: Context, payload: AlarmPayload) {
        val requestCode = payload.id.hashCode()
        val operation = receiverPendingIntent(context, requestCode, payload)
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(payload.triggerAtMillis, showIntent)
        alarmManager(context).setAlarmClock(alarmClockInfo, operation)
        remember(context, requestCode)
    }

    private fun receiverPendingIntent(
        context: Context,
        requestCode: Int,
        payload: AlarmPayload? = null,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        if (payload != null) {
            intent.putExtra(EXTRA_REQUEST_CODE, requestCode)
            intent.putExtra(EXTRA_ID, payload.id)
            intent.putExtra(EXTRA_TITLE, payload.title)
            intent.putExtra(EXTRA_TEXT, payload.text)
            intent.putExtra(EXTRA_BIG, payload.big)
            intent.putExtra(EXTRA_WHEN_MILLIS, payload.whenMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun remember(context: Context, requestCode: Int) {
        val preferences = preferences(context)
        val requestCodes = preferences.getStringSet(REQUEST_CODES_KEY, emptySet()).orEmpty().toMutableSet()
        requestCodes += requestCode.toString()
        preferences.edit().putStringSet(REQUEST_CODES_KEY, requestCodes).apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
