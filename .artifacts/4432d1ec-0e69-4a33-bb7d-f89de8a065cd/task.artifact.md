# Task List: Firebase Authentication, LoginScreen & User Profile Feature Implementation in SOS Prof

- `[x]` Add Credential Manager dependencies to `app/build.gradle.kts`
- `[x]` Implement `AuthRepository` for Firebase Auth (Google Sign-In, Email/Password sign-in, and account creation)
- `[x]` Implement `AuthViewModel` for authentication state management, email/password validation, and mode toggling
- `[x]` Implement `LoginScreen` Composable with Material 3 design, Email/Password inputs, sign-in/sign-up toggle, and Google Sign-In button matching reference design
- `[x]` Update `MainActivity` to wire up authentication state observation and conditional navigation
- `[x]` Implement `UserProfile` data model (`firstName`, `lastName`, `address`, `phone`, `email`)
- `[x]` Implement `UserProfileRepository` for Firebase Realtime Database integration (`users/{uid}`)
- `[x]` Implement `UserProfileViewModel` for managing profile state loading, editing, and saving
- `[x]` Implement `UserProfileScreen` Composable with Material 3 text fields, icons, snackbar feedback, and back navigation
- `[x]` Verify build and test compilation (`./gradlew :app:assembleDebug`)
