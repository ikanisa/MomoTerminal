# Performance Optimization Guide

## Compose Performance Tips

### 1. Avoid Heavy Recomposition

```kotlin
// ❌ BAD: Recomposes on every state change
@Composable
fun ItemList(items: List<Item>, selectedId: String) {
    LazyColumn {
        items(items) { item ->
            ItemCard(
                item = item,
                isSelected = item.id == selectedId // Recomposes all items
            )
        }
    }
}

// ✅ GOOD: Use key and remember
@Composable
fun ItemList(items: List<Item>, selectedId: String) {
    LazyColumn {
        items(
            items = items,
            key = { it.id } // Stable keys
        ) { item ->
            ItemCard(
                item = item,
                isSelected = remember(item.id, selectedId) { 
                    item.id == selectedId 
                }
            )
        }
    }
}
```

### 2. Use derivedStateOf for Computed Values

```kotlin
// ❌ BAD: Recomputes on every recomposition
@Composable
fun FilteredList(items: List<Item>, query: String) {
    val filtered = items.filter { it.title.contains(query) }
    // ...
}

// ✅ GOOD: Only recomputes when dependencies change
@Composable
fun FilteredList(items: List<Item>, query: String) {
    val filtered by remember(items, query) {
        derivedStateOf {
            items.filter { it.title.contains(query) }
        }
    }
    // ...
}
```

### 3. Defer State Reads

```kotlin
// ❌ BAD: Reads state in composition
@Composable
fun Counter(count: State<Int>) {
    Text("Count: ${count.value}") // Recomposes on every count change
}

// ✅ GOOD: Defer state read to draw phase
@Composable
fun Counter(count: State<Int>) {
    Text("Count: ${count.value}") // Still recomposes, but...
}

// ✅ BETTER: Use Modifier for non-structural changes
@Composable
fun AnimatedBox(offset: State<Float>) {
    Box(
        modifier = Modifier.offset { IntOffset(offset.value.toInt(), 0) }
        // Doesn't recompose, just redraws
    )
}
```

### 4. Stable Parameters

```kotlin
// ❌ BAD: Unstable lambda
@Composable
fun ItemCard(item: Item, onClick: (Item) -> Unit) {
    // onClick is unstable, causes recomposition
}

// ✅ GOOD: Stable lambda with remember
@Composable
fun ItemList(items: List<Item>, onItemClick: (Item) -> Unit) {
    items.forEach { item ->
        val onClick = remember(item.id) { { onItemClick(item) } }
        ItemCard(item, onClick)
    }
}
```

### 5. Lazy Layouts

```kotlin
// ✅ Use LazyColumn/LazyRow for long lists
@Composable
fun LongList(items: List<Item>) {
    LazyColumn {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            ItemCard(item)
        }
    }
}

// ✅ Use contentType for heterogeneous lists
LazyColumn {
    items(
        items = items,
        key = { it.id },
        contentType = { it.type } // Helps with recycling
    ) { item ->
        when (item.type) {
            "header" -> HeaderCard(item)
            "content" -> ContentCard(item)
        }
    }
}
```

## Image Loading Strategy

### 1. Coil Configuration

```kotlin
// In Application class
@Singleton
class ImageLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    val coil = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25) // 25% of app memory
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(50 * 1024 * 1024) // 50MB
                .build()
        }
        .respectCacheHeaders(false) // Use our cache policy
        .build()
}

// Usage in Compose
@Composable
fun ItemImage(url: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .size(Size.ORIGINAL) // Or specific size
            .build(),
        contentDescription = null,
        modifier = Modifier.size(100.dp)
    )
}
```

### 2. Image Optimization

```kotlin
// ✅ Specify size to avoid loading full resolution
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .size(300, 300) // Load only what's needed
        .scale(Scale.FILL)
        .build(),
    contentDescription = null
)

// ✅ Use placeholder and error images
AsyncImage(
    model = url,
    contentDescription = null,
    placeholder = painterResource(R.drawable.placeholder),
    error = painterResource(R.drawable.error)
)

// ✅ Preload images
LaunchedEffect(items) {
    items.forEach { item ->
        imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(item.imageUrl)
                .build()
        )
    }
}
```

## Performance Profiling Checklist

### 1. Startup Performance

```bash
# Measure cold start time
adb shell am start -W -n com.momoterminal/.MainActivity

# Output:
# TotalTime: 1234 (target: < 1500ms)
```

### 2. Compose Performance

```kotlin
// Enable composition tracing
@Composable
fun MyScreen() {
    CompositionLocalProvider(
        LocalInspectionMode provides true
    ) {
        // Your content
    }
}

// Check recomposition counts in Layout Inspector
```

### 3. Memory Profiling

```bash
# Dump memory info
adb shell dumpsys meminfo com.momoterminal

# Check for:
# - Total memory usage
# - Heap size
# - Native heap
# - Graphics memory
```

### 4. Network Performance

```kotlin
// Add logging interceptor
OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    })
    .build()

// Monitor:
# - Request count
# - Response times
# - Payload sizes
# - Cache hit rate
```

### 5. Database Performance

```kotlin
// Enable query logging
Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .setQueryCallback({ sqlQuery, bindArgs ->
        Log.d("Room", "Query: $sqlQuery")
    }, Executors.newSingleThreadExecutor())
    .build()

// Check for:
# - Slow queries (> 100ms)
# - Missing indexes
# - Large result sets
```

### 6. Frame Rate

```bash
# Monitor frame rate
adb shell dumpsys gfxinfo com.momoterminal

# Check for:
# - Dropped frames
# - Slow frames (> 16ms)
# - Janky animations
```

## Performance Optimization Checklist

### Startup
- [ ] Baseline profiles generated
- [ ] Heavy initialization deferred
- [ ] Splash screen optimized
- [ ] Cold start < 1.5s
- [ ] Warm start < 500ms

### UI/Compose
- [ ] Stable keys in LazyColumn
- [ ] derivedStateOf for computed values
- [ ] remember for expensive calculations
- [ ] Avoid unnecessary recomposition
- [ ] 60fps maintained during scrolling

### Images
- [ ] Coil/Glide configured with caching
- [ ] Image sizes specified
- [ ] Placeholders used
- [ ] Preloading for critical images
- [ ] Memory cache < 25% of app memory

### Network
- [ ] Request timeout configured (30s)
- [ ] Retry policy implemented
- [ ] Response caching enabled
- [ ] Payload sizes optimized
- [ ] Concurrent requests limited

### Database
- [ ] Indexes on frequently queried columns
- [ ] Queries optimized (< 100ms)
- [ ] Batch operations used
- [ ] Database size monitored
- [ ] Old data cleaned up

### Memory
- [ ] No memory leaks
- [ ] Bitmap recycling
- [ ] Large objects released
- [ ] WeakReferences for caches
- [ ] Memory usage < 100MB

### Offline
- [ ] Cache-first strategy
- [ ] Pending actions queued
- [ ] Sync on network available
- [ ] Offline indicator shown
- [ ] Cache expiration policy

### Monitoring
- [ ] Crashlytics integrated
- [ ] Performance traces added
- [ ] Network logging enabled
- [ ] Analytics events tracked
- [ ] Debug menu available

## Performance Testing

### 1. Automated Tests

```kotlin
@Test
fun testStartupPerformance() {
    val startTime = System.currentTimeMillis()
    
    // Launch app
    activityRule.launchActivity(null)
    
    // Wait for first frame
    onView(isRoot()).check(matches(isDisplayed()))
    
    val duration = System.currentTimeMillis() - startTime
    assertThat(duration).isLessThan(1500) // < 1.5s
}

@Test
fun testScrollPerformance() {
    // Scroll list
    onView(withId(R.id.recycler_view))
        .perform(RecyclerViewActions.scrollToPosition(100))
    
    // Check no dropped frames
    // (Use FrameMetricsAggregator)
}
```

### 2. Manual Testing

```
1. Cold start test:
   - Force stop app
   - Clear app data
   - Launch and measure time to interactive

2. Scroll test:
   - Open long list
   - Scroll rapidly
   - Check for jank/stuttering

3. Network test:
   - Enable airplane mode
   - Verify offline functionality
   - Re-enable network
   - Verify sync

4. Memory test:
   - Use app for 30 minutes
   - Check memory usage
   - Verify no leaks

5. Battery test:
   - Use app for 1 hour
   - Check battery drain
   - Should be < 5%
```

## Performance Targets

| Metric | Target | Critical |
|--------|--------|----------|
| Cold start | < 1.5s | < 2s |
| Warm start | < 500ms | < 1s |
| Time to interactive | < 2s | < 3s |
| Frame rate | 60fps | 30fps |
| Memory usage | < 100MB | < 150MB |
| APK size | < 20MB | < 30MB |
| Network timeout | 30s | 60s |
| Cache hit rate | > 80% | > 60% |
| Crash-free rate | > 99.5% | > 99% |

---

**Follow these guidelines to maintain excellent performance across all features!**
