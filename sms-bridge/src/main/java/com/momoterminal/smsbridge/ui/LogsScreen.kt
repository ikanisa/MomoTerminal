package com.momoterminal.smsbridge.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.smsbridge.data.AppDatabase
import com.momoterminal.smsbridge.data.MessageDao
import com.momoterminal.smsbridge.data.MessageEntity
import com.momoterminal.smsbridge.data.MessageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {
    val messages = database.messageDao().getRecentMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun LogsScreen(viewModel: LogsViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(messages) { message ->
            LogItem(message)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LogItem(message: MessageEntity) {
    val statusColor = when (message.status) {
        MessageStatus.SENT -> Color(0xFF4CAF50)
        MessageStatus.FAILED -> Color.Red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message.form,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when (message.status) {
                        MessageStatus.PENDING -> stringResource(com.momoterminal.smsbridge.R.string.status_pending)
                        MessageStatus.SENDING -> stringResource(com.momoterminal.smsbridge.R.string.status_sending)
                        MessageStatus.SENT -> stringResource(com.momoterminal.smsbridge.R.string.status_sent)
                        MessageStatus.FAILED -> stringResource(com.momoterminal.smsbridge.R.string.status_failed)
                    },
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${message.receivedAt} • Attempts: ${message.attempts}",
                style = MaterialTheme.typography.labelSmall
            )
            if (!message.lastError.isNullOrEmpty()) {
                Text(
                    text = "Error: ${message.lastError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
