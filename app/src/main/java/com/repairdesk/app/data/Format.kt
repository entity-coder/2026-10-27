package com.repairdesk.app.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Shared formatting so every screen prints money and dates the same way. */
object Format {

    private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun money(amount: Double): String = inr.format(amount)

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    fun date(millis: Long): String =
        if (millis <= 0L) "—" else dateFormat.format(Date(millis))

    /** Start of today (local midnight), for "delivered today" queries. */
    fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun endOfToday(): Long = startOfToday() + 24L * 60 * 60 * 1000

    /** Midnight [days] from now, for the "due soon" window. */
    fun daysFromNow(days: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }

    /** Next job code: one more than the highest RD-#### already in the DB. */
    fun nextJobCode(maxNumber: Int?): String =
        "RD-%04d".format((maxNumber ?: 0) + 1)
}
