package com.example.util

import android.content.Context
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReceiptImageStyle(val prefValue: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPrefValue(raw: String?): ReceiptImageStyle {
            return if (raw.equals(DARK.prefValue, ignoreCase = true)) DARK else LIGHT
        }
    }
}

class StockPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _photoStorageMode = MutableStateFlow(loadPhotoStorageMode())
    val photoStorageMode: StateFlow<PhotoStorageMode> = _photoStorageMode.asStateFlow()

    private val _receiptImageStyle = MutableStateFlow(loadReceiptImageStyle())
    val receiptImageStyle: StateFlow<ReceiptImageStyle> = _receiptImageStyle.asStateFlow()

    private val _hasOwnerPin = MutableStateFlow(hasOwnerPin())
    val hasOwnerPinFlow: StateFlow<Boolean> = _hasOwnerPin.asStateFlow()

    private val _lastUsedAccountId = MutableStateFlow(getLastUsedAccountId())
    val lastUsedAccountIdFlow: StateFlow<String> = _lastUsedAccountId.asStateFlow()

    private fun loadPhotoStorageMode(): PhotoStorageMode {
        val raw = prefs.getString(KEY_PHOTO_STORAGE_MODE, PhotoStorageMode.SYNC.prefValue)
        return PhotoStorageMode.entries.firstOrNull { it.prefValue == raw } ?: PhotoStorageMode.SYNC
    }

    fun getPhotoStorageMode(): PhotoStorageMode = _photoStorageMode.value

    fun setPhotoStorageMode(mode: PhotoStorageMode) {
        prefs.edit().putString(KEY_PHOTO_STORAGE_MODE, mode.prefValue).apply()
        _photoStorageMode.value = mode
    }

    private fun loadReceiptImageStyle(): ReceiptImageStyle {
        val raw = prefs.getString(KEY_RECEIPT_IMAGE_STYLE, ReceiptImageStyle.LIGHT.prefValue)
        return ReceiptImageStyle.fromPrefValue(raw)
    }

    fun getReceiptImageStyle(): ReceiptImageStyle = _receiptImageStyle.value

    fun setReceiptImageStyle(style: ReceiptImageStyle) {
        prefs.edit().putString(KEY_RECEIPT_IMAGE_STYLE, style.prefValue).apply()
        _receiptImageStyle.value = style
    }

    fun getLastUsedAccountId(): String =
        prefs.getString(KEY_LAST_ACCOUNT_ID, "cash_in_hand").orEmpty().ifBlank { "cash_in_hand" }

    fun setLastUsedAccountId(accountId: String) {
        val clean = accountId.trim().ifBlank { "cash_in_hand" }
        prefs.edit().putString(KEY_LAST_ACCOUNT_ID, clean).apply()
        _lastUsedAccountId.value = clean
    }

    fun hasOwnerPin(): Boolean = !prefs.getString(KEY_OWNER_PIN_HASH, null).isNullOrBlank()

    fun setOwnerPin(pin: String) {
        require(isValidPinFormat(pin)) { "PIN must be 4 to 6 digits" }
        prefs.edit().putString(KEY_OWNER_PIN_HASH, hashPin(pin)).apply()
        _hasOwnerPin.value = true
    }

    fun verifyOwnerPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_OWNER_PIN_HASH, null) ?: return false
        return saved == hashPin(pin)
    }

    fun changeOwnerPin(oldPin: String, newPin: String): Boolean {
        if (!verifyOwnerPin(oldPin)) return false
        if (!isValidPinFormat(newPin)) return false
        setOwnerPin(newPin)
        return true
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("storeos_pin_salt:$pin").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "storeos_stock_prefs"
        private const val KEY_PHOTO_STORAGE_MODE = "photo_storage_mode"
        private const val KEY_RECEIPT_IMAGE_STYLE = "receipt_image_style"
        private const val KEY_LAST_ACCOUNT_ID = "last_used_account_id"
        private const val KEY_OWNER_PIN_HASH = "owner_pin_hash"

        fun isValidPinFormat(pin: String): Boolean =
            pin.length in 4..6 && pin.all { it.isDigit() }
    }
}
