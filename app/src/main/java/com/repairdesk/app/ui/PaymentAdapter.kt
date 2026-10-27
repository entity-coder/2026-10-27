package com.repairdesk.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repairdesk.app.data.Format
import com.repairdesk.app.data.Payment
import com.repairdesk.app.databinding.ItemPaymentBinding

/** One row per payment: amount, method, and date. Read-only by design. */
class PaymentAdapter :
    ListAdapter<Payment, PaymentAdapter.PaymentViewHolder>(PaymentDiff) {

    object PaymentDiff : DiffUtil.ItemCallback<Payment>() {
        override fun areItemsTheSame(oldItem: Payment, newItem: Payment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Payment, newItem: Payment): Boolean =
            oldItem == newItem
    }

    inner class PaymentViewHolder(private val binding: ItemPaymentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: Payment) {
            binding.paymentAmount.text = Format.money(payment.amount)
            binding.paymentMethod.text = payment.method
            binding.paymentDate.text = Format.date(payment.paidAt)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
