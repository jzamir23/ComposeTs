package com.v2project.mqtt.ut

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    private const val defaultFormat = "yyyy-MM-dd HH:mm:ss.SSS"

    fun parse(date: String?, format: String = defaultFormat): DateTime {
        if (!date.isNullOrEmpty()) {
            try {
                return dateTime(SimpleDateFormat(format, Locale.CHINA).parse(date)?.time ?: 0)
            } catch (e: ParseException) {
                Log.w("DateFormatter", "$format data format not match -> ")
            }
        }
        return dateTime(0)
    }

    fun format(date: DateTime?, format: String = defaultFormat): String {
        return if (date == null || date.unixMillis() == 0L) {
            ""
        } else {
            SimpleDateFormat(format, Locale.CHINA).format(Date(date.unixMillis()))
        }
    }

    fun format(unixMillis: Long, format: String = defaultFormat): String {
        return SimpleDateFormat(format, Locale.CHINA).format(Date(unixMillis))
    }
}