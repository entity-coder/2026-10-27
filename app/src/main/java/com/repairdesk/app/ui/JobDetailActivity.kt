package com.repairdesk.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.repairdesk.app.R
import com.repairdesk.app.data.Format
import com.repairdesk.app.data.JobWithPayments
import com.repairdesk.app.data.Payment
import com.repairdesk.app.data.RepairDatabase
import com.repairdesk.app.databinding.ActivityJobDetailBinding
import com.repairdesk.app.databinding.DialogPaymentBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Everything about one job: details, its pipeline stage, its payments,
 * and its receipt. Advancing the stage is one tap; moving to Delivered
 * stamps deliveredAt, which is what powers "delivered today" on the
 * dashboard. Payments can never exceed the amount still due.
 */
class JobDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_JOB_ID = "job_id"
    }

    private lateinit var binding: ActivityJobDetailBinding
    private var current: JobWithPayments? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val jobId = intent.getLongExtra(EXTRA_JOB_ID, -1L)
        if (jobId == -1L) {
            finish()
            return
        }
        val dao = RepairDatabase.get(this).jobDao()
        val paymentAdapter = PaymentAdapter()
        binding.paymentsList.adapter = paymentAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    dao.allJobs(),
                    dao.paymentsForJob(jobId)
                ) { jobs, payments ->
                    val job = jobs.firstOrNull { it.id == jobId } ?: return@combine null
                    JobWithPayments(job, payments)
                }.collect { jobWithPayments ->
                    if (jobWithPayments == null) {
                        // Job was deleted elsewhere; nothing left to show.
                        finish()
                        return@collect
                    }
                    current = jobWithPayments
                    render(jobWithPayments, paymentAdapter)
                }
            }
        }

        binding.advanceButton.setOnClickListener {
            val job = current?.job ?: return@setOnClickListener
            val next = job.statusEnum.next() ?: return@setOnClickListener
            lifecycleScope.launch {
                dao.updateJob(
                    job.copy(
                        status = next.name,
                        deliveredAt = if (next == com.repairdesk.app.data.JobStatus.DELIVERED) {
                            System.currentTimeMillis()
                        } else {
                            job.deliveredAt
                        }
                    )
                )
            }
        }

        binding.addPaymentButton.setOnClickListener { showPaymentDialog() }

        binding.deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_job_title)
                .setMessage(R.string.delete_job_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        dao.deleteJob(jobId)
                        finish()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.printButton.setOnClickListener {
            current?.let { ReceiptPdf.print(this, it) }
        }

        binding.shareButton.setOnClickListener {
            current?.let { ReceiptPdf.share(this, it) }
        }
    }

    private fun render(
        data: JobWithPayments,
        paymentAdapter: PaymentAdapter
    ) {
        val job = data.job
        binding.jobCode.text = job.jobCode
        binding.statusBadge.text = job.statusEnum.label
        binding.customerName.text = job.customerName
        binding.phone.text = job.phone
        binding.device.text = job.device
        binding.issue.text = job.issue
        binding.accessories.text = job.accessories.ifEmpty { "—" }
        binding.promisedDate.text = Format.date(job.promisedDate)
        binding.notes.text = job.notes.ifEmpty { "—" }
        binding.estimateValue.text = Format.money(job.estimate)
        binding.paidValue.text = Format.money(data.paidSoFar)
        binding.dueValue.text = Format.money(data.due)
        paymentAdapter.submitList(data.payments)

        val next = job.statusEnum.next()
        if (next == null) {
            binding.advanceButton.isEnabled = false
            binding.advanceButton.text = getString(R.string.job_delivered)
        } else {
            binding.advanceButton.isEnabled = true
            binding.advanceButton.text = getString(R.string.move_to, next.label)
        }
        binding.addPaymentButton.isEnabled = data.due > 0.0
    }

    /** Payment dialog: amount capped at the due amount, method picker. */
    private fun showPaymentDialog() {
        val data = current ?: return
        val dialogBinding = DialogPaymentBinding.inflate(layoutInflater)
        dialogBinding.methodSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Payment.METHODS
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.record_payment)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.record) { _, _ ->
                val amount =
                    dialogBinding.amountInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                if (amount <= 0.0) {
                    Toast.makeText(this, R.string.payment_positive, Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }
                if (amount > data.due) {
                    Toast.makeText(
                        this,
                        getString(R.string.payment_exceeds_due, Format.money(data.due)),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    RepairDatabase.get(this@JobDetailActivity).jobDao()
                        .insertPayment(
                            Payment(
                                jobId = data.job.id,
                                amount = amount,
                                method = dialogBinding.methodSpinner.selectedItem as String
                            )
                        )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
