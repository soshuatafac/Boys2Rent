package com.huatafac.sosprof.model

data class UserProfile(
    // Header / Profile Info
    val fullName: String = "",
    val title: String = "Professor",
    val quote: String = "",
    val avatarUrl: String = "",
    val profileImageUri: String = "",          // URI file:// da cópia da foto neste telemóvel
    val fotoPerfilAtualizada: Long = 0L,        // versão (millis) da foto na nuvem, em fotos/{uid}/perfil; 0 = não está

    // Section 1: Dados Pessoais
    val birthDate: String = "",
    val nif: String = "",
    val citizenCard: String = "",
    val gender: String = "",
    val address: String = "",
    val postalCode: String = "",
    val locality: String = "",

    // Section 2: Contacto
    val phone: String = "",
    val email: String = "",
    val iban: String = "",
    val otherContacts: String = "",

    // Section 3: Dados Profissionais
    val educationLevel: String = "",
    val subjects: String = "",
    val schoolGroup: String = "",
    val county: String = "",
    val serviceYears: String = "",
    val professionalSituation: String = "",

    // Section 4: Preferências
    val interestAreas: String = "",
    val notificationsEnabled: Boolean = true,

    // Section 5: Privacidade
    val acceptedPrivacyTerms: Boolean = false,

    // Legacy fields for backward compatibility
    val firstName: String = "",
    val lastName: String = ""
)

/**
 * Campos do perfil para gravar com updateChildren em users/{uid}.
 * Se acrescentares um campo ao UserProfile, acrescenta-o também aqui (o UserProfileTest verifica isso).
 */
fun UserProfile.toFirebaseMap(): Map<String, Any> = mapOf(
    "fullName" to fullName,
    "title" to title,
    "quote" to quote,
    "avatarUrl" to avatarUrl,
    "profileImageUri" to profileImageUri,
    "fotoPerfilAtualizada" to fotoPerfilAtualizada,
    "birthDate" to birthDate,
    "nif" to nif,
    "citizenCard" to citizenCard,
    "gender" to gender,
    "address" to address,
    "postalCode" to postalCode,
    "locality" to locality,
    "phone" to phone,
    "email" to email,
    "iban" to iban,
    "otherContacts" to otherContacts,
    "educationLevel" to educationLevel,
    "subjects" to subjects,
    "schoolGroup" to schoolGroup,
    "county" to county,
    "serviceYears" to serviceYears,
    "professionalSituation" to professionalSituation,
    "interestAreas" to interestAreas,
    "notificationsEnabled" to notificationsEnabled,
    "acceptedPrivacyTerms" to acceptedPrivacyTerms,
    "firstName" to firstName,
    "lastName" to lastName
)
