package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.InviteCodeConfig
import com.example.data.model.UserProfile
import com.example.data.repository.StoreRepository
import com.example.domain.ShopType
import com.example.domain.ShopTypeCatalog
import com.example.util.AppLanguage
import com.example.util.FirestoreErrorHandler
import com.example.util.ShopLocale
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object Unauthenticated : AuthUiState()
    data class AuthenticatedNoStore(val user: FirebaseUser) : AuthUiState()
    data class AuthenticatedWithStore(
        val user: FirebaseUser,
        val storeId: String,
        val userProfile: UserProfile
    ) : AuthUiState()
    data class DemoMode(val storeId: String = "demo_store") : AuthUiState()
}

class AuthViewModel(
    private val repository: StoreRepository
) : ViewModel() {

    private val auth: FirebaseAuth? = try {
        Firebase.auth
    } catch (e: Exception) {
        Log.w("AuthViewModel", "FirebaseAuth unavailable: ${e.message}")
        null
    }

    val currentLanguage: StateFlow<AppLanguage> = repository.currentLanguage

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var profileJob: Job? = null
    private var boundUserId: String? = null
    private var isInDemoMode = false

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (isInDemoMode) return@AuthStateListener
        val user = firebaseAuth.currentUser
        if (user == null) {
            boundUserId = null
            profileJob?.cancel()
            _uiState.value = AuthUiState.Unauthenticated
        } else {
            observeUserStoreBinding(user)
        }
    }

    init {
        val currentAuth = auth
        if (currentAuth == null) {
            _uiState.value = AuthUiState.Unauthenticated
        } else {
            val existingUser = currentAuth.currentUser
            if (existingUser == null) {
                _uiState.value = AuthUiState.Unauthenticated
            } else {
                observeUserStoreBinding(existingUser)
            }
            currentAuth.addAuthStateListener(authStateListener)
        }
    }

    private fun observeUserStoreBinding(user: FirebaseUser) {
        if (profileJob?.isActive == true && boundUserId == user.uid) return
        val switchedAccount = boundUserId != user.uid
        boundUserId = user.uid
        if (switchedAccount) {
            clearMessages()
        }
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            repository.observeUserProfile(user.uid).collectLatest { profile ->
                if (isInDemoMode) return@collectLatest
                if (profile != null && profile.storeId.isNotBlank()) {
                    _uiState.value = AuthUiState.AuthenticatedWithStore(
                        user = user,
                        storeId = profile.storeId,
                        userProfile = profile
                    )
                } else {
                    _uiState.value = AuthUiState.AuthenticatedNoStore(user)
                }
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun toggleLanguage() {
        repository.toggleLanguage()
    }

    fun launchDemoMode(shopType: ShopType? = null) {
        isInDemoMode = true
        profileJob?.cancel()
        repository.configureDemoStore(shopType)
        clearMessages()
        _uiState.value = AuthUiState.DemoMode(storeId = "demo_store")
    }

    fun signInWithEmail(email: String, password: String) {
        val lang = currentLanguage.value
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "ইমেইল এবং পাসওয়ার্ড দিন"
            } else {
                "Please enter your email and password"
            }
            return
        }
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "অথেনটিকেশন সার্ভিস পাওয়া যায়নি"
            } else {
                "Authentication service is unavailable"
            }
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            clearMessages()
            try {
                isInDemoMode = false
                firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
                val user = firebaseAuth.currentUser
                if (user != null) {
                    observeUserStoreBinding(user)
                }
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = "Sign in failed. Please check your details and try again.",
                    fallbackBn = "সাইন ইন করা যায়নি। তথ্য যাচাই করে আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, confirmPassword: String) {
        val lang = currentLanguage.value
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.length < 6) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "সঠিক ইমেইল ও কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড দিন"
            } else {
                "Enter a valid email and a password of at least 6 characters"
            }
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "পাসওয়ার্ড দুটি মেলেনি"
            } else {
                "Passwords do not match"
            }
            return
        }
        val firebaseAuth = auth ?: return
        viewModelScope.launch {
            _isBusy.value = true
            clearMessages()
            try {
                isInDemoMode = false
                firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val user = firebaseAuth.currentUser
                if (user != null) {
                    repository.saveUserProfile(
                        UserProfile(
                            id = user.uid,
                            email = user.email ?: cleanEmail,
                            storeId = "",
                            role = "owner",
                            isSimpleMode = repository.getCachedUserSimpleMode(user.uid)
                        )
                    )
                    observeUserStoreBinding(user)
                }
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = "Could not create account. Please try again.",
                    fallbackBn = "অ্যাকাউন্ট তৈরি করা যায়নি। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val lang = currentLanguage.value
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "ইমেইল লিখুন"
            } else {
                "Please enter your email address"
            }
            return
        }
        val firebaseAuth = auth ?: return
        viewModelScope.launch {
            _isBusy.value = true
            clearMessages()
            try {
                firebaseAuth.sendPasswordResetEmail(cleanEmail).await()
                _successMessage.value = if (lang == AppLanguage.BN) {
                    "পাসওয়ার্ড রিসেট ইমেইল পাঠানো হয়েছে"
                } else {
                    "Password reset email sent to $cleanEmail"
                }
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = "Could not send password reset email.",
                    fallbackBn = "পাসওয়ার্ড রিসেট ইমেইল পাঠানো যায়নি।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        val lang = currentLanguage.value
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "অথেনটিকেশন সার্ভিস পাওয়া যায়নি"
            } else {
                "Authentication service is unavailable"
            }
            return
        }
        val serverClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }
        if (serverClientId.isBlank()) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "Google সাইন-ইন কনফিগার করা হয়নি"
            } else {
                "Google Sign-In client ID is not configured"
            }
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            clearMessages()
            try {
                val credentialManager = CredentialManager.create(context)
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val response = credentialManager.getCredential(context = context, request = request)
                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    isInDemoMode = false
                    firebaseAuth.signInWithCredential(authCredential).await()
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        runCatching { user.getIdToken(false).await() }
                        val existing = runCatching { repository.getUserProfileOnce(user.uid) }.getOrNull()
                        if (existing == null) {
                            runCatching {
                                repository.saveUserProfile(
                                    UserProfile(
                                        id = user.uid,
                                        email = user.email ?: "",
                                        storeId = "",
                                        role = "owner",
                                        isSimpleMode = repository.getCachedUserSimpleMode(user.uid)
                                    )
                                )
                            }.onFailure { err ->
                                Log.w("AuthViewModel", "Initial profile bootstrap deferred: ${err.message}")
                            }
                        }
                        clearMessages()
                        observeUserStoreBinding(user)
                    }
                } else {
                    _errorMessage.value = if (lang == AppLanguage.BN) {
                        "Google সাইন-ইন সম্পন্ন করা যায়নি। আবার চেষ্টা করুন।"
                    } else {
                        "Google Sign-In could not be completed. Please try again."
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("AuthViewModel", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            } catch (e: GetCredentialException) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = "Google Sign-In failed. Please try again.",
                    fallbackBn = "Google সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                )
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = "Google Sign-In failed. Please try again.",
                    fallbackBn = "Google সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun createStore(
        name: String,
        phone: String,
        address: String,
        shopType: ShopType?
    ) {
        val lang = currentLanguage.value
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _errorMessage.value = if (lang == AppLanguage.BN) {
                "দোকানের নাম দিন"
            } else {
                "Please enter your shop name"
            }
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            clearMessages()
            try {
                val categories = if (shopType != null) {
                    ShopTypeCatalog.startingCategories(shopType)
                } else {
                    emptyList()
                }
                val storeId = repository.createStore(
                    name = cleanName,
                    phone = phone.trim(),
                    address = address.trim(),
                    shopType = shopType?.id ?: "",
                    productCategories = categories
                )
                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    val profile = runCatching { repository.getUserProfileOnce(currentUser.uid) }.getOrNull()
                        ?: UserProfile(
                            id = currentUser.uid,
                            email = currentUser.email ?: "",
                            storeId = storeId,
                            role = "owner",
                            isSimpleMode = false
                        )
                    _uiState.value = AuthUiState.AuthenticatedWithStore(
                        user = currentUser,
                        storeId = storeId,
                        userProfile = profile.copy(storeId = storeId)
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = ShopLocale.get("err_create_store_failed", lang),
                    fallbackBn = ShopLocale.get("err_create_store_failed", lang)
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun joinStoreWithInvite(inviteCode: String) {
        val lang = currentLanguage.value
        val cleanCode = InviteCodeConfig.normalize(inviteCode)
        val user = auth?.currentUser
        if (user == null) {
            _errorMessage.value = ShopLocale.get("err_join_store_failed", lang)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            clearMessages()
            try {
                val result = repository.joinStoreWithInvite(
                    code = cleanCode,
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: ""
                )
                result.onSuccess { joinedStoreId ->
                    val profile = runCatching { repository.getUserProfileOnce(user.uid) }.getOrNull()
                        ?: UserProfile(
                            id = user.uid,
                            email = user.email ?: "",
                            storeId = joinedStoreId,
                            role = "staff",
                            isSimpleMode = false
                        )
                    _uiState.value = AuthUiState.AuthenticatedWithStore(
                        user = user,
                        storeId = joinedStoreId,
                        userProfile = profile.copy(storeId = joinedStoreId)
                    )
                }.onFailure { err ->
                    _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                        err,
                        lang,
                        fallbackEn = ShopLocale.get("err_join_store_failed", lang),
                        fallbackBn = ShopLocale.get("err_join_store_failed", lang)
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = ShopLocale.get("err_join_store_failed", lang),
                    fallbackBn = ShopLocale.get("err_join_store_failed", lang)
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            isInDemoMode = false
            boundUserId = null
            profileJob?.cancel()
            runCatching { auth?.signOut() }
            runCatching {
                CredentialManager.create(context).clearCredentialState(
                    androidx.credentials.ClearCredentialStateRequest()
                )
            }
            clearMessages()
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileJob?.cancel()
        runCatching { auth?.removeAuthStateListener(authStateListener) }
    }
}
