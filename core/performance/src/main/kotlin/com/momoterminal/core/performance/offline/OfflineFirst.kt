package com.momoterminal.core.performance.offline

import androidx.room.*
import androidx.work.*
import com.momoterminal.core.common.Result
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// 1. OFFLINE-FIRST REPOSITORY PATTERN

abstract class OfflineFirstRepository<T, ID> {
    
    // Template method pattern
    fun get(id: ID): Flow<Result<T>> = flow {
        emit(Result.Loading)
        
        // 1. Emit cached data immediately
        getFromCache(id).firstOrNull()?.let {
            emit(Result.Success(it))
        }
        
        // 2. Fetch from network if online
        if (isOnline()) {
            try {
                val fresh = fetchFromNetwork(id)
                saveToCache(fresh)
                emit(Result.Success(fresh))
            } catch (e: Exception) {
                // Cache is already emitted, just log error
                emit(Result.Error(e))
            }
        }
    }
    
    fun getAll(): Flow<Result<List<T>>> = flow {
        emit(Result.Loading)
        
        // 1. Emit cached data
        getAllFromCache().firstOrNull()?.let {
            if (it.isNotEmpty()) {
                emit(Result.Success(it))
            }
        }
        
        // 2. Fetch from network
        if (isOnline()) {
            try {
                val fresh = fetchAllFromNetwork()
                saveAllToCache(fresh)
                emit(Result.Success(fresh))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }
    
    // Abstract methods to implement
    protected abstract fun getFromCache(id: ID): Flow<T?>
    protected abstract fun getAllFromCache(): Flow<List<T>>
    protected abstract suspend fun fetchFromNetwork(id: ID): T
    protected abstract suspend fun fetchAllFromNetwork(): List<T>
    protected abstract suspend fun saveToCache(item: T)
    protected abstract suspend fun saveAllToCache(items: List<T>)
    protected abstract fun isOnline(): Boolean
}

// 2. EXAMPLE IMPLEMENTATION

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val data: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE id = :id")
    fun getById(id: String): Flow<ItemEntity?>
    
    @Query("SELECT * FROM items ORDER BY cachedAt DESC")
    fun getAll(): Flow<List<ItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)
    
    @Query("DELETE FROM items WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

@Singleton
class ItemRepository @Inject constructor(
    private val dao: ItemDao,
    private val api: ItemApiService,
    private val networkMonitor: NetworkMonitor
) : OfflineFirstRepository<ItemEntity, String>() {
    
    override fun getFromCache(id: String) = dao.getById(id)
    override fun getAllFromCache() = dao.getAll()
    override suspend fun fetchFromNetwork(id: String) = api.getItem(id).toEntity()
    override suspend fun fetchAllFromNetwork() = api.getItems().map { it.toEntity() }
    override suspend fun saveToCache(item: ItemEntity) = dao.insert(item)
    override suspend fun saveAllToCache(items: List<ItemEntity>) = dao.insertAll(items)
    override fun isOnline() = networkMonitor.isOnline.value
}

// 3. PENDING ACTION QUEUE (for offline operations)

@Entity(tableName = "pending_actions")
data class PendingAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "CREATE", "UPDATE", "DELETE"
    val entityType: String, // "ITEM", "USER", etc.
    val entityId: String?,
    val data: String, // JSON payload
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PendingAction>>
    
    @Insert
    suspend fun insert(action: PendingAction): Long
    
    @Delete
    suspend fun delete(action: PendingAction)
    
    @Query("UPDATE pending_actions SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)
}

@Singleton
class PendingActionQueue @Inject constructor(
    private val dao: PendingActionDao,
    private val workManager: WorkManager
) {
    
    suspend fun enqueue(action: PendingAction) {
        dao.insert(action)
        scheduleSyncWork()
    }
    
    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "sync_pending_actions",
            ExistingWorkPolicy.KEEP,
            syncWork
        )
    }
}

// 4. SYNC WORKER

class SyncWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val pendingActionDao: PendingActionDao,
    private val api: ItemApiService
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val actions = pendingActionDao.getAll().first()
        
        var hasFailure = false
        
        actions.forEach { action ->
            try {
                when (action.type) {
                    "CREATE" -> api.createItem(action.data)
                    "UPDATE" -> api.updateItem(action.entityId!!, action.data)
                    "DELETE" -> api.deleteItem(action.entityId!!)
                }
                
                // Success - remove from queue
                pendingActionDao.delete(action)
                
            } catch (e: Exception) {
                // Failure - increment retry
                if (action.retryCount < action.maxRetries) {
                    pendingActionDao.incrementRetry(action.id)
                    hasFailure = true
                } else {
                    // Max retries reached - remove or mark as failed
                    pendingActionDao.delete(action)
                }
            }
        }
        
        return if (hasFailure) Result.retry() else Result.success()
    }
}

// 5. SYNC STATE IN UI

data class SyncState(
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncTime: Long? = null,
    val syncError: String? = null
)

data class ItemListUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val syncState: SyncState = SyncState()
) : UiState

@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val pendingActionQueue: PendingActionQueue
) : BaseViewModel<ItemListUiState, ItemListEvent, ItemListEffect>(
    initialState = ItemListUiState()
) {
    
    init {
        loadItems()
        observeSyncState()
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            repository.getAll().collect { result ->
                when (result) {
                    is Result.Loading -> updateState { copy(isLoading = true) }
                    is Result.Success -> updateState { 
                        copy(items = result.data, isLoading = false, error = null) 
                    }
                    is Result.Error -> updateState { 
                        copy(isLoading = false, error = AppError.from(result.exception)) 
                    }
                }
            }
        }
    }
    
    private fun observeSyncState() {
        viewModelScope.launch {
            pendingActionQueue.observePendingCount().collect { count ->
                updateState { 
                    copy(syncState = syncState.copy(
                        pendingCount = count,
                        isSyncing = count > 0
                    ))
                }
            }
        }
    }
    
    override fun onEvent(event: ItemListEvent) {
        when (event) {
            is ItemListEvent.CreateItem -> createItem(event.data)
            is ItemListEvent.Refresh -> loadItems()
        }
    }
    
    private fun createItem(data: String) {
        viewModelScope.launch {
            // Queue for offline sync
            pendingActionQueue.enqueue(
                PendingAction(
                    type = "CREATE",
                    entityType = "ITEM",
                    entityId = null,
                    data = data
                )
            )
            
            // Optimistic update
            sendEffect(ItemListEffect.ShowSuccess("Item queued for sync"))
        }
    }
}

// 6. UI COMPONENT WITH SYNC INDICATOR

@Composable
fun ItemListScreen(
    viewModel: ItemListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Items") },
                actions = {
                    // Sync indicator
                    if (uiState.syncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    
                    if (uiState.syncState.pendingCount > 0) {
                        Badge { Text("${uiState.syncState.pendingCount}") }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
            items(uiState.items) { item ->
                ItemCard(item)
            }
        }
    }
}
