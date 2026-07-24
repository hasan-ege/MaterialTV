package com.hasanege.materialtv.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALIAS = "materialtv_secret_key"
    private const val KEY_STORE_PROVIDER = "AndroidKeyStore"

    private val keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER).apply {
        load(null)
    }

    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEY_STORE_PROVIDER
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encrypt(text: String): String {
        if (text.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted bytes: [IV_length (4 bytes)] + [IV] + [encrypted_bytes]
            val combined = ByteArray(4 + iv.size + encryptedBytes.size)
            combined[0] = (iv.size and 0xFF).toByte()
            System.arraycopy(iv, 0, combined, 4, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 4 + iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            if (combined.size < 5) return ""
            val ivSize = combined[0].toInt() and 0xFF
            if (ivSize <= 0 || combined.size < 4 + ivSize) return ""
            
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 4, iv, 0, ivSize)
            
            val encryptedBytesSize = combined.size - 4 - ivSize
            val encryptedBytes = ByteArray(encryptedBytesSize)
            System.arraycopy(combined, 4 + ivSize, encryptedBytes, 0, encryptedBytesSize)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
