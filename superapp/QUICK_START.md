# Quick Start Guide - Generic Super App

## 🎯 End-to-End Example: Fetching and Displaying Entities

This guide walks through a complete flow from backend API to UI display.

## Flow Overview

```
Backend API → Network Layer → Repository → Use Case → ViewModel → Compose UI
     ↓
  Room DB (Cache)
```

## Step-by-Step Implementation

### 1. Backend API Response

**Endpoint**: `GET /entities?page=1&pageSize=20`

**Response**:
```json
{
  "data": [
    {
      "id": "ent_001",
      "type": "product",
      "title": "Sample Product",
      "description": "A great product",
      "metadata": {
        "price": 29.99,
        "category": "electronics"
      },
      "status": "active",
      "createdAt": "2025-12-03T10:00:00Z",
      "updatedAt": "2025-12-03T10:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalPages": 3,
    "totalItems": 55
  }
}
```

### 2. Network Layer (DTO)

**File**: `core/network/src/main/kotlin/com/superapp/core/network/model/EntityDto.kt`

```kotlin
data class EntityDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("metadata") val metadata: Map<String, Any>,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)
```

### 3. API Service Interface

**File**: `core/network/src/main/kotlin/com/superapp/core/network/api/EntityApiService.kt`

```kotlin
interface EntityApiService {
    @GET("entities")
    suspend fun getEntities(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): PaginatedResponseDto<EntityDto>
}
```

### 4. Room Database Entity

**File**: `core/database/src/main/kotlin/com/superapp/core/database/entity/EntityEntity.kt`

```kotlin
@Entity(tableName = "entities")
data class EntityEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val description: String?,
    val metadataJson: String,  // Store as JSON string
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

### 5. Domain Model

**File**: `core/domain/src/main/kotlin/com/superapp/core/domain/model/Entity.kt`

```kotlin
data class Entity(
    val id: String,
    val type: String,
    val title: String,
    val description: String?,
    val metadata: Map<String, Any>,
    val status: EntityStatus,
    val createdAt: Long,
    val updatedAt: Long
)

enum class EntityStatus {
    ACTIVE, INACTIVE, ARCHIVED
}
```

### 6. Mapper (DTO → Domain → Database)

**File**: `core/data/src/main/kotlin/com/superapp/core/data/mapper/EntityMapper.kt`

```kotlin
// Network DTO → Domain Model
fun EntityDto.toDomain(): Entity {
    return Entity(
        id = id,
        type = type,
        title = title,
        description = description,
        metadata = metadata,
        status = EntityStatus.valueOf(status.uppercase()),
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli()
    )
}

// Network DTO → Database Entity
fun EntityDto.toEntity(): EntityEntity {
    return EntityEntity(
        id = id,
        type = type,
        title = title,
        description = description,
        metadataJson = Gson().toJson(metadata),
        status = status,
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli()
    )
}

// Database Entity → Domain Model
fun EntityEntity.toDomain(): Entity {
    return Entity(
        id = id,
        type = type,
        title = title,
        description = description,
        metadata = Gson().fromJson(metadataJson, Map::class.java) as Map<String, Any>,
        status = EntityStatus.valueOf(status.uppercase()),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
```

### 7. Repository Implementation (Offline-First)

**File**: `core/data/src/main/kotlin/com/superapp/core/data/repository/EntityRepositoryImpl.kt`

```kotlin
class EntityRepositoryImpl @Inject constructor(
    private val remoteDataSource: EntityRemoteDataSource,
    private val localDataSource: EntityLocalDataSource
) : EntityRepository {

    override fun getEntities(page: Int, pageSize: Int): Flow<Result<PaginatedResult<Entity>>> = flow {
        // 1. Emit loading state
        emit(Result.Loading)

        try {
            // 2. Emit cached data first (offline-first)
            val offset = (page - 1) * pageSize
            localDataSource.getEntities(pageSize, offset)
                .map { entities -> entities.map { it.toDomain() } }
                .collect { cachedEntities ->
                    if (cachedEntities.isNotEmpty()) {
                        emit(Result.Success(PaginatedResult(
                            data = cachedEntities,
                            pagination = Pagination(page, pageSize, 1, cachedEntities.size)
                        )))
                    }
                }

            // 3. Fetch fresh data from API
            val response = remoteDataSource.getEntities(page, pageSize)
            
            // 4. Update local cache
            localDataSource.insertEntities(response.data.map { it.toEntity() })
            
            // 5. Emit fresh data
            emit(Result.Success(response.toDomain { it.toDomain() }))
            
        } catch (e: Exception) {
            // 6. Emit error (cached data already shown if available)
            emit(Result.Error(e, e.message))
        }
    }
}
```

### 8. Use Case

**File**: `core/domain/src/main/kotlin/com/superapp/core/domain/usecase/entity/GetEntitiesUseCase.kt`

```kotlin
data class GetEntitiesParams(
    val page: Int = 1,
    val pageSize: Int = 20
)

class GetEntitiesUseCase @Inject constructor(
    private val repository: EntityRepository
) : FlowUseCase<GetEntitiesParams, PaginatedResult<Entity>>() {

    override fun invoke(params: GetEntitiesParams): Flow<Result<PaginatedResult<Entity>>> {
        return repository.getEntities(params.page, params.pageSize)
    }
}
```

### 9. ViewModel (State Management)

**File**: `feature/featureA/src/main/kotlin/com/superapp/feature/featurea/FeatureAViewModel.kt`

```kotlin
@HiltViewModel
class FeatureAViewModel @Inject constructor(
    private val getEntitiesUseCase: GetEntitiesUseCase
) : BaseViewModel<FeatureAUiState, FeatureAUiEvent, FeatureAUiEffect>(
    initialState = FeatureAUiState()
) {

    init {
        onEvent(FeatureAUiEvent.LoadEntities)
    }

    override fun onEvent(event: FeatureAUiEvent) {
        when (event) {
            is FeatureAUiEvent.LoadEntities -> loadEntities()
            is FeatureAUiEvent.OnEntityClick -> handleEntityClick(event.entityId)
        }
    }

    private fun loadEntities() {
        viewModelScope.launch {
            getEntitiesUseCase(GetEntitiesParams(page = currentState.currentPage))
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            updateState { copy(isLoading = true, error = null) }
                        }
                        is Result.Success -> {
                            updateState {
                                copy(
                                    entities = result.data.data,
                                    isLoading = false,
                                    hasMorePages = currentPage < result.data.pagination.totalPages
                                )
                            }
                        }
                        is Result.Error -> {
                            updateState { copy(isLoading = false, error = result.message) }
                            sendEffect(FeatureAUiEffect.ShowError(result.message ?: "Unknown error"))
                        }
                    }
                }
        }
    }

    private fun handleEntityClick(entityId: String) {
        sendEffect(FeatureAUiEffect.NavigateToDetail(entityId))
    }
}
```

### 10. UI State & Events

**File**: `feature/featureA/src/main/kotlin/com/superapp/feature/featurea/FeatureAContract.kt`

```kotlin
data class FeatureAUiState(
    val entities: List<Entity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
) : UiState

sealed interface FeatureAUiEvent : UiEvent {
    data object LoadEntities : FeatureAUiEvent
    data object LoadNextPage : FeatureAUiEvent
    data class OnEntityClick(val entityId: String) : FeatureAUiEvent
}

sealed interface FeatureAUiEffect : UiEffect {
    data class NavigateToDetail(val entityId: String) : FeatureAUiEffect
    data class ShowError(val message: String) : FeatureAUiEffect
}
```

### 11. Compose UI

**File**: `feature/featureA/src/main/kotlin/com/superapp/feature/featurea/FeatureAScreen.kt`

```kotlin
@Composable
fun FeatureAScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: FeatureAViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle one-time effects
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FeatureAUiEffect.NavigateToDetail -> onNavigateToDetail(effect.entityId)
                is FeatureAUiEffect.ShowError -> {
                    // Show toast or snackbar
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Feature A") }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.entities.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.entities.isEmpty() -> {
                    Text(text = uiState.error, modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.entities) { entity ->
                            EntityListItem(
                                entity = entity,
                                onClick = { viewModel.onEvent(FeatureAUiEvent.OnEntityClick(entity.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityListItem(entity: Entity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = entity.title, style = MaterialTheme.typography.titleMedium)
            entity.description?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = entity.status.name, style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

## 🔄 Complete Data Flow Summary

1. **User opens screen** → ViewModel initialized
2. **ViewModel** → Calls `GetEntitiesUseCase`
3. **Use Case** → Calls `EntityRepository.getEntities()`
4. **Repository** → Emits `Result.Loading`
5. **Repository** → Reads from Room DB (cached data)
6. **Repository** → Emits `Result.Success(cachedData)`
7. **ViewModel** → Updates `uiState` with cached data
8. **UI** → Displays cached data immediately
9. **Repository** → Fetches from API via `EntityRemoteDataSource`
10. **Repository** → Saves fresh data to Room DB
11. **Repository** → Emits `Result.Success(freshData)`
12. **ViewModel** → Updates `uiState` with fresh data
13. **UI** → Updates with fresh data

## 🎨 UI States Handled

- **Loading**: Show progress indicator
- **Success with data**: Display list
- **Success with empty data**: Show empty state
- **Error with cached data**: Show data + error banner
- **Error without cached data**: Show error message

## 🚀 Running the Example

1. **Start your backend API** (or use mock data)
2. **Update API base URL** in `NetworkModule.kt`
3. **Build and run** the app
4. **Navigate to Feature A** screen
5. **Observe**:
   - Loading indicator appears
   - Cached data shows (if available)
   - Fresh data loads from API
   - UI updates automatically

## 🧪 Testing the Flow

### Unit Test ViewModel

```kotlin
@Test
fun `when LoadEntities event, should update state with entities`() = runTest {
    // Given
    val mockEntities = listOf(Entity(...))
    coEvery { getEntitiesUseCase(any()) } returns flowOf(Result.Success(mockEntities))

    // When
    viewModel.onEvent(FeatureAUiEvent.LoadEntities)

    // Then
    assertEquals(mockEntities, viewModel.uiState.value.entities)
    assertFalse(viewModel.uiState.value.isLoading)
}
```

## 📝 Customization Tips

### Change Entity Type
Rename "Entity" to your domain object (e.g., "Product", "Post", "Task"):
1. Update domain models
2. Update DTOs
3. Update database entities
4. Update API endpoints
5. Update UI strings

### Add Filtering
Add filter parameters to `GetEntitiesParams`:
```kotlin
data class GetEntitiesParams(
    val page: Int = 1,
    val pageSize: Int = 20,
    val type: String? = null,
    val status: EntityStatus? = null,
    val searchQuery: String? = null
)
```

### Add Pull-to-Refresh
```kotlin
val pullRefreshState = rememberPullRefreshState(
    refreshing = uiState.isLoading,
    onRefresh = { viewModel.onEvent(FeatureAUiEvent.Refresh) }
)
```

---

**You now have a complete, production-ready data flow from backend to UI!** 🎉
