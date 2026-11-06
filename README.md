<h1 align="center">🏥 WardCare</h1>
<p align="center"><b>A bedside assistant for ward nurses, built to keep working when the Wi-Fi doesn't.</b></p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20FCM-FFCA28?logo=firebase&logoColor=black" alt="Firebase">
  <img src="https://img.shields.io/badge/version-1.0.0-2150A2" alt="Version">
</p>

# WardCare

WardCare is a staff-facing Android app (Kotlin) for tracking patients and medication administration on a hospital ward. Nurses log doses as given, missed or delayed, even when the ward Wi-Fi is down, and the app syncs to the cloud when the connection returns. It is **not** a clinical decision-support tool (no dosage advice or drug-interaction checking). All patient data in the demo is fictional.

**Team (Group 19):** Tiyase Nteseng (ST10445279), Mabelebele Maselelo (ST10436039)

## 1. Purpose of the app

- **Problem:** paper-based tracking is slow and error-prone, nurses carry heavy workloads, and hospital Wi-Fi is unreliable.
- **Vision:** a digital bedside assistant for patient tracking and offline-first medication logging with cloud sync.
- **Users and value:** nurses, doctors and supervisors. Logging never depends on connectivity, nothing is lost offline, and the UI is available in English, isiZulu and Afrikaans.

## 2. Features (mapped to the brief)

| Requirement | How WardCare does it |
|---|---|
| Register / log in with encrypted password | Firebase Authentication (email + password). The app never stores the password; Firebase keeps only a salted hash. Passwords are sent over TLS. |
| Single sign-on | Google Sign-In, exchanged for a Firebase session |
| Settings | Profile, ward, language and notification toggle (Settings screen) |
| REST API + database | Cloud Firestore accessed through its **REST API** with Retrofit (users, patients, medicationLogs) |
| Offline mode with sync | Room is the local source of truth; queued logs sync in atomic batches of 20, automatically on reconnect |
| Real-time notifications | Firebase Cloud Messaging push (topic `ward-alerts`) plus an in-app notification list |
| Multi-language | English, isiZulu, Afrikaans, switched at runtime with `AppCompatDelegate.setApplicationLocales` |
| Invalid input handling | `InputValidator` on all forms; errors are shown, never crashes |

## 3. Architecture

MVVM + Repository pattern with Room as the single source of truth.

```
UI (Fragments, ViewBinding, Navigation) -> ViewModels -> Repositories -> Room (local) + Retrofit (Firestore REST)
```

**Offline sync:** a log is saved to Room with `isSyncPending = true` -> `ApiService.commit` sends up to 20 logs per atomic batch -> only after HTTP 200 is the log marked synced. Failures keep the log queued, and a network callback re-triggers sync when the device reconnects.

**Security:** every REST call carries the user's Firebase ID token and Firestore security rules only allow signed-in staff (a user can only read or write their own profile and push token). Logs avoid passwords, tokens and patient details.

## 4. Testing and CI

- Unit tests (`app/src/test`): input validation, Firestore JSON mapping, and the offline queue/sync logic (offline stays queued, server error stays queued, batches of 20).
- **GitHub Actions** Unit tests and debug build
The job was not started because your account is locked due to a billing issue.

## 5. Logging

`android.util.Log` is used across auth, repositories, sync, the database seeding and push handling. In Logcat filter by tags such as `AuthViewModel`, `MedicationRepository`, `UserRepository`, `PatientRepository`, `AppDatabase`, `MainActivity` and `WardCareMessaging`.

## 6. Tech stack

Kotlin, AndroidX (ViewBinding, Navigation, Lifecycle), Material 3, Room, Retrofit + OkHttp + Gson, Kotlin Coroutines, Firebase Auth, Firestore (REST), Firebase Cloud Messaging, Google Sign-In, JUnit 4, GitHub Actions. Min SDK 24, target SDK 35.

## 7. Running the app

1. Clone the repo and open it in Android Studio (JDK 17, AGP 8.7). `app/google-services.json` is included.
2. Run on an emulator with Google Play services or a phone (API 24+).
3. For Google sign-in on your own machine, add your debug SHA-1 to the Firebase project.
4. To demo push notifications: Firebase console -> Messaging -> new campaign -> Notification -> target topic `ward-alerts`.

## 8. Known limitations

- Any signed-in staff member can read and write patient and medication data (no per-ward roles yet).
- Push alerts are sent from the Firebase console, because there is no server of our own to send them automatically.
- Notifications that arrive while the app is in the background are shown by the system tray but only added to the in-app list when received in the foreground.

## 9. References

- Firestore REST API: https://firebase.google.com/docs/firestore/use-rest-api
- Firebase Auth on Android: https://firebase.google.com/docs/auth/android/start
- Google Sign-In with Firebase: https://firebase.google.com/docs/auth/android/google-signin
- Firebase Cloud Messaging: https://firebase.google.com/docs/cloud-messaging/android/receive
- Android Room: https://developer.android.com/training/data-storage/room
