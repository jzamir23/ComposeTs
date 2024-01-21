@file:Suppress("unused")
@file:OptIn(ExperimentalTime::class)

package com.v2project.mqtt.ut

import com.soywiz.klock.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

typealias  DateTime = LocalDateTime
typealias  KLockTime = com.soywiz.klock.DateTime

fun nowInstant() = Clock.System.now()

fun nowDateTime() = nowInstant().toDateTime()

fun nowUnixMillis() = Clock.System.now().toEpochMilliseconds()

fun Instant.toDateTime(): DateTime {
    return toLocalDateTime(currentSystemDefault())
}

operator fun Instant.plus(period: DateTimePeriod): Instant {
    return plus(period, currentSystemDefault())
}

fun Instant.minus(period: DateTimePeriod): Instant {
    return minus(period, currentSystemDefault())
}

fun Instant.toUnix() = toEpochMilliseconds()

fun instant(unixMillis: Long) = Instant.fromEpochMilliseconds(unixMillis)

fun DateTime.toInstant() = toInstant(currentSystemDefault())

fun DateTime.unixMillis() = toInstant().toEpochMilliseconds()

fun DateTime.month0() = monthNumber - 1

fun DateTime.month1() = monthNumber

fun DateTime.copyOf(
    year: Int = this.year,
    monthNumber: Int = this.monthNumber,
    dayOfMonth: Int = this.dayOfMonth,
    hour: Int = this.hour,
    minute: Int = this.minute,
    second: Int = this.second,
    nanosecond: Int = this.nanosecond,
): DateTime {
    return DateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
}

fun dateTime(time: Long) = Instant.fromEpochMilliseconds(time).toDateTime()

fun DateTime.startOfYear() = DateTime(year, 1, 1, 0, 0, 0, 0)

fun DateTime.startOfMonth() = DateTime(year, month, 1, 0, 0, 0, 0)

fun DateTime.startOfDay() = DateTime(year, month, dayOfMonth, 0, 0, 0, 0)

fun DateTime.startOfHour() = DateTime(year, month, dayOfMonth, hour, 0, 0, 0)

fun DateTime.startOfMinute() = DateTime(year, month, dayOfMonth, hour, minute, 0, 0)

fun DateTime.startOfSecond() = DateTime(year, month, dayOfMonth, hour, minute, second, 0)

fun DateTime.endOfYear() = DateTime(year, 12, 31, 23, 59, 59, 999999999)

fun DateTime.endOfDay() = DateTime(year, month, dayOfMonth, 23, 59, 59, 999999999)

fun DateTime.endOfHour() = DateTime(year, month, dayOfMonth, hour, 59, 59, 999999999)

fun DateTime.endOfMinute() = DateTime(year, month, dayOfMonth, hour, minute, 59, 999999999)

fun DateTime.endOfSecond() = DateTime(year, month, dayOfMonth, hour, minute, second, 999999999)

operator fun DateTime.plus(duration: Duration): DateTime {
    return (this.toInstant() + duration).toDateTime()
}

operator fun DateTime.minus(duration: Duration): DateTime {
    return (this.toInstant() - duration).toDateTime()
}

fun DateTime.nextYears(amount: Int = 1): DateTime {
    return dateTime((KLockTime(this.unixMillis()).local.local + amount.years).localUnadjusted.utc.unixMillisLong)
}

fun DateTime.nextMonths(amount: Int = 1): DateTime {
    return dateTime((KLockTime(this.unixMillis()).local.local + amount.months).localUnadjusted.utc.unixMillisLong)
}

fun DateTime.nextWeeks(amount: Int = 1): DateTime {
    return dateTime((KLockTime(this.unixMillis()).local.local + amount.weeks).localUnadjusted.utc.unixMillisLong)
}

fun DateTime.nextDays(amount: Int = 1): DateTime {
    return this + amount.days
}

fun DateTime.nextHours(amount: Int = 1): DateTime {
    return this + amount.hours
}

fun DateTime.nextMinutes(amount: Int = 1): DateTime {
    return this + amount.minutes
}

fun DateTime.nextSeconds(amount: Int): DateTime {
    return this + amount.seconds
}

fun DateTime.weekIndex0Monday(): Int {
    return KLockTime(this.unixMillis()).local.local.dayOfWeek.index0Monday
}

fun DateTime.weekIndex1Monday(): Int {
    return KLockTime(this.unixMillis()).local.local.dayOfWeek.index1Monday
}

fun DateTime.weekIndex0Sunday(): Int {
    return KLockTime(this.unixMillis()).local.local.dayOfWeek.index0Sunday
}

fun DateTime.weekIndex1Sunday(): Int {
    return KLockTime(this.unixMillis()).local.local.dayOfWeek.index1Sunday
}

/**
 * 将时间转换为指定字符串格式
 */
fun date2String(date: DateTime?, format: String): String {
    try {
        return date?.let { DateFormat(format).format(it.toKLockTime().utc) } ?: ""
    } catch (e: DateException) {
        e.printStackTrace()
    }
    return ""
}

fun date2String(date: DateTime?): String {
    return date?.toString() ?: ""
}

fun date2String(unixMillis: Long, format: String): String {
    return date2String(dateTime(unixMillis), format)
}

fun date2String(unixMillis: Long): String {
    return date2String(dateTime(unixMillis))
}

/**
 * 将时间格式字符串转为DateTime时间类型
 */
fun string2Date(dateString: String?, format: String): DateTime {
    try {
        return if (dateString.isNullOrEmpty()) {
            dateTime(0)
        } else {
            dateString.let { DateFormat(format).parse(it).utc.toDateTime() }
        }
    } catch (e: DateException) {
        e.printStackTrace()
    }
    return dateTime(0)
}

fun string2Date(dateString: String?): DateTime {
    return if (dateString.isNullOrEmpty()) {
        dateTime(0)
    } else {
        if (dateString.contains("T", true)) {
            dateString.toLocalDateTime()
        } else {
            string2Date(dateString, "yyyy-MM-dd HH:mm:ss.SSS")
        }
    }
}

fun DateTime.toKLockTime(): KLockTime {
    return KLockTime(unixMillis()).local.local
}

fun KLockTime.toDateTime(): DateTime {
    return dateTime(this.localUnadjusted.utc.unixMillisLong)
}
