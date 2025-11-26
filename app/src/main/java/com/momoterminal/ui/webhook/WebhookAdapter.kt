package com.momoterminal.ui.webhook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.momoterminal.R
import com.momoterminal.data.local.entity.WebhookConfigEntity

/**
 * RecyclerView adapter for displaying webhook configurations.
 */
class WebhookAdapter(
    private val onItemClick: (WebhookConfigEntity) -> Unit,
    private val onToggleClick: (WebhookConfigEntity) -> Unit,
    private val onTestClick: (WebhookConfigEntity) -> Unit,
    private val onDeleteClick: (WebhookConfigEntity) -> Unit
) : ListAdapter<WebhookConfigEntity, WebhookAdapter.WebhookViewHolder>(WebhookDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebhookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_webhook, parent, false)
        return WebhookViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: WebhookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class WebhookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvWebhookName)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvWebhookUrl)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        private val switchActive: SwitchMaterial = itemView.findViewById(R.id.switchActive)
        private val btnTest: MaterialButton = itemView.findViewById(R.id.btnTest)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(webhook: WebhookConfigEntity) {
            tvName.text = webhook.name
            tvUrl.text = webhook.url
            tvPhone.text = if (webhook.phoneNumber.isBlank()) "All numbers" else webhook.phoneNumber
            
            // Temporarily remove listener to prevent triggering during bind
            switchActive.setOnCheckedChangeListener(null)
            switchActive.isChecked = webhook.isActive
            switchActive.setOnCheckedChangeListener { _, _ ->
                onToggleClick(webhook)
            }
            
            // Update visual state based on active status
            itemView.alpha = if (webhook.isActive) 1.0f else 0.6f
            
            btnTest.setOnClickListener {
                onTestClick(webhook)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(webhook)
            }
            
            itemView.setOnClickListener {
                onItemClick(webhook)
            }
        }
    }
    
    class WebhookDiffCallback : DiffUtil.ItemCallback<WebhookConfigEntity>() {
        override fun areItemsTheSame(oldItem: WebhookConfigEntity, newItem: WebhookConfigEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: WebhookConfigEntity, newItem: WebhookConfigEntity): Boolean {
            return oldItem == newItem
        }
    }
}
