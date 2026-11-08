package com.huatafac.sosprof.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileTest {
    @Test
    fun newProfile_hasNoSampleData() {
        val profile = UserProfile(email = "prof@escola.pt")

        assertEquals("", profile.fullName)
        assertEquals("", profile.quote)
        assertEquals("", profile.gender)
        assertEquals("", profile.locality)
        assertEquals("", profile.educationLevel)
        assertEquals("", profile.subjects)
        assertEquals("", profile.county)
        assertEquals("", profile.serviceYears)
        assertEquals("", profile.professionalSituation)
        assertEquals("", profile.interestAreas)
        assertEquals("", profile.citizenCard)
        assertEquals("", profile.iban)
        assertEquals("", profile.profileImageUri)
        assertEquals("prof@escola.pt", profile.email)
    }

    @Test
    fun firebaseMap_hasEveryProfileField() {
        val campos = UserProfile::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals(campos, UserProfile().toFirebaseMap().keys)
    }
}
