# Быстрый старт WLVPN

## 5 минут до первого запуска

### Шаг 1: Клонирование репозитория
```bash
git clone https://github.com/gidtfrhipoppppp-sudo/WLVPN.git
cd WLVPN
```

### Шаг 2: Открыть в Android Studio
```bash
# macOS/Linux
open .

# Windows
start .

# Или просто откройте Android Studio -> Open Project -> выберите папку
```

### Шаг 3: Ожидание синхронизации Gradle
- Android Studio автоматически загрузит все зависимости
- Это может занять 2-3 минуты в первый раз

### Шаг 4: Выбор устройства
```
Run → Select Device
 ├─ Выберите эмулятор или подключенное устройство
 └─ Нажмите OK
```

### Шаг 5: Запуск приложения
```
Run → Run 'app'
 или просто нажмите Shift + F10
```

## Структура проекта в Android Studio

```
WLVPN
├── app                          # Модуль приложения
│   ├── src/main
│   │   ├── java
│   │   │   └── com/example/wlvpn
│   │   │       ├── data         # Слой с данными
│   │   │       ├── domain       # Business logic
│   │   │       ├── ui           # Jetpack Compose UI
│   │   │       ├── service      # VPN Service
│   │   │       ├── di           # Dependency Injection
│   │   │       ├── MainActivity # Точка входа
│   │   │       └── WLVPNApp     # Application class
│   │   └── res                  # Resources
│   ├── build.gradle.kts         # Зависимости модуля
│   └── AndroidManifest.xml      # Конфиг приложения
├── build.gradle.kts             # Конфиг build системы
├── settings.gradle.kts          # Конфиг проекта
└── README.md                    # Документация
```

## Команды из терминала

### Сборка Debug версии
```bash
./gradlew assembleDebug
# APK будет в: app/build/outputs/apk/debug/
```

### Сборка Release версии
```bash
./gradlew assembleRelease
# APK будет в: app/build/outputs/apk/release/
```

### Установка и запуск на устройстве
```bash
./gradlew installDebug
./gradlew runDebug
```

### Запуск тестов
```bash
./gradlew test              # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

### Очистка проекта
```bash
./gradlew clean
# Затем: Build → Clean Project в Android Studio
```

## Основные file'ы для изменения

### Если хотите изменить UI
→ [app/src/main/java/com/example/wlvpn/ui/screens/MainScreen.kt](../app/src/main/java/com/example/wlvpn/ui/screens/MainScreen.kt)

### Если хотите изменить логику подключения
→ [app/src/main/java/com/example/wlvpn/service/VpnService.kt](../app/src/main/java/com/example/wlvpn/service/VpnService.kt)

### Если хотите добавить новые зависимости
→ [app/build.gradle.kts](../app/build.gradle.kts)

### Если хотите изменить запросы к GitHub
→ [app/src/main/java/com/example/wlvpn/data/api/GitHubVpnApi.kt](../app/src/main/java/com/example/wlvpn/data/api/GitHubVpnApi.kt)

## Решение проблем

### "Cannot resolve symbol 'R'"
```bash
# 1. Clean project
./gradlew clean

# 2. Invalidate caches in Android Studio
File → Invalidate Caches

# 3. Rebuild
Build → Rebuild Project
```

### Gradle sync fails
```bash
# 1. Убедитесь, что у вас установлен Android SDK
# в Android Studio: Tools → SDK Manager

# 2. Удалите кэш Gradle
rm -rf ~/.gradle/caches

# 3. Попробуйте снова
./gradlew sync
```

### Приложение не устанавливается
```bash
# 1. Если используете эмулятор - перезагрузите его
# 2. Удалите старое приложение:
adb uninstall com.example.wlvpn

# 3. Установите снова
./gradlew installDebug
```

### GitHub API limit
Добавьте GitHub Personal Access Token в `OkHttpClient` interceptor:

```kotlin
// В app/src/main/java/com/example/wlvpn/di/Module.kt
addInterceptor { chain ->
    val original = chain.request()
    val request = original.newBuilder()
        .header("Authorization", "token YOUR_TOKEN_HERE")
        .build()
    chain.proceed(request)
}
```

## Отладка

### Логирование
```kotlin
// В любом месте кода
Log.d("WLVPN", "Debug message: $variable")
Log.e("WLVPN", "Error occurred", exception)
```

### View логи в Android Studio
```
View → Tool Windows → Logcat
```

### Отладка процесса подключения
```
Run → Debug 'app'
```

## Следующие шаги

1. **Изучите архитектуру**: [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)
2. **Модифицируйте UI**: Измените цвета, шрифты в [ui/theme/Theme.kt](app/src/main/java/com/example/wlvpn/ui/theme/Theme.kt)
3. **Добавьте функции**: Расширьте [VpnViewModel](app/src/main/java/com/example/wlvpn/ui/viewmodels/VpnViewModel.kt)
4. **Интегрируйте с реальным VPN**: Реализуйте [VpnService](app/src/main/java/com/example/wlvpn/service/VpnService.kt)
5. **Добавьте тесты**: Создайте test'ы в `app/src/test/`

## Связь

- GitHub Issues: [Создать issue](https://github.com/gidtfrhipoppppp-sudo/WLVPN/issues/new)
- Pull Requests: [Создать PR](https://github.com/gidtfrhipoppppp-sudo/WLVPN/pulls)

## Дополнительные ресурсы

- [Android Development Docs](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [OpenVPN Android](https://github.com/schwabe/ics-openvpn)
