package com.momoterminal.ui.webhook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.momoterminal.R
import com.momoterminal.data.local.entity.SmsDeliveryLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying SMS delivery logs.
 */
class DeliveryLogAdapter(
    private val onItemClick: (SmsDeliveryLogEntity) -> Unit,
    private val onRetryClick: (SmsDeliveryLogEntity) -> Unit,
    private val getWebhookName: (Long) -> String
) : ListAdapter<SmsDeliveryLogEntity, DeliveryLogAdapter.DeliveryLogViewHolder>(DeliveryLogDiffCallback()) {
    
    companion object {
        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_log, parent, false)
        return DeliveryLogViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DeliveryLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DeliveryLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWebhookName: TextView = itemView.findViewById(R.id.tvWebhookName)
        private val tvSender: TextView = itemView.findViewById(R.id.tvSender)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val btnRetry: ImageButton = itemView.findViewById(R.id.btnRetry)
        
        fun bind(log: SmsDeliveryLogEntity) {
            tvWebhookName.text = getWebhookName(log.webhookId)
            tvSender.text = log.sender
            tvMessage.text = log.message.take(80) + if (log.message.length > 80) "..." else ""
            tvTimestamp.text = dateFormat.format(Date(log.createdAt))
            
            // Set status with color
            tvStatus.text = log.status.uppercase()
            val statusColor = when (log.status) {
                SmsDeliveryLogEntity.STATUS_SENT, 
                SmsDeliveryLogEntity.STATUS_DELIVERED -> android.graphics.Color.parseColor("#4CAF50")
                SmsDeliveryLogEntity.STATUS_FAILED -> android.graphics.Color.parseColor("#F44336")
                else -> android.graphics.Color.parseColor("#FF9800")
            }
            tvStatus.setTextColor(statusColor)
            
            // Show retry button only for failed/pending
            val canRetry = log.status == SmsDeliveryLogEntity.STATUS_FAILED || 
                          log.status == SmsDeliveryLogEntity.STATUS_PENDING
            btnRetry.visibility = if (canRetry) View.VISIBLE else View.GONE
            
            btnRetry.setOnClickListener {
                onRetryClick(log)
            }
            
            itemView.setOnClickListener {
                onItemClick(log)
            }
        }
    }
    
    class DeliveryLogDiffCallback : DiffUtil.ItemCallback<SmsDeliveryLogEntity>() {
        override fun areItemsTheSame(oldItem: SmsDeliveryLogEntity, newItem: SmsDeliveryLogEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: SmsDeliveryLogEntity, newItem: SmsDeliveryLogEntity): Boolean {
            return oldItem == newItem
        }
    }
}
