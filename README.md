# WLVPN - Android VPN Application

Android VPN приложение, которое загружает и использует VPN конфигурации из репозитория [igareck/vpn-configs-for-russia](https://github.com/igareck/vpn-configs-for-russia).

## Особенности

- **Загрузка конфигов с GitHub**: Приложение автоматически загружает доступные VPN конфигурации из репозитория
- **Простой интерфейс**: Интуитивный UI на основе Jetpack Compose
- **Быстрое подключение**: Одна кнопка для подключения/отключения VPN
- **Выбор сервера**: Список доступных серверов с возможностью выбора
- **Поддержка OpenVPN**: Использует OpenVPN конфигурации
- **Offline режим**: Кэширует загруженные конфигурации локально

## Структура проекта

```
WLVPN/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/wlvpn/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/         # GitHub API интеграция
│   │   │   │   │   ├── models/      # Data классы
│   │   │   │   │   └── repository/  # Repository pattern
│   │   │   │   ├── domain/
│   │   │   │   │   └── usecase/     # Business logic
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/     # Jetpack Compose screens
│   │   │   │   │   ├── theme/       # Design system
│   │   │   │   │   └── viewmodels/  # MVVM ViewModels
│   │   │   │   ├── service/         # VPN Service
│   │   │   │   ├── di/              # Koin DI modules
│   │   │   │   ├── MainActivity.kt  # Entry point
│   │   │   │   └── WLVPNApp.kt      # Application class
│   │   │   └── AndroidManifest.xml
│   │   └── res/                     # Resources
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Технологический стек

### Android & Kotlin
- **Kotlin** - Основной язык программирования
- **Android API 24+** - Минимальная версия SDK
- **Jetpack Compose** - Modern UI toolkit
- **Material3** - Material Design компоненты

### Networking & VPN
- **Retrofit 2** - HTTP клиент
- **OkHttp3** - HTTP interceptor и cookie manager
- **OpenVPN Android** - VPN подключение
- **Gson** - JSON serialization

### Architecture & DI
- **MVVM** - Model-View-ViewModel паттерн
- **Repository Pattern** - Data layer abstraction
- **Koin** - Lightweight DI framework
- **Coroutines** - Asynchronous programming

## Установка и запуск

### Требования

- Android Studio Hedgehog или выше
- JDK 17+
- Android SDK 34+
- Gradle 8.2+

### Шаги установки

1. **Клонируйте репозиторий**
   ```bash
   git clone https://github.com/gidtfrhipoppppp-sudo/WLVPN.git
   cd WLVPN
   ```

2. **Откройте в Android Studio**
   ```bash
   android-studio .
   ```

3. **Синхронизируйте Gradle**
   - Android Studio автоматически синхронизирует проект при открытии
   - Или используйте: `./gradlew sync`

4. **Запустите приложение**
   - Выберите эмулятор или подключите устройство
   - Нажмите "Run" в Android Studio (Shift + F10)

### Build из командной строки

```bash
# Debug версия
./gradlew installDebug

# Release версия
./gradlew assembleRelease
```

## Использование

### Основной процесс

1. **Запустите приложение** - При запуске приложение автоматически загружает список VPN серверов из GitHub
2. **Выберите сервер** - Нажмите на нужный сервер из списка
3. **Подключитесь** - Нажмите кнопку "Подключиться"
4. **Примите разрешение** - Android попросит разрешение на использование VPN
5. **Используйте интернет** - Весь трафик будет маршрутизирован через VPN

### Отключение VPN

- Нажмите кнопку "Отключиться" в приложении или
- Используйте системные настройки VPN в Android

## Архитектурные решения

### Data Flow

```
GitHub (vpn-configs)
    ↓
GitHub API (Retrofit)
    ↓
VpnRepository
    ↓
FetchVpnConfigsUseCase
    ↓
VpnViewModel
    ↓
UI (Jetpack Compose)
```

### Dependency Injection (Koin)

Все зависимости автоматически внедряются через Koin контейнер в `di/Module.kt`:

```kotlin
// API и Networking
- OkHttpClient
- Retrofit instance
- GitHubVpnApi

// Data layer
- VpnRepository

// Domain layer
- FetchVpnConfigsUseCase

// Presentation layer
- VpnViewModel
```

## Разрешения Android

Приложение требует следующие разрешения:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

## Запросы к GitHub API

Приложение использует GitHub API для загрузки конфигураций:

```
GET https://api.github.com/repos/igareck/vpn-configs-for-russia/contents
GET https://api.github.com/repos/igareck/vpn-configs-for-russia/contents/{path}
```

**Примечание**: GitHub API имеет ограничение на 60 запросов в час для неавторизованных пользователей. Для большего количества запросов используйте GitHub Personal Access Token.

## Развитие проекта

### Планируемые улучшения

- [x] Базовая загрузка конфигураций с GitHub
- [x] UI для выбора серверов
- [ ] Интеграция с OpenVPN Native Lib для лучшего подключения
- [ ] Сохранение предпочтений пользователя
- [ ] Статистика использования
- [x] Расширенные настройки VPN (выбор типа соединения, резольвера DNS, тёмная тема с сохранением в настройках)
- [x] Переключатель ByeByDPI в настройках
- [x] Автообновление VPN конфигураций (настраиваемый интервал)
- [x] Импорт собственных VPN конфигураций через настройки
- [ ] Поддержка WireGuard
- [ ] Автоматическое переключение серверов
- [ ] Push-уведомления
- [ ] Сортировка и фильтрация серверов по пингу и нагрузке

### Прямые взносы

1. Создайте fork репозитория
2. Создайте ветку для вашего улучшения (`git checkout -b feature/AmazingFeature`)
3. Закоммитьте изменения (`git commit -m 'Add some AmazingFeature'`)
4. Отправьте изменения (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## Лицензия

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Автор

- **gidtfrhipoppppp-sudo**

## Благодарности

- [igareck/vpn-configs-for-russia](https://github.com/igareck/vpn-configs-for-russia) - VPN конфигурации
- [de.blinkt/openvpn-android](https://github.com/schwabe/ics-openvpn) - OpenVPN для Android
- Android & Jetpack Compose сообщество

## Поддержка и помощь

Если у вас есть вопросы или проблемы:

1. Проверьте [Issues](https://github.com/gidtfrhipoppppp-sudo/WLVPN/issues)
2. Создайте новый Issue с детальным описанием проблемы
3. Укажите версию Android и информацию об устройстве

## Отказ от ответственности

Это приложение предназначено только для законного использования. Пользователь несет полную ответственность за использование этого приложения. Убедитесь, что использование VPN законно в вашей стране и юрисдикции.
