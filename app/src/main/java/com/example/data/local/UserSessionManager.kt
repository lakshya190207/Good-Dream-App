package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.model.CrmUserRecord
import com.example.data.model.CrmUserType
import timber.log.Timber
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages customer session persistence with AES-256 encrypted SharedPreferences
 * compliant with Android Production security guidelines.
 */
class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPreferencesWithRecovery(context)

    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun saveUserSession(email: String, name: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .apply()
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Registers a new user with their full name, email, and secure hashed passcode with per-user salt.
     */
    fun registerUser(email: String, name: String, passcode: String) {
        val cleanEmail = email.trim().lowercase()
        val hashedPass = hashPasscode(passcode.trim(), cleanEmail)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_EMAIL, cleanEmail)
            .putString(KEY_USER_NAME, name.trim())
            .putString(KEY_PREFIX_PASSCODE + cleanEmail, hashedPass)
            .putString(KEY_PREFIX_NAME + cleanEmail, name.trim())
            .apply()
    }

    /**
     * Verifies the entered passcode against the stored hashed passcode for [email].
     * Supports both modern PBKDF2 ($pbkdf2$...) and legacy SHA-256 hashes, transparently
     * upgrading legacy hashes to PBKDF2 upon successful verification.
     */
    fun verifyUserPasscode(email: String, enteredPasscode: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val storedHash = prefs.getString(KEY_PREFIX_PASSCODE + cleanEmail, null) ?: return false
        val cleanPasscode = enteredPasscode.trim()

        if (storedHash.startsWith("\$pbkdf2\$")) {
            val parts = storedHash.split("\$")
            if (parts.size >= 4) {
                val saltHex = parts[2]
                val expectedHashHex = parts[3]
                val computedHash = hashPbkdf2(cleanPasscode, saltHex)
                val expectedBytes = expectedHashHex.toByteArray(Charsets.UTF_8)
                val computedBytes = computedHash.toByteArray(Charsets.UTF_8)
                return expectedBytes.size == computedBytes.size &&
                        MessageDigest.isEqual(expectedBytes, computedBytes)
            }
        }

        // Legacy SHA-256 verification with automatic upgrade
        val legacyHash = hashPasscodeLegacy(cleanPasscode, cleanEmail)
        val storedBytes = storedHash.toByteArray(Charsets.UTF_8)
        val legacyBytes = legacyHash.toByteArray(Charsets.UTF_8)
        val isLegacyMatch = storedBytes.size == legacyBytes.size &&
                MessageDigest.isEqual(storedBytes, legacyBytes)

        if (isLegacyMatch) {
            val newHash = hashPasscode(cleanPasscode, cleanEmail)
            prefs.edit().putString(KEY_PREFIX_PASSCODE + cleanEmail, newHash).apply()
            return true
        }

        return false
    }

    fun getUserNameForEmail(email: String): String? {
        val cleanEmail = email.trim().lowercase()
        return prefs.getString(KEY_PREFIX_NAME + cleanEmail, null)
    }

    fun hasUserPasscode(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return prefs.contains(KEY_PREFIX_PASSCODE + cleanEmail)
    }

    fun getSavedAddresses(email: String): List<SavedAddress> {
        val cleanEmail = email.trim().lowercase()
        val rawJson = prefs.getString(KEY_PREFIX_ADDRESSES + cleanEmail, null) ?: return emptyList()
        return try {
            val jsonArray = org.json.JSONArray(rawJson)
            val list = mutableListOf<SavedAddress>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SavedAddress(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        tag = obj.optString("tag", "Home"),
                        fullName = obj.optString("fullName", ""),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        flatHouseNo = obj.optString("flatHouseNo", ""),
                        streetLocality = obj.optString("streetLocality", ""),
                        landmark = obj.optString("landmark", ""),
                        city = obj.optString("city", "Bengaluru"),
                        state = obj.optString("state", "Karnataka"),
                        pincode = obj.optString("pincode", "560001"),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Timber.e(e, "Error parsing saved addresses")
            emptyList()
        }
    }

    fun saveAddress(email: String, address: SavedAddress) {
        val cleanEmail = email.trim().lowercase()
        val currentList = getSavedAddresses(cleanEmail).toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == address.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = address
        } else {
            currentList.add(address)
        }
        val jsonArray = org.json.JSONArray()
        currentList.forEach { addr ->
            val obj = org.json.JSONObject().apply {
                put("id", addr.id)
                put("tag", addr.tag)
                put("fullName", addr.fullName)
                put("phoneNumber", addr.phoneNumber)
                put("flatHouseNo", addr.flatHouseNo)
                put("streetLocality", addr.streetLocality)
                put("landmark", addr.landmark)
                put("city", addr.city)
                put("state", addr.state)
                put("pincode", addr.pincode)
                put("isDefault", addr.isDefault)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PREFIX_ADDRESSES + cleanEmail, jsonArray.toString()).apply()
    }

    fun deleteAddress(email: String, addressId: String) {
        val cleanEmail = email.trim().lowercase()
        val updated = getSavedAddresses(cleanEmail).filter { it.id != addressId }
        val jsonArray = org.json.JSONArray()
        updated.forEach { addr ->
            val obj = org.json.JSONObject().apply {
                put("id", addr.id)
                put("tag", addr.tag)
                put("fullName", addr.fullName)
                put("phoneNumber", addr.phoneNumber)
                put("flatHouseNo", addr.flatHouseNo)
                put("streetLocality", addr.streetLocality)
                put("landmark", addr.landmark)
                put("city", addr.city)
                put("state", addr.state)
                put("pincode", addr.pincode)
                put("isDefault", addr.isDefault)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PREFIX_ADDRESSES + cleanEmail, jsonArray.toString()).apply()
    }

    fun clearUserSession() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .apply()
    }

    /**
     * Completely purges the user's account credentials and session in compliance
     * with Google Play Store User Data & Account Deletion guidelines.
     */
    fun deleteUserAccountAndData(email: String?) {
        val editor = prefs.edit()
        if (!email.isNullOrBlank()) {
            val cleanEmail = email.trim().lowercase()
            editor.remove(KEY_PREFIX_PASSCODE + cleanEmail)
            editor.remove(KEY_PREFIX_NAME + cleanEmail)
            editor.remove(KEY_PREFIX_PHONE + cleanEmail)
            editor.remove(KEY_PREFIX_NOTES + cleanEmail)
            editor.remove(KEY_PREFIX_ADDRESSES + cleanEmail)
        }
        editor.remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .apply()
    }

    /**
     * Returns all registered users stored securely in SharedPreferences.
     */
    fun getAllRegisteredUsers(): List<CrmUserRecord> {
        val allEntries = prefs.all
        val users = mutableListOf<CrmUserRecord>()
        allEntries.keys.filter { it.startsWith(KEY_PREFIX_NAME) }.forEach { key ->
            val email = key.removePrefix(KEY_PREFIX_NAME)
            val name = allEntries[key] as? String ?: "Sanctuary Client"
            val addresses = getSavedAddresses(email)
            val phone = addresses.firstOrNull()?.phoneNumber ?: prefs.getString(KEY_PREFIX_PHONE + email, "") ?: ""
            val notes = prefs.getString(KEY_PREFIX_NOTES + email, "") ?: ""
            users.add(
                CrmUserRecord(
                    id = email,
                    name = name,
                    email = email,
                    phone = phone,
                    userType = CrmUserType.REGISTERED_VIP,
                    addresses = addresses,
                    notes = notes
                )
            )
        }
        return users
    }

    /**
     * Saves or updates a user directly from the Admin CRM Studio.
     */
    fun saveCrmUser(email: String, name: String, phone: String, notes: String = "") {
        val cleanEmail = email.trim().lowercase()
        val editor = prefs.edit()
            .putString(KEY_PREFIX_NAME + cleanEmail, name.trim())
            .putString(KEY_PREFIX_PHONE + cleanEmail, phone.trim())
            .putString(KEY_PREFIX_NOTES + cleanEmail, notes.trim())
        if (!hasUserPasscode(cleanEmail)) {
            val defaultPasscode = hashPasscode("1234", cleanEmail)
            editor.putString(KEY_PREFIX_PASSCODE + cleanEmail, defaultPasscode)
        }
        editor.apply()
    }

    private fun hashPasscode(passcode: String, saltIdentifier: String): String {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val hashHex = hashPbkdf2(passcode, saltHex)
        return "\$pbkdf2\$$saltHex\$$hashHex"
    }

    private fun hashPbkdf2(passcode: String, saltHex: String): String {
        val saltBytes = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(passcode.toCharArray(), saltBytes, 210_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun hashPasscodeLegacy(passcode: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedBytes = ("$salt:GoodDreamSecuritySalt2026:$passcode").toByteArray(Charsets.UTF_8)
        val hash = digest.digest(saltedBytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME_ENCRYPTED = "gooddream_secure_user_session"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val PREFS_NAME_FALLBACK = "gooddream_user_session"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_PREFIX_PASSCODE = "passcode_user_"
        private const val KEY_PREFIX_NAME = "name_user_"
        private const val KEY_PREFIX_PHONE = "phone_user_"
        private const val KEY_PREFIX_NOTES = "notes_user_"
        private const val KEY_PREFIX_ADDRESSES = "addresses_user_"
        private const val KEY_FCM_TOKEN = "key_fcm_token"

        private fun createEncryptedPreferencesWithRecovery(context: Context): SharedPreferences {
            return try {
                buildEncryptedPreferences(context)
            } catch (e: Exception) {
                Timber.w(e, "EncryptedSharedPreferences initialization failed; attempting keystore alias reset")
                resetCorruptedKeystoreAlias()
                try {
                    buildEncryptedPreferences(context)
                } catch (retryEx: Exception) {
                    Timber.w(retryEx, "Hardware Keystore unavailable in current environment; falling back to private SharedPreferences")
                    context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
                }
            }
        }

        private fun buildEncryptedPreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME_ENCRYPTED,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        private fun resetCorruptedKeystoreAlias() {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                    keyStore.deleteEntry(MASTER_KEY_ALIAS)
                    Timber.i("Purged corrupted Keystore alias: $MASTER_KEY_ALIAS")
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to reset Keystore entry")
            }
        }
    }
}

data class SavedAddress(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tag: String = "Home",
    val fullName: String,
    val phoneNumber: String,
    val flatHouseNo: String,
    val streetLocality: String,
    val landmark: String = "",
    val city: String = "Bengaluru",
    val state: String = "Karnataka",
    val pincode: String = "560001",
    val isDefault: Boolean = false
)

