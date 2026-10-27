package com.repairdesk.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repairdesk.app.data.Format
import com.repairdesk.app.data.Job
import com.repairdesk.app.databinding.ItemJobBinding

/**
 * One row per job card: code + status badge, customer, device, issue,
 * promised date. Tapping opens the detail screen; the row itself never
 * mutates data, so list updates stay predictable.
 */
class JobAdapter(
    private val onClick: (Job) -> Unit
) : ListAdapter<Job, JobAdapter.JobViewHolder>(JobDiff) {

    object JobDiff : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean =
            oldItem == newItem
    }

    inner class JobViewHolder(private val binding: ItemJobBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(job: Job) {
            binding.jobCode.text = job.jobCode
            binding.statusBadge.text = job.statusEnum.label
            binding.customerName.text = job.customerName
            binding.deviceLine.text = "${job.device} · ${job.phone}"
            binding.issueLine.text = job.issue
            binding.promisedLine.text = "Promised: ${Format.date(job.promisedDate)}"
            binding.root.setOnClickListener { onClick(job) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
