---
name: android-production-architecture
description: >-
  Architectural blueprint and patterns for building scalable, production-ready Android applications with Clean Architecture, MVI/MVVM, Kotlin Coroutines, Flow, and Jetpack Compose.
---

# Android Production Architecture Blueprint

Use this skill when architecting, designing, or refactoring Android applications to ensure enterprise-grade modularity, testability, and responsiveness.

---

## 1. Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                       UI Layer                          │
│  - Jetpack Compose Screens & Components                 │
│  - ViewModels (UiState & UiEvent)                       │
└────────────────────────────┬────────────────────────────┘
                             │ Observes StateFlow / Dispatches Events
┌────────────────────────────▼────────────────────────────┐
│                     Domain Layer                        │
│  - Use Cases / Interactors (Pure Kotlin)                │
│  - Business Models                                      │
└────────────────────────────┬────────────────────────────┘
                             │ Calls
┌────────────────────────────▼────────────────────────────┐
│                      Data Layer                         │
│  - Repositories (Single Source of Truth)                │
│  - Local Data Sources (Room DB, DataStore)              │
│  - Remote Data Sources (Retrofit, Firebase, Ktor)       │
└─────────────────────────────────────────────────────────┘
```

---

## 2. UI Layer State Modeling (MVI / UDF)

### A. Sealed UI State
Always represent screen states using a sealed interface:

```kotlin
sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Success(
        val items: List<ProductItem>,
        val selectedCategory: String? = null,
        val isRefreshing: Boolean = false
    ) : CatalogUiState
    data class Error(val message: String) : CatalogUiState
}
```

### B. User Actions / Events
```kotlin
sealed interface CatalogUiEvent {
    data class SelectCategory(val category: String) : CatalogUiEvent
    data class AddToCart(val productId: String) : CatalogUiEvent
    data object Refresh : CatalogUiEvent
}
```

### C. ViewModel Implementation
```kotlin
class CatalogViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun onEvent(event: CatalogUiEvent) {
        when (event) {
            is CatalogUiEvent.SelectCategory -> filterCategory(event.category)
            is CatalogUiEvent.AddToCart -> addToCart(event.productId)
            is CatalogUiEvent.Refresh -> loadCatalog(isRefresh = true)
        }
    }

    private fun loadCatalog(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) _uiState.value = CatalogUiState.Loading
            repository.getProductsStream()
                .catch { e -> _uiState.value = CatalogUiState.Error(e.localizedMessage ?: "Unknown error") }
                .collect { items ->
                    _uiState.value = CatalogUiState.Success(items = items)
                }
        }
    }
}
```

---

## 3. Repository Pattern & Single Source of Truth

The repository coordinates local database caching and remote network synchronization:

```kotlin
interface ProductRepository {
    fun getProductsStream(): Flow<List<ProductItem>>
    suspend fun syncProducts(): Result<Unit>
    suspend fun getProductById(id: String): ProductItem?
}

class DefaultProductRepository(
    private val localDao: ProductDao,
    private val remoteApi: ProductApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    override fun getProductsStream(): Flow<List<ProductItem>> =
        localDao.observeAllProducts()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override suspend fun syncProducts(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val response = remoteApi.fetchProducts()
            localDao.upsertProducts(response.map { it.toEntity() })
        }
    }
}
```

---

## 4. Compose UI Integration
Collect state safely in Compose with lifecycle awareness:

```kotlin
@Composable
fun CatalogRoute(
    viewModel: CatalogViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CatalogScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = onNavigateToDetail
    )
}
```
