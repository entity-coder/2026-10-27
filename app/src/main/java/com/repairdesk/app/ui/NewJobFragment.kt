package com.repairdesk.app.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.repairdesk.app.R
import com.repairdesk.app.data.Format
import com.repairdesk.app.data.Job
import com.repairdesk.app.data.RepairDatabase
import com.repairdesk.app.databinding.FragmentNewJobBinding
import java.util.Calendar
import kotlinx.coroutines.launch

/**
 * The intake form. Only name, phone, device, and issue are required —
 * everything else (accessories, estimate, promised date, notes) is
 * optional, because a customer standing at the counter should never be
 * blocked by a field the shop doesn't know yet.
 */
class NewJobFragment : Fragment() {

    private var _binding: FragmentNewJobBinding? = null
    private val binding get() = _binding!!

    private var promisedDate: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewJobBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.promisedDateInput.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    promisedDate = picked.timeInMillis
                    binding.promisedDateInput.setText(Format.date(promisedDate))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.saveButton.setOnClickListener {
            val name = binding.customerNameInput.text?.toString()?.trim().orEmpty()
            val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()
            val device = binding.deviceInput.text?.toString()?.trim().orEmpty()
            val issue = binding.issueInput.text?.toString()?.trim().orEmpty()
            if (name.isEmpty() || phone.isEmpty() || device.isEmpty() || issue.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    R.string.new_job_required_missing,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val estimate =
                binding.estimateInput.text?.toString()?.toDoubleOrNull() ?: 0.0

            val dao = RepairDatabase.get(requireContext()).jobDao()
            lifecycleScope.launch {
                // Job codes are sequential per shop (RD-0001, RD-0002, …)
                // and derived from the highest code already stored, so a
                // deleted job never causes a code to be reused.
                val code = Format.nextJobCode(dao.maxJobNumber())
                dao.insertJob(
                    Job(
                        jobCode = code,
                        customerName = name,
                        phone = phone,
                        device = device,
                        issue = issue,
                        accessories = binding.accessoriesInput.text?.toString()
                            ?.trim().orEmpty(),
                        estimate = estimate,
                        promisedDate = promisedDate,
                        notes = binding.notesInput.text?.toString()?.trim().orEmpty()
                    )
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.job_created, code),
                    Toast.LENGTH_SHORT
                ).show()
                (activity as? MainActivity)?.goToJobsTab()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
