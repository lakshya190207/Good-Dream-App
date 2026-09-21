---
name: android-dependency-injection
description: >-
  Production dependency injection guide for Android using Hilt and Koin, structuring modules, injecting ViewModels, providing singletons, and configuring test fixtures.
---

# Android Dependency Injection Guide (Hilt & Koin)

Use this skill when configuring dependency injection, resolving circular dependencies, scoping services, or swapping dependencies for automated unit and instrumentation tests.

---

## 1. Hilt Production Setup

### A. Application Class
```kotlin
@HiltAndroidApp
class App : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Inject dependencies into Activity if needed
}
```

### B. Network & Database Modules
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY 
                        else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.gooddream.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_production.db"
        ).build()
    }

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
}
```

### C. Repository Binding
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        impl: DefaultProductRepository
    ): ProductRepository
}
```

### D. ViewModel Injection
```kotlin
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // ...
}
```

---

## 2. Koin Alternative (Lightweight DI)

If using Koin instead of Hilt:

```kotlin
val appModule = module {
    single { provideOkHttpClient() }
    single { provideRetrofit(get()) }
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "app.db").build() }
    single { get<AppDatabase>().productDao() }
    single<ProductRepository> { DefaultProductRepository(get(), get()) }
    viewModel { CatalogViewModel(get()) }
}

// In Application onCreate:
startKoin {
    androidLogger()
    androidContext(this@GoodDreamApplication)
    modules(appModule)
}
```
