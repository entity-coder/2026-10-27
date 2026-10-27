package com.repairdesk.app.data

/**
 * The repair pipeline. A job moves forward one step at a time; it can never
 * skip a stage, and Delivered is terminal. The PC version of RepairDesk uses
 * exactly this pipeline, so a shop moving between the two never relearns it.
 */
enum class JobStatus(val label: String) {
    RECEIVED("Received"),
    DIAGNOSING("Diagnosing"),
    IN_REPAIR("In Repair"),
    READY("Ready"),
    DELIVERED("Delivered");

    /** The next stage, or null when the job is already delivered. */
    fun next(): JobStatus? = when (this) {
        RECEIVED -> DIAGNOSING
        DIAGNOSING -> IN_REPAIR
        IN_REPAIR -> READY
        READY -> DELIVERED
        DELIVERED -> null
    }

    /** True while the job still occupies the workbench. */
    fun isInProgress(): Boolean = this != DELIVERED

    companion object {
        fun fromName(name: String): JobStatus =
            entries.firstOrNull { it.name == name } ?: RECEIVED
    }
}
