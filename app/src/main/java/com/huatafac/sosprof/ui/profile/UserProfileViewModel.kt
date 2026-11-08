package com.huatafac.sosprof.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huatafac.sosprof.data.FotosRepository
import com.huatafac.sosprof.data.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.huatafac.sosprof.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UserProfileState {
    data object Loading : UserProfileState
    data class Success(val profile: UserProfile, val successMessage: String? = null) : UserProfileState
    data class Error(val message: String) : UserProfileState
}

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserProfileRepository = UserProfileRepository()
    private val fotos = FotosRepository()

    /** Pasta privada da app com a cópia local da foto do perfil ("profile_{versão}.jpg"). */
    private val pastaFoto = File(application.filesDir, "foto_perfil")

    /** Versão (millis) da foto do perfil que está na nuvem; 0 = não está. */
    private var fotoPerfilAtualizada = 0L

    private val _uiState = MutableStateFlow<UserProfileState>(UserProfileState.Loading)
    val uiState: StateFlow<UserProfileState> = _uiState.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _title = MutableStateFlow("Professor")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _quote = MutableStateFlow("")
    val quote: StateFlow<String> = _quote.asStateFlow()

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl: StateFlow<String> = _avatarUrl.asStateFlow()

    private val _profileImageUri = MutableStateFlow("")
    val profileImageUri: StateFlow<String> = _profileImageUri.asStateFlow()

    private val _birthDate = MutableStateFlow("")
    val birthDate: StateFlow<String> = _birthDate.asStateFlow()

    private val _nif = MutableStateFlow("")
    val nif: StateFlow<String> = _nif.asStateFlow()

    private val _citizenCard = MutableStateFlow("")
    val citizenCard: StateFlow<String> = _citizenCard.asStateFlow()

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _postalCode = MutableStateFlow("")
    val postalCode: StateFlow<String> = _postalCode.asStateFlow()

    private val _locality = MutableStateFlow("")
    val locality: StateFlow<String> = _locality.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _iban = MutableStateFlow("")
    val iban: StateFlow<String> = _iban.asStateFlow()

    private val _otherContacts = MutableStateFlow("")
    val otherContacts: StateFlow<String> = _otherContacts.asStateFlow()

    private val _educationLevel = MutableStateFlow("")
    val educationLevel: StateFlow<String> = _educationLevel.asStateFlow()

    private val _subjects = MutableStateFlow("")
    val subjects: StateFlow<String> = _subjects.asStateFlow()

    private val _schoolGroup = MutableStateFlow("")
    val schoolGroup: StateFlow<String> = _schoolGroup.asStateFlow()

    private val _county = MutableStateFlow("")
    val county: StateFlow<String> = _county.asStateFlow()

    private val _serviceYears = MutableStateFlow("")
    val serviceYears: StateFlow<String> = _serviceYears.asStateFlow()

    private val _professionalSituation = MutableStateFlow("")
    val professionalSituation: StateFlow<String> = _professionalSituation.asStateFlow()

    private val _interestAreas = MutableStateFlow("")
    val interestAreas: StateFlow<String> = _interestAreas.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _acceptedPrivacyTerms = MutableStateFlow(false)
    val acceptedPrivacyTerms: StateFlow<Boolean> = _acceptedPrivacyTerms.asStateFlow()

    fun loadProfile(uid: String, defaultEmail: String = "") {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UserProfileState.Loading
            val result = repository.getUserProfile(uid)
            if (result.isSuccess) {
                val profile = result.getOrNull() ?: UserProfile(email = defaultEmail)
                
                val nameToUse = when {
                    profile.fullName.isNotBlank() -> profile.fullName
                    profile.firstName.isNotBlank() -> if (profile.lastName.isNotBlank()) "${profile.firstName} ${profile.lastName}" else profile.firstName
                    else -> ""
                }

                _fullName.value = nameToUse
                _title.value = if (profile.title.isNotBlank()) profile.title else "Professor"
                _quote.value = profile.quote
                _avatarUrl.value = profile.avatarUrl
                _profileImageUri.value = profile.profileImageUri
                fotoPerfilAtualizada = profile.fotoPerfilAtualizada
                sincronizarFotoPerfil(uid, profile)
                _birthDate.value = profile.birthDate
                _nif.value = profile.nif
                _citizenCard.value = profile.citizenCard
                _gender.value = profile.gender
                _address.value = profile.address
                _postalCode.value = profile.postalCode
                _locality.value = profile.locality
                _phone.value = profile.phone
                _email.value = if (profile.email.isBlank()) defaultEmail else profile.email
                _iban.value = profile.iban
                _otherContacts.value = profile.otherContacts
                _educationLevel.value = profile.educationLevel
                _subjects.value = profile.subjects
                _schoolGroup.value = profile.schoolGroup
                _county.value = profile.county
                _serviceYears.value = profile.serviceYears
                _professionalSituation.value = profile.professionalSituation
                _interestAreas.value = profile.interestAreas
                _notificationsEnabled.value = profile.notificationsEnabled
                _acceptedPrivacyTerms.value = profile.acceptedPrivacyTerms

                _uiState.value = UserProfileState.Success(profile)
            } else {
                _uiState.value = UserProfileState.Error(result.exceptionOrNull()?.localizedMessage ?: "Failed to load profile")
            }
        }
    }

    fun onFullNameChanged(value: String) { _fullName.value = value }
    fun onTitleChanged(value: String) { _title.value = value }
    fun onQuoteChanged(value: String) { _quote.value = value }
    fun onBirthDateChanged(value: String) { _birthDate.value = value }
    fun onNifChanged(value: String) { _nif.value = value }
    fun onCitizenCardChanged(value: String) { _citizenCard.value = value }
    fun onGenderChanged(value: String) { _gender.value = value }
    fun onAddressChanged(value: String) { _address.value = value }
    fun onPostalCodeChanged(value: String) { _postalCode.value = value }
    fun onLocalityChanged(value: String) { _locality.value = value }
    fun onPhoneChanged(value: String) { _phone.value = value }
    fun onEmailChanged(value: String) { _email.value = value }
    fun onIbanChanged(value: String) { _iban.value = value }
    fun onOtherContactsChanged(value: String) { _otherContacts.value = value }
    fun onEducationLevelChanged(value: String) { _educationLevel.value = value }
    fun onSubjectsChanged(value: String) { _subjects.value = value }
    fun onSchoolGroupChanged(value: String) { _schoolGroup.value = value }
    fun onCountyChanged(value: String) { _county.value = value }
    fun onServiceYearsChanged(value: String) { _serviceYears.value = value }
    fun onProfessionalSituationChanged(value: String) { _professionalSituation.value = value }
    fun onInterestAreasChanged(value: String) { _interestAreas.value = value }
    fun onNotificationsEnabledChanged(value: Boolean) { _notificationsEnabled.value = value }
    fun onAcceptedPrivacyTermsChanged(value: Boolean) { _acceptedPrivacyTerms.value = value }
    fun onProfileImageUriChanged(value: String) { _profileImageUri.value = value }

    /**
     * Garante a foto do perfil neste telemóvel: se a versão da nuvem não tem cópia local, descarrega-a;
     * se a foto só existe no telemóvel (versões antigas da app), envia-a para a nuvem.
     */
    private fun sincronizarFotoPerfil(uid: String, profile: UserProfile) {
        viewModelScope.launch {
            val versao = profile.fotoPerfilAtualizada
            if (versao > 0) {
                val local = File(pastaFoto, "profile_$versao.jpg")
                if (local.exists()) {
                    _profileImageUri.value = Uri.fromFile(local).toString()
                    return@launch
                }
                val nuvem = fotos.descarregarPerfil(uid) ?: return@launch
                val uri = withContext(Dispatchers.IO) {
                    runCatching {
                        pastaFoto.mkdirs()
                        pastaFoto.listFiles()?.forEach { it.delete() }
                        local.writeBytes(nuvem.jpeg)
                        Uri.fromFile(local).toString()
                    }.getOrNull()
                } ?: return@launch
                // Só substitui se, entretanto, não se tirou outra foto
                if (_profileImageUri.value == profile.profileImageUri) _profileImageUri.value = uri
            } else {
                val ficheiro = ficheiroLocal(profile.profileImageUri) ?: return@launch
                val v = enviarFotoPerfil(uid, ficheiro)
                if (v > 0) repository.guardarFotoPerfil(uid, Uri.fromFile(ficheiro).toString(), v)
            }
        }
    }

    /** Ficheiro local de um URI file://, se existir. */
    private fun ficheiroLocal(uri: String): File? =
        uri.takeIf { it.startsWith("file:") }?.let { Uri.parse(it).path }?.let { File(it) }?.takeIf { it.exists() }

    /** Envia a foto para a nuvem. Devolve a versão gravada (0 se o envio foi recusado). */
    private suspend fun enviarFotoPerfil(uid: String, ficheiro: File): Long {
        val versao = ficheiro.nameWithoutExtension.substringAfter('_', "").toLongOrNull()?.takeIf { it > 0 }
            ?: System.currentTimeMillis()
        val bytes = withContext(Dispatchers.IO) { runCatching { ficheiro.readBytes() }.getOrNull() } ?: return 0L
        val envio = fotos.enviarPerfil(uid, bytes, versao)
        if (envio == FotosRepository.Envio.FALHOU) return 0L
        fotoPerfilAtualizada = versao
        return versao
    }

    fun saveProfile(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            // Foto nova (tirada nesta edição): vai para a nuvem e apagam-se as cópias antigas do telemóvel
            val ficheiroFoto = ficheiroLocal(_profileImageUri.value.trim())
            val versaoNaFoto = ficheiroFoto?.nameWithoutExtension?.substringAfter('_', "")?.toLongOrNull()
            if (ficheiroFoto != null && versaoNaFoto != fotoPerfilAtualizada) {
                if (enviarFotoPerfil(uid, ficheiroFoto) == 0L) fotoPerfilAtualizada = 0L
                withContext(Dispatchers.IO) {
                    runCatching { pastaFoto.listFiles()?.filter { it != ficheiroFoto }?.forEach { it.delete() } }
                }
            }
            val currentProfile = UserProfile(
                fullName = _fullName.value.trim(),
                title = _title.value.trim(),
                quote = _quote.value.trim(),
                avatarUrl = _avatarUrl.value.trim(),
                profileImageUri = _profileImageUri.value.trim(),
                fotoPerfilAtualizada = fotoPerfilAtualizada,
                birthDate = _birthDate.value.trim(),
                nif = _nif.value.trim(),
                citizenCard = _citizenCard.value.trim(),
                gender = _gender.value.trim(),
                address = _address.value.trim(),
                postalCode = _postalCode.value.trim(),
                locality = _locality.value.trim(),
                phone = _phone.value.trim(),
                email = _email.value.trim(),
                iban = _iban.value.trim(),
                otherContacts = _otherContacts.value.trim(),
                educationLevel = _educationLevel.value.trim(),
                subjects = _subjects.value.trim(),
                schoolGroup = _schoolGroup.value.trim(),
                county = _county.value.trim(),
                serviceYears = _serviceYears.value.trim(),
                professionalSituation = _professionalSituation.value.trim(),
                interestAreas = _interestAreas.value.trim(),
                notificationsEnabled = _notificationsEnabled.value,
                acceptedPrivacyTerms = _acceptedPrivacyTerms.value
            )
            _uiState.value = UserProfileState.Loading
            val result = repository.saveUserProfile(uid, currentProfile)
            if (result.isSuccess) {
                _uiState.value = UserProfileState.Success(currentProfile, successMessage = "Perfil guardado com sucesso!")
            } else {
                _uiState.value = UserProfileState.Error(result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun clearMessage() {
        val currentState = _uiState.value
        if (currentState is UserProfileState.Success && currentState.successMessage != null) {
            _uiState.value = currentState.copy(successMessage = null)
        }
    }
}
