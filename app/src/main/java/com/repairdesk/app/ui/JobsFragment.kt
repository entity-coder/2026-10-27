package com.repairdesk.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.repairdesk.app.R
import com.repairdesk.app.data.JobStatus
import com.repairdesk.app.data.RepairDatabase
import com.repairdesk.app.databinding.FragmentJobsBinding
import kotlinx.coroutines.launch

/**
 * The work queue: every job, newest first, searchable by code, name,
 * phone, or device, and filterable by pipeline status. Filtering happens
 * in memory over the full list — a repair shop has hundreds of jobs, not
 * millions, so one Flow plus local filtering is simpler and stays instant.
 */
class JobsFragment : Fragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!

    private var statusFilter: JobStatus? = null
    private var query: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dao = RepairDatabase.get(requireContext()).jobDao()
        val adapter = JobAdapter { job ->
            startActivity(
                Intent(requireContext(), JobDetailActivity::class.java)
                    .putExtra(JobDetailActivity.EXTRA_JOB_ID, job.id)
            )
        }
        binding.jobsList.adapter = adapter

        // Status filter chips: All + one per pipeline stage.
        val chipGroup = binding.statusChips
        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.filter_all)
            isCheckable = true
            isChecked = true
        }
        chipGroup.addView(allChip)
        JobStatus.entries.forEach { status ->
            chipGroup.addView(Chip(requireContext()).apply {
                text = status.label
                isCheckable = true
                tag = status
            })
        }
        chipGroup.setOnCheckedStateChangeListener { _, _ ->
            val checked = chipGroup.checkedChipId
            val chip = chipGroup.findViewById<Chip>(checked)
            statusFilter = chip?.tag as? JobStatus
            applyFilter(adapter)
        }

        binding.searchInput.doAfterTextChanged { text ->
            query = text?.toString()?.trim().orEmpty()
            applyFilter(adapter)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.allJobs().collect { jobs ->
                    // Keep the unfiltered list as the source of truth so
                    // typing and chip taps never need a second query.
                    allJobs = jobs
                    applyFilter(adapter)
                }
            }
        }
    }

    private var allJobs = emptyList<com.repairdesk.app.data.Job>()

    private fun applyFilter(adapter: JobAdapter) {
        val q = query.lowercase()
        val filtered = allJobs.filter { job ->
            val matchesStatus = statusFilter == null || job.statusEnum == statusFilter
            val matchesQuery = q.isEmpty() ||
                job.jobCode.lowercase().contains(q) ||
                job.customerName.lowercase().contains(q) ||
                job.phone.contains(q) ||
                job.device.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
        adapter.submitList(filtered)
        binding.emptyView.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
