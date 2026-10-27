package com.repairdesk.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.repairdesk.app.data.Format
import com.repairdesk.app.data.RepairDatabase
import com.repairdesk.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The counter view: how much work is on the bench, what was delivered
 * today, how much money is still owed, which jobs are due soon, and
 * which are ready for collection. Nothing here edits data — it only
 * routes the user to the job that needs attention.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dao = RepairDatabase.get(requireContext()).jobDao()
        val dueSoonAdapter = JobAdapter { openJob(it.id) }
        val readyAdapter = JobAdapter { openJob(it.id) }
        binding.dueSoonList.adapter = dueSoonAdapter
        binding.readyList.adapter = readyAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        dao.inProgressCount(),
                        dao.deliveredTodayCount(
                            Format.startOfToday(), Format.endOfToday()
                        ),
                        dao.totalPendingPayments()
                    ) { inProgress, deliveredToday, pending ->
                        Triple(inProgress, deliveredToday, pending)
                    }.collect { (inProgress, deliveredToday, pending) ->
                        binding.inProgressValue.text = inProgress.toString()
                        binding.deliveredTodayValue.text = deliveredToday.toString()
                        binding.pendingPaymentsValue.text = Format.money(pending)
                    }
                }
                launch {
                    dao.dueSoonJobs(Format.daysFromNow(2)).collect { jobs ->
                        dueSoonAdapter.submitList(jobs)
                        binding.dueSoonEmpty.visibility =
                            if (jobs.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    dao.readyJobs().collect { jobs ->
                        readyAdapter.submitList(jobs)
                        binding.readyEmpty.visibility =
                            if (jobs.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun openJob(jobId: Long) {
        startActivity(
            Intent(requireContext(), JobDetailActivity::class.java)
                .putExtra(JobDetailActivity.EXTRA_JOB_ID, jobId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
