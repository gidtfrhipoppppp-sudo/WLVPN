# WLVPN - Документация разработчика

## Обзор архитектуры

### Слои приложения

```
┌─────────────────────────────────────┐
│          UI Layer (Compose)         │ <- MainScreen, ServerCard
├─────────────────────────────────────┤
│        ViewModel Layer (MVVM)       │ <- VpnViewModel
├─────────────────────────────────────┤
│       Use Case Layer (Domain)       │ <- FetchVpnConfigsUseCase
├─────────────────────────────────────┤
│      Repository Layer (Data)        │ <- VpnRepository
├─────────────────────────────────────┤
│         API Layer (Network)         │ <- GitHubVpnApi
└─────────────────────────────────────┘
```

## Основные компоненты

### 1. Data Layer

#### Models (`data/models/VpnConfig.kt`)
- `VpnConfig` - Конфигурация VPN с параметрами подключения
- `VpnServer` - Информация о VPN сервере
- `ConnectionState` - Состояние подключения

#### API (`data/api/GitHubVpnApi.kt`)
```kotlin
// Загрузка списка конфигов из GitHub
suspend fun getVpnConfigs(): List<GitHubContent>

// Загрузка содержимого конкретного файла
suspend fun getVpnConfigFile(@Path("path") path: String): GitHubContent
```

#### Repository (`data/repository/VpnRepository.kt`)
- Управление состоянием серверов
- Парсинг конфигов OpenVPN
- Извлечение информации о сервере из config файла

### 2. Domain Layer

#### Use Cases (`domain/usecase/FetchVpnConfigsUseCase.kt`)
```kotlin
class FetchVpnConfigsUseCase(private val repository: VpnRepository) {
    suspend operator fun invoke(): Result<List<VpnConfig>>
}
```

### 3. Presentation Layer

#### ViewModel (`ui/viewmodels/VpnViewModel.kt`)
```kotlin
fun fetchServers()           // Загрузить список серверов
fun selectServer(server)     // Выбрать сервер
fun connect()                // Подключиться к VPN
fun disconnect()             // Отключиться от VPN
```

#### UI Components (`ui/screens/MainVpnScreen.kt`)
- `MainVpnScreen` - главный экран с расширенными настройками (тип соединения, резольвер DNS, информация о Tor/DNSCrypt)
- `SettingsDialog` - всплывающее окно для базовых настроек приложения (сохранение предпочтений в DataStore, включая ByeByDPI, автообновление и импорт конфигов)
- `ConnectionStatusCard` - Карточка статуса подключения
- `ServerCard` - Карточка сервера в списке

#### Theme (`ui/theme/Theme.kt`)
- Material3 темы (Light/Dark)
- Цветовая схема
- Typography

### 4. Service Layer

#### VPN Service (`service/VpnService.kt`)
```kotlin
class VpnService : AndroidVpnService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    private fun startVpnConnection()
}
```

### 5. DI Module (`di/Module.kt`)
```kotlin
// Network
single { OkHttpClient.Builder()... }
single { Retrofit.Builder()... }
single { get<Retrofit>().create(GitHubVpnApi::class.java) }

// Data
single { VpnRepository(get()) }

// Domain
single { FetchVpnConfigsUseCase(get()) }

// Presentation
viewModel { VpnViewModel(get(), get()) }
```

## Парсинг OpenVPN конфигов

### Регулярное выражение для извлечения сервера
```kotlin
val remoteRegex = """remote\s+([^\s]+)\s+(\d+)""".toRegex()
```

### Пример конопфига
```
remote server.example.com 1194
proto udp
cipher AES-256-CBC
auth SHA256
```

## Интеграция с GitHub API

### Endpoints
```
GET /repos/igareck/vpn-configs-for-russia/contents
GET /repos/igareck/vpn-configs-for-russia/contents/{path}
```

### Лимиты
- 60 запросов/час для неавторизованных пользователей
- 5000 запросов/час с GitHub Personal Access Token

### Использование PAT (Personal Access Token)
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("Authorization", "token YOUR_GITHUB_TOKEN")
            .build()
        chain.proceed(request)
    }
    .build()
```

## Состояние приложения (Coroutine Flow)

### VpnScreenState
```kotlin
data class VpnScreenState(
    val servers: List<VpnServer> = emptyList(),      // Список серверов
    val selectedServer: VpnServer? = null,            // Выбранный сервер
    val isConnecting: Boolean = false,                // Подключение в процессе
    val isConnected: Boolean = false,                 // Подключено
    val isLoading: Boolean = false,                   // Загрузка серверов
    val errorMessage: String? = null                  // Ошибка
)
```

## Жизненный цикл подключения

```
1. Пользователь запускает приложение
   ↓
2. Activity onCreate() вызывает WLVPNApp.onCreate()
   ↓
3. Koin инициализирует все зависимости
   ↓
4. MainVpnScreen() теперь отображает настройки соединения и вызывает ViewModel для управления VPN. Показ `SettingsDialog` прикреплён к значку шестерёнки, изменения сохраняются через VpnManager/DataStore.
   ↓
5. VpnViewModel.fetchServers() загружает конфиги
   ↓
6. GitHubVpnApi.getVpnConfigs() запрашивает список у GitHub
   ↓
7. VpnRepository парсит конфиги и обновляет состояние
   ↓
8. Пользователь выбирает сервер (selectServer)
   ↓
9. Пользователь нажимает "Подключиться"
   ↓
10. VpnViewModel.connect() запускает VpnService
    ↓
11. VpnService создает VPN интерфейс и маршруты
```

## Обработка ошибок

### Network Errors
```kotlin
val result = fetchVpnConfigsUseCase()
result.onFailure { exception ->
    _state.value = _state.value.copy(
        isLoading = false,
        errorMessage = exception.message ?: "Неизвестная ошибка"
    )
}
```

### Common Issues

| Проблема | Решение |
|----------|---------|
| GitHub rate limit | Использовать Personal Access Token |
| Нет интернета | Показать сообщение об ошибке и предложить повторить |
| Неверный парс конфига | Проверить формат OVPN файла |
| VPN разрешение | Запросить разрешение у системы Android |

## Тестирование

### Unit Tests
```kotlin
@Test
fun testParseServerFromConfig() {
    val config = "remote test.com 1194"
    val result = parseServerFromConfig("test-config", config)
    assertEquals("test.com", result?.serverAddress)
    assertEquals(1194, result?.port)
}
```

### Integration Tests
```kotlin
@Test
fun testFetchVpnConfigs() = runTest {
    val result = fetchVpnConfigsUseCase()
    result.onSuccess { configs ->
        assertTrue(configs.isNotEmpty())
    }
}
```

## Производительность

### Оптимизация
1. **Кэширование**: Сохранять загруженные конфиги локально в DataStore
2. **Пагинация**: Загружать серверы постепенно при скролле
3. **Параллельные запросы**: Использовать `coroutineScope` для параллельных запросов

### Memory Management
- ViewModel: Кэширует данные во время жизненного цикла Activity
- Coroutines: Автоматически отменяются при уничтожении ViewModel
- Repository: SingletonScoped - создается один раз на приложение

## Безопасность

### Android Permissions
- `INTERNET` - Для сетевых запросов
- `BIND_VPN_SERVICE` - Для VPN подключения
- `CHANGE_NETWORK_STATE` - Для управления сетью

### Data Security
- Конфиги хранятся в памяти (не сохраняются на диск)
- Личные ключи и сертификаты хранятся отдельно
- API requests используют HTTPS

## Развитие проекта

### Планируемые функции
- [ ] Сохранение конфигов локально
- [ ] Автоматическое переключение серверов
- [ ] Статистика использования
- [ ] Поддержка WireGuard
- [ ] Расширенные настройки

### Contributing
1. Fork репозитория
2. Создайте feature branch
3. Добавьте tests
4. Отправьте Pull Request
