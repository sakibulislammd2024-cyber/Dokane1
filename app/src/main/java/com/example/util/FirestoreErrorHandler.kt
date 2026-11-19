package com.example.util

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

object FirestoreErrorHandler {

    fun handleFirestoreError(
        exception: Throwable,
        operationType: OperationType,
        path: String?
    ): String {
        val causes = generateSequence(exception) { it.cause }
        val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: exception

        val currentUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

        val providerInfoList = currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("email", provider.email)
            }
        } ?: emptyList()

        val authInfoJson = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", JSONArray(providerInfoList))
        }

        val errorInfoJson = JSONObject().apply {
            put("error", rootError.message ?: rootError.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfoJson)
        }

        val jsonString = errorInfoJson.toString()
        runCatching { Log.e("FirestoreError", "Firestore Error: $jsonString") }
        return jsonString
    }

    fun isPermissionDenied(exception: Throwable): Boolean {
        val causes = generateSequence(exception) { it.cause }
        val fsEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return true
        if (causes.any { it is SecurityException }) return true
        val msg = causes.mapNotNull { it.message }.joinToString(" ")
        return msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true)
    }

    fun isNetworkError(exception: Throwable): Boolean {
        val causes = generateSequence(exception) { it.cause }
        val fsEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fsEx?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            fsEx?.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        ) {
            return true
        }
        return causes.any {
            it is FirebaseNetworkException ||
                it is UnknownHostException ||
                it is SocketTimeoutException ||
                it is IOException
        }
    }

    fun containsRawTechnicalDetails(message: String?): Boolean {
        if (message.isNullOrBlank()) return true
        val blocked = listOf(
            "PERMISSION_DENIED",
            "Missing or insufficient permissions",
            "FAILED_PRECONDITION",
            "UNAUTHENTICATED",
            "UNAVAILABLE",
            "DEADLINE_EXCEEDED",
            "FirebaseFirestoreException",
            "FirebaseAuthException",
            "GetCredential",
            "io.grpc",
            "com.google.",
            "Status{code=",
            "Exception:"
        )
        return blocked.any { message.contains(it, ignoreCase = true) }
    }

    fun toUserFriendlyMessage(
        exception: Throwable,
        lang: AppLanguage,
        fallbackEn: String = "Something went wrong. Please try again.",
        fallbackBn: String = "কিছু একটা সমস্যা হয়েছে। আবার চেষ্টা করুন।"
    ): String {
        val causes = generateSequence(exception) { it.cause }.toList()
        val fsEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fsEx != null) {
            return when (fsEx.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    if (lang == AppLanguage.BN) {
                        "এই কাজটি করার অনুমতি নেই বা দোকানের তথ্য লোড হতে পারেনি।"
                    } else {
                        "You don't have permission for this action."
                    }
                }
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                    if (lang == AppLanguage.BN) {
                        "চালিয়ে যেতে আবার সাইন ইন করুন।"
                    } else {
                        "Please sign in again to continue."
                    }
                }
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                    if (lang == AppLanguage.BN) {
                        "ইন্টারনেট সংযোগে সমস্যা হয়েছে। একটু পরে আবার চেষ্টা করুন।"
                    } else {
                        "Network issue. Please check your connection and try again."
                    }
                }
                FirebaseFirestoreException.Code.NOT_FOUND -> {
                    if (lang == AppLanguage.BN) {
                        "প্রয়োজনীয় তথ্য পাওয়া যায়নি।"
                    } else {
                        "Requested record could not be found."
                    }
                }
                else -> if (lang == AppLanguage.BN) fallbackBn else fallbackEn
            }
        }

        val authEx = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()
        if (authEx != null) {
            return when (authEx) {
                is FirebaseAuthInvalidCredentialsException -> {
                    if (lang == AppLanguage.BN) {
                        "ইমেইল বা পাসওয়ার্ড সঠিক নয়।"
                    } else {
                        "Invalid email or password."
                    }
                }
                is FirebaseAuthInvalidUserException -> {
                    if (lang == AppLanguage.BN) {
                        "এই ইমেইল দিয়ে কোনো অ্যাকাউন্ট পাওয়া যায়নি।"
                    } else {
                        "No account found with this email."
                    }
                }
                is FirebaseAuthUserCollisionException -> {
                    if (lang == AppLanguage.BN) {
                        "এই ইমেইল দিয়ে আগেই অ্যাকাউন্ট তৈরি করা আছে।"
                    } else {
                        "An account with this email already exists."
                    }
                }
                else -> if (lang == AppLanguage.BN) fallbackBn else fallbackEn
            }
        }

        if (isPermissionDenied(exception)) {
            return if (lang == AppLanguage.BN) {
                "এই কাজটি করার অনুমতি নেই।"
            } else {
                "You don't have permission for this action."
            }
        }

        if (isNetworkError(exception)) {
            return if (lang == AppLanguage.BN) {
                "ইন্টারনেট সংযোগে সমস্যা হয়েছে। একটু পরে আবার চেষ্টা করুন।"
            } else {
                "Network issue. Please check your connection and try again."
            }
        }

        // Preserve deliberate, user-facing validation / domain messages from ShopLocale
        if (exception !is FirebaseException && !containsRawTechnicalDetails(exception.message)) {
            val clean = exception.message?.trim()
            if (!clean.isNullOrBlank()) return clean
        }

        return if (lang == AppLanguage.BN) fallbackBn else fallbackEn
    }
}
