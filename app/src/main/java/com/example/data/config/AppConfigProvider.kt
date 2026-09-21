package com.example.data.config

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Provider responsible for fetching, caching, and dynamically supplying
 * AppConfig (such as contact info, working hours, and promotional offer banners)
 * from Firestore at application startup.
 */
object AppConfigProvider {
    private const val TAG = "AppConfigProvider"
    const val FIRESTORE_COLLECTION = "app_config"
    const val FIRESTORE_DOCUMENT = "global_config"

    private val _configState = MutableStateFlow(AppConfig())
    val configState: StateFlow<AppConfig> = _configState.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null
    private val isInitialized = AtomicBoolean(false)

    /**
     * Initializes and fetches configuration at app startup.
     * Can be safely called from Application.onCreate() or ViewModel init.
     */
    fun fetchAtStartup(context: Context? = null, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        if (isInitialized.getAndSet(true)) return

        scope.launch {
            fetchRemoteConfig()
            startRealtimeListener()
        }
    }

    /**
     * Suspends and fetches the latest AppConfig from Firestore with graceful offline fallback.
     */
    suspend fun fetchRemoteConfig(): AppConfig = withContext(Dispatchers.IO) {
        try {
            val apps = FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext)
            if (apps.isEmpty()) {
                Log.w(TAG, "No active FirebaseApp instance; providing default AppConfig")
                return@withContext _configState.value
            }

            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection(FIRESTORE_COLLECTION).document(FIRESTORE_DOCUMENT)
            val snapshot = docRef.get().awaitTask()

            if (snapshot.exists() && snapshot.data != null) {
                val remoteConfig = try {
                    snapshot.toObject(AppConfig::class.java) ?: AppConfig.fromMap(snapshot.data)
                } catch (e: Exception) {
                    Log.w(TAG, "Using fromMap fallback for AppConfig: ${e.message}")
                    AppConfig.fromMap(snapshot.data)
                }
                Log.i(TAG, "Successfully fetched AppConfig from Firestore: company=${remoteConfig.companyName}, banners=${remoteConfig.offerBanners.size}")
                _configState.value = remoteConfig
                remoteConfig
            } else {
                Log.i(TAG, "Firestore config document does not exist yet. Seeding default AppConfig...")
                val defaultConfig = AppConfig()
                try {
                    docRef.set(defaultConfig.toMap()).awaitTask()
                    Log.i(TAG, "Successfully seeded initial AppConfig to Firestore")
                } catch (seedError: Exception) {
                    Log.w(TAG, "Notice: could not seed default config to Firestore: ${seedError.message}")
                }
                _configState.value = defaultConfig
                defaultConfig
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to fetch AppConfig from Firestore (${e.message}). Retaining local default values.")
            _configState.value
        }
    }

    /**
     * Listens in real-time to remote config changes in Firestore.
     */
    fun startRealtimeListener() {
        try {
            if (firestoreListener != null) return
            val db = FirebaseFirestore.getInstance()
            firestoreListener = db.collection(FIRESTORE_COLLECTION).document(FIRESTORE_DOCUMENT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore realtime listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                        val updated = try {
                            snapshot.toObject(AppConfig::class.java) ?: AppConfig.fromMap(snapshot.data)
                        } catch (_: Exception) {
                            AppConfig.fromMap(snapshot.data)
                        }
                        _configState.value = updated
                        Log.i(TAG, "AppConfig dynamically refreshed via Firestore realtime event")
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start Firestore realtime listener: ${e.message}")
        }
    }

    /**
     * Manually updates the config state (useful for local admin panel or testing).
     */
    fun updateConfigManually(newConfig: AppConfig) {
        _configState.value = newConfig
    }

    /**
     * Resets listeners and status for testing.
     */
    fun resetForTesting() {
        firestoreListener?.remove()
        firestoreListener = null
        isInitialized.set(false)
        _configState.value = AppConfig()
    }
}

/**
 * Await extension for Google Play Services Task without external dependency conflicts.
 */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}
