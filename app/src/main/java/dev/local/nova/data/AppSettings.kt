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

    var systemPrompt: String?
        get() = prefs.getString(KEY_SYSTEM_PROMPT, null)
        set(value) {
            prefs.edit().putString(KEY_SYSTEM_PROMPT, value?.takeIf { it.isNotBlank() }).apply()
        }

    /** Saklar global: kalau mati, tidak ada memori yang disisipkan ke system prompt. */
    var memoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MEMORY_ENABLED, value).apply()
        }

    /** Kosong berarti parameter tidak dikirim sama sekali. */
    var reasoningEffort: String?
        get() = prefs.getString(KEY_REASONING_EFFORT, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_REASONING_EFFORT, value?.trim()?.takeIf { it.isNotBlank() }).apply()
        }

    /** 0 berarti tidak dipakai. */
    var maxTokens: Int
        get() = prefs.getInt(KEY_MAX_TOKENS, 0)
        set(value) {
            prefs.edit().putInt(KEY_MAX_TOKENS, value.coerceAtLeast(0)).apply()
        }

    val isConfigured: Boolean
        get() = !baseUrl.isNullOrBlank() && !apiKey.isNullOrBlank()

    fun clear() {
        prefs.edit()
            .remove(KEY_BASE_URL)
            .remove(KEY_MODEL)
            .remove(KEY_SYSTEM_PROMPT)
            .remove(KEY_REASONING_EFFORT)
            .remove(KEY_MAX_TOKENS)
            .apply()
        secure.remove(KEY_API_KEY)
    }

    companion object {
        private const val PREFS_NAME = "nova_settings"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "default_model"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_REASONING_EFFORT = "reasoning_effort"
        private const val KEY_MAX_TOKENS = "max_tokens"
    }
}
