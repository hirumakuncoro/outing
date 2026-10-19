package id.my.ikhsanheriyawan.outing.data

import android.content.Context

object SetupPrefs {
    private const val PREFERENCES_NAME = "setup"
    private const val COMPLETED_IDS_KEY = "completedIds"

    fun completedIds(context: Context): Set<String> = preferences(context)
        .getStringSet(COMPLETED_IDS_KEY, emptySet())
        .orEmpty()
        .toSet()

    fun setCompleted(context: Context, id: String, completed: Boolean) {
        val ids = completedIds(context).toMutableSet()
        if (completed) ids += id else ids -= id
        preferences(context).edit().putStringSet(COMPLETED_IDS_KEY, ids).apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
