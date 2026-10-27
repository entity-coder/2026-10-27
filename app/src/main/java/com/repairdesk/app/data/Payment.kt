package com.repairdesk.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One payment against a job. Payments are append-only — there is no edit or
 * delete — so the money trail for a job can always be audited. A payment can
 * never exceed the amount still due; the UI enforces that cap.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Job::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("jobId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val amount: Double,
    val method: String = "Cash",
    val paidAt: Long = System.currentTimeMillis()
) {
    companion object {
        val METHODS = listOf("Cash", "UPI", "Card", "Bank")
    }
}
