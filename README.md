# Firebase Example App

Пример Android-приложения (Kotlin + Jetpack Compose) для работы с Firebase.

## Что делает приложение

1. Пользователь входит по **email + паролю** (Firebase Authentication, Sign-in method
   "Email/Password") — с регистрацией.
2. После входа приложение читает из **Cloud Firestore** документ `users/<uid>` и показывает
   уникальную строку, привязанную к пользователю (поле `secret`).

Каждый пользователь получает строку, индивидуальную для него: документ определяется
по `FirebaseUser.uid` (уникален в рамках вашего проекта).

## Структура

```
app/src/main/java/ru/filantrop/firebaseexampleapp/
├── MainActivity.kt            # входная точка, переключение экранов по состоянию аутентификации
├── data/
│   ├── AuthRepository.kt      # обёртка над Firebase Auth (email, слушатель состояния)
│   └── UserDataRepository.kt  # чтение документа users/<uid> из Firestore
├── ui/
│   ├── MainViewModel.kt       # состояние приложения (аутентификация + строка), маппинг ошибок
│   ├── LoginScreen.kt         # экран входа
│   └── HomeScreen.kt          # экран с уникальной строкой
└── util/TaskExt.kt            # мост из Task-ов GMS в Kotlin-корутины (await)
```

## Настройка проекта (обязательно)

В репозитории лежит **заглушка** `app/google-services.json` (пустой проект,
существующий API key, placeholder-`web` client). Приложение соберётся, но работать
не будет — нужно связать его с вашим реальным проектом Firebase.

1. Создайте проект на <https://console.firebase.google.com>.
2. Добавьте Android-приложение:
   - пакет: `ru.filantrop.firebaseexampleapp`;
   - имя: `Firebase Example` (SHA-1 не требуется).
3. Скачайте `google-services.json` и положите его в `app/`, заменив заглушку.
4. **Authentication → Sign-in method** — включите **Email/Password**.
5. **Firestore Database** — создайте базу (режим Production).

### Правила Firestore

В **Firestore → Rules** разрешите каждому пользователю читать только свой документ:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if false;
    }
  }
}
```

Строка — read-only: приложение её не меняет, только читает.

### Как положить строку пользователю

1. Войдите в приложение (так в **Authentication → Users** появится пользователь).
2. Скопируйте его **UID**.
3. В **Firestore** создайте коллекцию `users` (если её нет), затем внутри — документ
   с ID, равным UID, и строковым полем `secret` (любое значение).

Теперь при входе этого пользователя приложение покажет его `secret`.
Для других пользователей покажется сообщение, что строка ещё не сохранена.

Альтернатива — генерировать строку автоматически при регистрации
(Cloud Function on Auth Trigger, пишущая `users/<uid>`). Для примера этого не нужно.

## Запуск

```
./gradlew :app:assembleDebug   # или просто запустить в Android Studio
```

Требования: API 31+ (Android 12.0+), на устройстве должен быть Play Services.

## Примечания

- Состояние аутентификации отслеживается через `AuthStateListener`, поэтому
  после выхода (`signOut`) приложение само вернётся на экран входа.
- Ошибки входа отображаются на русском с учётом кода `FirebaseAuthException`
  (`auth/invalid-credential` → «Неверный email или пароль» и т.д.).
- Версии библиотек Firebase задаёт BOM `firebase-bom` в `build.gradle.kts` модуля app.
