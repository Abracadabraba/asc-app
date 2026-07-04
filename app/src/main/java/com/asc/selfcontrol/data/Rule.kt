package com.asc.selfcontrol.data

/**
 * A time-lock rule for a single app.
 * startMinute / endMinute are minutes-since-midnight (0..1439).
 * If startMinute < endMinute: locked during [start, end) same day.
 * If startMinute > endMinute: locked overnight, e.g. 22:00 -> 07:00 next day.
 */
data class Rule(
    val packageName: String,
    val label: String,
    val startMinute: Int,
    val endMinute: Int
) {
    fun isLockedAt(nowMinute: Int): Boolean {
        return if (startMinute == endMinute) {
            false
        } else if (startMinute < endMinute) {
            nowMinute in startMinute until endMinute
        } else {
            nowMinute >= startMinute || nowMinute < endMinute
        }
    }

    fun toJson(): org.json.JSONObject {
        val o = org.json.JSONObject()
        o.put("packageName", packageName)
        o.put("label", label)
        o.put("startMinute", startMinute)
        o.put("endMinute", endMinute)
        return o
    }

    companion object {
        fun fromJson(o: org.json.JSONObject): Rule {
            return Rule(
                packageName = o.getString("packageName"),
                label = o.optString("label", o.getString("packageName")),
                startMinute = o.getInt("startMinute"),
                endMinute = o.getInt("endMinute")
            )
        }
    }
}
