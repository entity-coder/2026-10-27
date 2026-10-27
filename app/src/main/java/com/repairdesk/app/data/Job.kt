package com.repairdesk.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One repair job card. Money is stored as rupees (Double) — the same choice
 * the PC version makes — because shop estimates are entered and printed in
 * rupees, not paise; see README trade-offs for the rounding caveat.
 *
 * Dates are epoch milliseconds (midnight local time for promisedDate).
 * deliveredAt is null until the job reaches Delivered.
 */
@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobCode: String,
    val customerName: String,
    val phone: String,
    val device: String,
    val issue: String,
    val accessories: String = "",
    val estimate: Double = 0.0,
    val promisedDate: Long = 0L,
    val notes: String = "",
    val status: String = JobStatus.RECEIVED.name,
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null
) {
    val statusEnum: JobStatus get() = JobStatus.fromName(status)

    /** Amount still owed: estimate minus everything paid so far, never negative. */
    fun dueAmount(paidSoFar: Double): Double =
        maxOf(0.0, estimate - paidSoFar)
}
