package com.whisper.android.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whisper.android.WhisperApplication
import com.whisper.android.util.KeyUtils
import com.whisper.android.util.KeypairResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OnboardingTab { IMPORT, GENERATE }

data class OnboardingUiState(
    val tab: OnboardingTab = OnboardingTab.IMPORT,
    val importNsecInput: String = "",
    val generatedNsec: String? = null,
    val generatedNpub: String? = null,
    val error: String? = null,
    val navigateToFeed: Boolean = false,
)

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val PREFS_NAME = "whisper_secure_prefs"
        const val KEY_NSEC = "nsec"
        const val KEY_HEX_PUBKEY = "hex_pubkey"
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var pendingKeypair: KeypairResult? = null

    fun selectTab(tab: OnboardingTab) {
        _uiState.update { it.copy(tab = tab, error = null) }
    }

    fun onNsecInputChanged(value: String) {
        _uiState.update { it.copy(importNsecInput = value, error = null) }
    }

    fun importNsec(nsec: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val hexPrivKey = KeyUtils.nsecToHex(nsec.trim())
            if (hexPrivKey == null) {
                _uiState.update { it.copy(error = "Invalid nsec key. Please check and try again.") }
                return@launch
            }
            val hexPubKey = KeyUtils.deriveHexPubKey(hexPrivKey)
            saveToEncryptedPrefs(nsec.trim(), hexPubKey)
            _uiState.update { it.copy(navigateToFeed = true) }
        }
    }

    fun generateKeypair() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = KeyUtils.generateKeypair()
            pendingKeypair = result
            _uiState.update {
                it.copy(
                    generatedNsec = result.nsec,
                    generatedNpub = result.npub,
                    error = null,
                )
            }
        }
    }

    fun confirmKeypair() {
        val keypair = pendingKeypair ?: return
        viewModelScope.launch(Dispatchers.IO) {
            saveToEncryptedPrefs(keypair.nsec, keypair.hexPubKey)
            _uiState.update { it.copy(navigateToFeed = true) }
        }
    }

    private suspend fun saveToEncryptedPrefs(nsec: String, hexPubKey: String) =
        withContext(Dispatchers.IO) {
            getApplication<WhisperApplication>().securePrefs.edit()
                .putString(KEY_NSEC, nsec)
                .putString(KEY_HEX_PUBKEY, hexPubKey)
                .apply()
        }
}
