---
name: android-network-resilience-auth
description: >-
  Network resilience guide covering OkHttp Authenticator for seamless token refreshes (HTTP 401), exponential backoff retries, offline connectivity monitoring, and graceful failure handling.
---

# Android Network Resilience, Offline Fallback & Token Refresh

Use this skill when handling intermittent network connectivity, automatic OAuth2/JWT token refreshes, preventing infinite 401 retry storms, and providing immediate offline indicators in Jetpack Compose.

---

## 1. Thread-Safe Token Refresh with OkHttp `Authenticator`

When access tokens expire, backend returns `HTTP 401 Unauthorized`. If multiple parallel requests fail simultaneously with 401, a naive implementation issues multiple refresh requests, invalidating tokens and logging the user out!

### A. Thread-Safe Token Authenticator
```kotlin
class TokenAuthenticator(
    private val tokenProvider: TokenStorage,
    private val authService: AuthApiService
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loop if refresh token is also invalid
        if (responseCount(response) >= 3) {
            tokenProvider.clearTokens()
            return null
        }

        return runBlocking {
            mutex.withLock {
                val currentToken = tokenProvider.getAccessToken()

                // Check if another parallel thread already refreshed the token
                val requestToken = response.request.header("Authorization")
                val tokenToUse = if (requestToken != null && requestToken != "Bearer $currentToken") {
                    currentToken // Already refreshed by another coroutine!
                } else {
                    // Perform refresh
                    val refreshToken = tokenProvider.getRefreshToken() ?: return@runBlocking null
                    val refreshResponse = authService.refreshToken(RefreshTokenRequest(refreshToken))

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newTokens = refreshResponse.body()!!
                        tokenProvider.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                        newTokens.accessToken
                    } else {
                        tokenProvider.clearTokens()
                        return@runBlocking null
                    }
                }

                // Re-try original request with the new access token
                response.request.newBuilder()
                    .header("Authorization", "Bearer $tokenToUse")
                    .build()
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
```

---

## 2. Real-Time Network Connectivity Observer

Provide a reactive `StateFlow<Boolean>` representing network availability:

```kotlin
class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(hasInternet)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
```

### A. Compose Offline Banner
```kotlin
@Composable
fun OfflineNoticeBanner(isOnline: Boolean) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    "You are offline. Showing cached catalog data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
```
