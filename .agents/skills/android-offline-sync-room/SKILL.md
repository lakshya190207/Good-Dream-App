---
name: android-offline-sync-room
description: >-
  Production guide for offline-first Android apps using Room database, robust schema migrations, Flow observation, and WorkManager background synchronization.
---

# Android Offline-First & Room Data Layer

Use this skill when building offline-capable Android apps, implementing Room migrations, managing cache-network synchronization, or setting up reliable background synchronization with WorkManager.

---

## 1. Room Database Setup with Migrations

### A. Database Definition & AutoMigration
```kotlin
@Database(
    entities = [ProductEntity::class, CategoryEntity::class, CartItemEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
}
```

### B. Manual Migration for Complex Schema Changes
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN discount_percent INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_category ON products(category_id)")
    }
}
```

### C. Safe Database Builder
Never use `fallbackToDestructiveMigration()` in production!
```kotlin
fun buildDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "app_production.db"
    )
    .addMigrations(MIGRATION_2_3)
    .build()
```

---

## 2. DAO Best Practices

```kotlin
@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): ProductEntity?

    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE updatedAt < :timestamp")
    suspend fun purgeStaleCache(timestamp: Long)
}
```

---

## 3. WorkManager Background Synchronization

WorkManager guarantees background work execution even if the app process is killed or the device reboots.

### A. Define the CoroutineWorker
```kotlin
class SyncDataWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: ProductRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val syncResult = repository.syncProducts()
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
```

### B. Schedule Periodic Background Sync
```kotlin
fun schedulePeriodicDataSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val syncRequest = PeriodicWorkRequestBuilder<SyncDataWorker>(
        repeatInterval = 6,
        repeatIntervalTimeUnit = TimeUnit.HOURS
    )
    .setConstraints(constraints)
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "CatalogPeriodicSync",
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
}
```
