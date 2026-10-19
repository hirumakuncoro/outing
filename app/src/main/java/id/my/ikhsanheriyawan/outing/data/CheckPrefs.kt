package id.my.ikhsanheriyawan.outing.data

import android.content.Context

object CheckPrefs {
    private const val PREFERENCES_NAME = "check"
    private const val CHECKED_IDS_KEY = "checkedIds"

    fun checkedIds(context: Context): Set<String> = preferences(context)
        .getStringSet(CHECKED_IDS_KEY, emptySet())
        .orEmpty()
        .toSet()

    fun setChecked(context: Context, id: String, checked: Boolean) {
        val ids = checkedIds(context).toMutableSet()
        if (checked) ids += id else ids -= id
        preferences(context).edit().putStringSet(CHECKED_IDS_KEY, ids).apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
