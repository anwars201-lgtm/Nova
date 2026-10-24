package dev.local.nova.data

import android.content.Context
import dev.local.nova.security.SecureStore

/**
 * Pengaturan koneksi pengguna.
 *
 * Base URL disimpan di SharedPreferences biasa karena bukan rahasia. API key disimpan
 * lewat [SecureStore] sehingga yang ada di disk hanya ciphertext.
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secure = SecureStore(context)

    var baseUrl: String?
        get() = prefs.getString(KEY_BASE_URL, null)
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value).apply()
        }

    var apiKey: String?
        get() = secure.get(KEY_API_KEY)
        set(value) {
            if (value.isNullOrBlank()) secure.remove(KEY_API_KEY) else secure.put(KEY_API_KEY, value)
        }

    var defaultModel: String?
        get() = prefs.getString(KEY_MODEL, null)
        set(value) {
            prefs.edit().putString(KEY_MODEL, value).apply()
        }

    val isConfigured: Boolean
        get() = !baseUrl.isNullOrBlank() && !apiKey.isNullOrBlank()

    fun clear() {
        prefs.edit().remove(KEY_BASE_URL).remove(KEY_MODEL).apply()
        secure.remove(KEY_API_KEY)
    }

    companion object {
        private const val PREFS_NAME = "nova_settings"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "default_model"
        private const val KEY_API_KEY = "api_key"
    }
}
