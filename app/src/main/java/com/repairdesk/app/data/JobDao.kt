package com.repairdesk.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A job together with all of its payments, for the detail screen and receipts. */
data class JobWithPayments(
    val job: Job,
    val payments: List<Payment>
) {
    val paidSoFar: Double get() = payments.sumOf { it.amount }
    val due: Double get() = job.dueAmount(paidSoFar)
}

@Dao
interface JobDao {

    @Insert
    suspend fun insertJob(job: Job): Long

    @Update
    suspend fun updateJob(job: Job)

    @Query("DELETE FROM jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: Long)

    /** Newest first — the Jobs tab is a work queue, not an archive. */
    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun allJobs(): Flow<List<Job>>

    /**
     * Search across the fields a customer actually quotes on the phone:
     * job code, name, phone number, or device. Empty query returns everything
     * (the caller passes "%" wrapped text, so blank input matches all rows).
     */
    @Query(
        "SELECT * FROM jobs WHERE jobCode LIKE :query OR customerName LIKE :query " +
            "OR phone LIKE :query OR device LIKE :query ORDER BY createdAt DESC"
    )
    fun searchJobs(query: String): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE status = :status ORDER BY createdAt DESC")
    fun jobsByStatus(status: String): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun jobById(jobId: Long): Job?

    @Query("SELECT * FROM payments WHERE jobId = :jobId ORDER BY paidAt ASC")
    fun paymentsForJob(jobId: Long): Flow<List<Payment>>

    @Transaction
    suspend fun jobWithPayments(jobId: Long): JobWithPayments? {
        val job = jobById(jobId) ?: return null
        val payments = paymentsListForJob(jobId)
        return JobWithPayments(job, payments)
    }

    @Query("SELECT * FROM payments WHERE jobId = :jobId ORDER BY paidAt ASC")
    suspend fun paymentsListForJob(jobId: Long): List<Payment>

    @Insert
    suspend fun insertPayment(payment: Payment)

    /** Highest numeric job-code suffix so far, for the next RD-0001 code. */
    @Query("SELECT MAX(CAST(SUBSTR(jobCode, 4) AS INTEGER)) FROM jobs")
    suspend fun maxJobNumber(): Int?

    /** Jobs still on the bench, for the dashboard. */
    @Query("SELECT COUNT(*) FROM jobs WHERE status != 'DELIVERED'")
    fun inProgressCount(): Flow<Int>

    /** Delivered today (local midnight to midnight), for the dashboard. */
    @Query(
        "SELECT COUNT(*) FROM jobs WHERE status = 'DELIVERED' " +
            "AND deliveredAt >= :dayStart AND deliveredAt < :dayEnd"
    )
    fun deliveredTodayCount(dayStart: Long, dayEnd: Long): Flow<Int>

    /** Everything still owed across all open jobs. */
    @Query(
        "SELECT COALESCE(SUM(j.estimate - COALESCE(p.paid, 0)), 0) FROM jobs j " +
            "LEFT JOIN (SELECT jobId, SUM(amount) AS paid FROM payments GROUP BY jobId) p " +
            "ON p.jobId = j.id WHERE j.status != 'DELIVERED'"
    )
    fun totalPendingPayments(): Flow<Double>

    /** Open jobs promised within the next [until] millis — the "due soon" strip. */
    @Query(
        "SELECT * FROM jobs WHERE status != 'DELIVERED' AND promisedDate > 0 " +
            "AND promisedDate <= :until ORDER BY promisedDate ASC"
    )
    fun dueSoonJobs(until: Long): Flow<List<Job>>

    /** Open jobs in Ready status, waiting for the customer to collect. */
    @Query("SELECT * FROM jobs WHERE status = 'READY' ORDER BY promisedDate ASC")
    fun readyJobs(): Flow<List<Job>>
}
