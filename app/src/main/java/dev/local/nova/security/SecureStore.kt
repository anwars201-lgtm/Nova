package dev.local.nova.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Penyimpanan rahasia: token dan API key.
 *
 * Kunci AES-256 dibuat di AndroidKeyStore dan tidak bisa diekspor. Yang tersimpan di
 * SharedPreferences hanyalah `IV (12 byte) + ciphertext` dalam Base64. Artinya membaca file
 * prefs tidak memberi apa pun tanpa keystore perangkat ini (D6).
 *
 * Konsekuensi yang diterima: uninstall menghapus kunci keystore, sehingga nilai lama tidak
 * bisa didekripsi lagi dan pengguna harus login ulang.
 */
class SecureStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun put(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = iv + encrypted
        prefs.edit().putString(key, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun get(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return try {
            val packed = Base64.decode(stored, Base64.NO_WRAP)
            require(packed.size > IV_LENGTH) { "ciphertext terlalu pendek" }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_BITS, packed, 0, IV_LENGTH),
            )
            val plain = cipher.doFinal(packed, IV_LENGTH, packed.size - IV_LENGTH)
            String(plain, Charsets.UTF_8)
        } catch (t: Throwable) {
            // Kunci berubah (uninstall/reinstall) atau data rusak: buang agar tidak
            // menyisakan nilai yang tak bisa dipakai selamanya.
            prefs.edit().remove(key).apply()
            null
        }
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean = prefs.contains(key)

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "nova_secure"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nova_cred_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
    }
}
