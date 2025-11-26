package com.momoterminal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.momoterminal.R
import com.momoterminal.data.TransactionEntity
import java.util.concurrent.TimeUnit

/**
 * RecyclerView adapter for displaying transaction history.
 * Uses DiffUtil for efficient updates.
 */
class TransactionAdapter : ListAdapter<TransactionEntity, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgStatus: ImageView = itemView.findViewById(R.id.imgStatus)
        private val tvSender: TextView = itemView.findViewById(R.id.tvSender)
        private val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        
        fun bind(transaction: TransactionEntity) {
            tvSender.text = transaction.sender
            tvBody.text = transaction.body
            tvTimestamp.text = getRelativeTime(transaction.timestamp)
            
            // Set status icon based on status using custom colors from colors.xml
            when (transaction.status) {
                "SENT" -> {
                    imgStatus.setImageResource(R.drawable.ic_check_circle)
                    imgStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.status_sent))
                }
                "FAILED" -> {
                    imgStatus.setImageResource(R.drawable.ic_error)
                    imgStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.status_failed))
                }
                else -> { // PENDING
                    imgStatus.setImageResource(R.drawable.ic_pending)
                    imgStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.status_pending))
                }
            }
        }
        
        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes min ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours hr ago"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days d ago"
                }
            }
        }
    }
    
    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem == newItem
        }
    }
}
