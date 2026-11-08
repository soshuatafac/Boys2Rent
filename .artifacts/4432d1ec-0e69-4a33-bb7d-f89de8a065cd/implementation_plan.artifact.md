# Implementation Plan: Adding Firebase and Google Login to SOS Prof

This implementation plan details the steps required to integrate Firebase Authentication with Google Sign-In into the SOS Prof application, enabling secure user authentication using Credential Manager and Firebase Auth.

## User Review Required

> [!IMPORTANT]
> Google Sign-In requires a configured `google-services.json` file (which is already present in `app/google-services.json`) and SHA-1 signing certificate fingerprint registered in the Firebase Console for Google authentication to succeed on physical/virtual devices.

## Open Questions

- None. Firebase BOM and Firebase Auth are already declared in `app/build.gradle.kts`. We will add Credential Manager dependencies for modern Google Sign-In.

## Proposed Changes

### Dependencies & Build Configuration

#### [MODIFY] [build.gradle.kts](file:///C:/Users/carlo/AndroidStudioProjects/SOSProf/app/build.gradle.kts)
- Add Jetpack Credential Manager and Google ID dependencies:
  - `androidx.credentials:credentials`
  - `androidx.credentials:credentials-play-services-auth`
  - `com.google.libraries.identity.googleid:googleid`

---

### Authentication Architecture (`com.huatafac.sosprof.auth`)

#### [NEW] [AuthRepository.kt](file:///C:/Users/carlo/AndroidStudioProjects/SOSProf/app/src/main/java/com/huatafac/sosprof/auth/AuthRepository.kt)
- Encapsulates Firebase Auth operations:
  - Check current user state (`FirebaseUser?`)
  - Sign in with Google credential / ID token using Firebase `GoogleAuthProvider.getCredential`
  - Sign out method

#### [NEW] [AuthViewModel.kt](file:///C:/Users/carlo/AndroidStudioProjects/SOSProf/app/src/main/java/com/huatafac/sosprof/auth/AuthViewModel.kt)
- Manages authentication UI state (`AuthState`: Unauthenticated, Loading, Authenticated, Error)
- Exposes StateFlow for current user and error messages
- Handles Google Sign-In request coordination with Credential Manager API

---

### UI & Navigation (`com.huatafac.sosprof.ui.login` and `MainActivity`)

#### [NEW] [LoginScreen.kt](file:///C:/Users/carlo/AndroidStudioProjects/SOSProf/app/src/main/java/com/huatafac/sosprof/ui/login/LoginScreen.kt)
- Material 3 compliant login screen featuring:
  - App branding / logo
  - Google Sign-In button with loading indicator and error handling
  - Edge-to-edge support and M3 dynamic color integration

#### [MODIFY] [MainActivity.kt](file:///C:/Users/carlo/AndroidStudioProjects/SOSProf/app/src/main/java/com/huatafac/sosprof/MainActivity.kt)
- Observe `AuthViewModel` state
- Conditionally render `LoginScreen` if user is unauthenticated, or main dashboard if authenticated
- Manage credential manager launcher / intent handling for Google Sign-In

---

## Verification Plan

### Automated Tests
- Run unit tests for `AuthViewModel` using coroutines test rule.
- Run gradle build: `./gradlew :app:assembleDebug`

### Manual Verification
- Deploy app to an emulator or device.
- Verify that unauthenticated users are presented with `LoginScreen`.
- Tap Google Sign-In button, select a Google account via Credential Manager, and verify successful authentication and transition to the main dashboard.
- Test sign-out flow and error handling.
