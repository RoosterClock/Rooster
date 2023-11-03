package com.rooster.rooster

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Date

class AlarmDbHelper(context: Context) : SQLiteOpenHelper(context, "alarm_db", null, 1) {

    private val alarmHandler = AlarmHandler()
    val context = context

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE alarms (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    label TEXT,
    mode TEXT,
    relative1 TEXT,
    relative2 TEXT,
    time1 INTEGER,
    time2 INTEGER,
    calculated_time INTEGER,
    enabled BOOLEAN,
    monday BOOLEAN,
    tuesday BOOLEAN,
    wednesday BOOLEAN,
    thursday BOOLEAN,
    friday BOOLEAN,
    saturday BOOLEAN,
    sunday BOOLEAN
);""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // TODO: Implement database migration if needed
    }

    fun insertAlarm(alarm: AlarmCreation) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("label", alarm.label)
            put("mode", alarm.mode)
            put("relative1", alarm.relative1)
            put("relative2", alarm.relative2)
            put("time1", alarm.time1)
            put("time2", alarm.time2)
            put("calculated_time", alarm.calculatedTime)
            put("enabled", alarm.enabled)
            put("monday", false)
            put("tuesday", false)
            put("wednesday", false)
            put("thursday", false)
            put("friday", false)
            put("saturday", false)
            put("sunday", false)
        }

        db.insert("alarms", null, values)
    }


    fun getAlarm(id: Long): Alarm? {
        val db = readableDatabase
        val cursor = db.query(
            "alarms",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            return Alarm(
                id = cursor.getLong(cursor.getColumnIndex("id")),
                label = cursor.getString(cursor.getColumnIndex("label")),
                mode = cursor.getString(cursor.getColumnIndex("mode")),
                relative1 = cursor.getString(cursor.getColumnIndex("relative1")),
                relative2 = cursor.getString(cursor.getColumnIndex("relative2")),
                time1 = cursor.getLong(cursor.getColumnIndex("time1")),
                time2 = cursor.getLong(cursor.getColumnIndex("time2")),
                calculatedTime = cursor.getLong(cursor.getColumnIndex("calculated_time")),
                enabled = cursor.getInt(cursor.getColumnIndex("enabled")) == 1,
                monday = cursor.getInt(cursor.getColumnIndex("monday")) == 1,
                tuesday = cursor.getInt(cursor.getColumnIndex("tuesday")) == 1,
                wednesday = cursor.getInt(cursor.getColumnIndex("wednesday")) == 1,
                thursday = cursor.getInt(cursor.getColumnIndex("thursday")) == 1,
                friday = cursor.getInt(cursor.getColumnIndex("friday")) == 1,
                saturday = cursor.getInt(cursor.getColumnIndex("saturday")) == 1,
                sunday = cursor.getInt(cursor.getColumnIndex("sunday")) == 1,
            )
        } else {
            return null
        }
    }


    fun updateAlarm(alarm: Alarm) {
        val db = writableDatabase
        alarm.calculatedTime = calculateTime(alarm)
        Log.e("TIME", alarm.time1.toString())
        Log.e("CALCULATED TIME", alarm.calculatedTime.toString())
        val values = ContentValues().apply {
            put("label", alarm.label)
            put("mode", alarm.mode)
            put("relative1", alarm.relative1)
            put("relative2", alarm.relative2)
            put("time1", alarm.time1)
            put("time2", alarm.time2)
            put("calculated_time", alarm.calculatedTime)
            put("enabled", alarm.enabled)
            put("monday", alarm.monday)
            put("tuesday", alarm.tuesday)
            put("wednesday", alarm.wednesday)
            put("thursday", alarm.thursday)
            put("friday", alarm.friday)
            put("saturday", alarm.saturday)
            put("sunday", alarm.sunday)
        }

        db.update("alarms", values, "id = ?", arrayOf(alarm.id.toString()))
        if (alarm.enabled) {
            alarmHandler.unsetAlarm(context, alarm)
            alarmHandler.setAlarm(context, alarm)
        } else {
            alarmHandler.unsetAlarm(context, alarm)
        }
    }

    private fun calculateTime(alarm: Alarm): Long {
        var calculatedTime = 0L
        if (alarm.mode == "At") {
            if (alarm.relative1 == "Pick Time") {
                calculatedTime = alarm.time1
                return calculatedTime
            } else {
                calculatedTime = getRelativeTime(alarm.relative1)
                return calculatedTime
            }
        } else if (alarm.mode == "Between") {
            var time1 = 0L
            var time2 = 0L
            if (alarm.relative1 == "Pick Time") {
                time1 = alarm.time1
            } else {
                time1 = getRelativeTime(alarm.relative1)
            }
            if (alarm.relative2 == "Pick Time") {
                time2 = alarm.time2
            } else {
                time2 = getRelativeTime(alarm.relative2)
            }
            calculatedTime = (time1 + time2) / 2
            return calculatedTime
        } else if (alarm.mode == "After") {
            var time1 = getRelativeTime(alarm.relative2)
            var time2 = alarm.time1
            calculatedTime = (time1 + time2)
            return calculatedTime
        } else if (alarm.mode == "Before") {
            var time1 = getRelativeTime(alarm.relative2)
            var time2 = alarm.time1
            calculatedTime = (time1 - time2)
            return calculatedTime
        }
        calculatedTime = addDays(alarm, calculatedTime)
        return calculatedTime
    }

    private fun addDays(alarm: Alarm, calculatedTime: Long): Long {
        val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        val weekdays = listOf(
            alarm.monday,
            alarm.tuesday,
            alarm.wednesday,
            alarm.thursday,
            alarm.friday,
            alarm.saturday,
            alarm.sunday
        )
        // Start searching from the current day and go up to 7 days (a full week)
        for (i in 0 until 7) {
            val dayToCheck = (currentDayOfWeek + i) % 7  // Ensure it wraps around the days of the week
            if (weekdays[dayToCheck]) {
                // Calculate the difference in days between the current day and the day with a true value
                val daysToAdd = (dayToCheck - currentDayOfWeek + 7) % 7
                // Calculate the time difference in milliseconds and add it to calculatedTime
                val millisecondsToAdd = daysToAdd * 24 * 60 * 60 * 1000
                return calculatedTime + millisecondsToAdd
            }
        }

        // If no true value is found in the next 7 days, return the original calculatedTime
        return calculatedTime
    }

    fun getRelativeTime(relative1: String): Long {
        val sharedPrefs = context.getSharedPreferences("rooster_prefs",
            AppCompatActivity.MODE_PRIVATE
        )
        var timeInMillis = 0L
        when (relative1) {
            "Astronomical Dawn" -> timeInMillis = sharedPrefs.getLong("astroDawn", 0)
            "Nautical Dawn" -> timeInMillis = sharedPrefs.getLong("nauticalDawn", 0)
            "Civil Dawn" -> timeInMillis = sharedPrefs.getLong("civilDawn", 0)
            "Sunrise" -> timeInMillis = sharedPrefs.getLong("sunrise", 0)
            "Sunset" -> timeInMillis = sharedPrefs.getLong("sunset", 0)
            "Civil Dusk" -> timeInMillis = sharedPrefs.getLong("civilDusk", 0)
            "Nautical Dusk" -> timeInMillis = sharedPrefs.getLong("nauticalDusk", 0)
            "Astronomical Dusk" -> timeInMillis = sharedPrefs.getLong("astroDusk", 0)
            "Solar Noon" -> timeInMillis = sharedPrefs.getLong("solarNoon", 0)
        }
         // Calculate the time difference in milliseconds between local time and GMT+0.
         val timeDifferenceMillis = TimeZone.getDefault().rawOffset - TimeZone.getTimeZone("GMT+0").rawOffset

         // Add the time difference to the local time to get GMT+0 time.
         val timeInMillisGMT = timeInMillis + timeDifferenceMillis
        return timeInMillisGMT / 1000
    }

    fun deleteAlarm(id: Long) {
        val db = writableDatabase
        db.delete("alarms", "id = ?", arrayOf(id.toString()))
        alarmHandler.unsetAlarmById(context, id)
    }

    fun getAllAlarms(): List<Alarm> {
        val db = readableDatabase
        val cursor = db.query("alarms", arrayOf("id", "label", "mode", "relative1", "relative2", "time1", "time2", "calculated_time", "enabled", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"), null, null, null, null, null)
        val alarms = mutableListOf<Alarm>()
        while (cursor.moveToNext()) {
            alarms.add(Alarm(
                id = cursor.getLong(cursor.getColumnIndex("id")),
                label = cursor.getString(cursor.getColumnIndex("label")),
                mode = cursor.getString(cursor.getColumnIndex("mode")),
                relative1 = cursor.getString(cursor.getColumnIndex("relative1")),
                relative2 = cursor.getString(cursor.getColumnIndex("relative2")),
                time1 = cursor.getLong(cursor.getColumnIndex("time1")),
                time2 = cursor.getLong(cursor.getColumnIndex("time2")),
                calculatedTime = cursor.getLong(cursor.getColumnIndex("calculated_time")),
                enabled = cursor.getInt(cursor.getColumnIndex("enabled")) == 1,
                monday = cursor.getInt(cursor.getColumnIndex("monday")) == 1,
                tuesday = cursor.getInt(cursor.getColumnIndex("tuesday")) == 1,
                wednesday = cursor.getInt(cursor.getColumnIndex("wednesday")) == 1,
                thursday = cursor.getInt(cursor.getColumnIndex("thursday")) == 1,
                friday = cursor.getInt(cursor.getColumnIndex("friday")) == 1,
                saturday = cursor.getInt(cursor.getColumnIndex("saturday")) == 1,
                sunday = cursor.getInt(cursor.getColumnIndex("sunday")) == 1,
            ))
        }
        cursor.close()
        return alarms
    }
}