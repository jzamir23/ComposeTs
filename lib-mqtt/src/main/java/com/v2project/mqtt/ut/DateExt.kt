@file:Suppress("unused")

package com.v2project.mqtt.ut

import kotlin.time.Duration.Companion.minutes

@Suppress("EnumEntryName")
enum class EnumDateFormat(val format: String) {
    TIME_TAG("yyyyMMddHHmmss"),
    DATE_TIME("yyyy-MM-dd HH:mm:ss.SSS"),
    DATE_TIME_S("yyyy-MM-dd HH:mm:ss"),
    yyyy("yyyy"),
    yyyyMM("yyyyMM"),
    yyyyMMdd("yyyyMMdd"),
    yyyyMMddHH("yyyyMMddHH"),
    yyyyMMddHHmmss("yyyyMMddHHmmss"),
    yyyy_MM_dd_HH_mm_ss_SSS("yyyy-MM-dd HH:mm:ss.SSS"),
    yyyy_MM_dd_HH_mm_ss("yyyy-MM-dd HH:mm:ss"),
    yyyy_MM_dd_HH_mm("yyyy-MM-dd HH:mm"),
    yyyy_MM_dd_HH("yyyy-MM-dd HH:00:00"),
    yyyy_MM_dd("yyyy-MM-dd"),
    yyyy_MM("yyyy-MM"),
    MM_dd_HH_mm("MM-dd HH:mm"),
    MM_dd_HH("MM-dd HH"),
    MM_dd("MM-dd"),
    HH("HH"),
    HH_mm("HH:mm"),
    HH_mm_ss("HH:mm:ss"),
    mm_ss("mm:ss"),
}

fun DateTime.timeTag(interval: Int = 15): DateTime {
    return this.startOfMinute().copyOf(minute = this.minute / interval * interval)
}

fun DateTime.ceilTimeTag(interval: Int = 15): DateTime {
    return if (this.minute % interval == 0) this.timeTag() else this.nextTimeTag()
}

/**
 * 获取指定时间标签上n个时间标签， 默认为1个时间标签
 */
fun DateTime.prevTimeTag(amount: Int = 1): DateTime {
    return (this - (amount * 15).minutes).timeTag()
}

/**
 * 获取指定时间标签下n个时间标签， 默认为1个时间标签
 */
fun DateTime.nextTimeTag(amount: Int = 1): DateTime {
    return (this + (amount * 15).minutes).timeTag()
}

fun DateTime.isToday() = this.startOfDay() == nowDateTime().startOfDay()

fun DateTime.isCurrentMonth() = this.startOfMonth() == nowDateTime().startOfMonth()

fun DateTime.isStartOfDay() = this == this.startOfDay()

fun DateTime.format(dateFormat: EnumDateFormat): String {
    return DateFormatter.format(this, dateFormat.format)
}

fun String.parse(dateFormat: EnumDateFormat): DateTime {
    return DateFormatter.parse(this, dateFormat.format)
}