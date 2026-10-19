package id.my.ikhsanheriyawan.outing.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import id.my.ikhsanheriyawan.outing.MainActivity
import id.my.ikhsanheriyawan.outing.R

object Notifier {
    const val CHANNEL_ID = "jadwal"
    private const val CHANNEL_NAME = "Jadwal Outing"

    fun show(context: Context, intent: Intent) {
        createChannel(context)

        val requestCode = intent.getIntExtra(AlarmScheduler.EXTRA_REQUEST_CODE, 0)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(AlarmScheduler.EXTRA_TEXT).orEmpty()
        val big = intent.getStringExtra(AlarmScheduler.EXTRA_BIG).orEmpty()

        val openAppIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
