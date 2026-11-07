# Технический аудит проекта FootballPulse

> **Дата аудита:** 7 ноября 2026 г.  
> **Репозиторий:** `com.artsiom.footballpulse` (`FootballPulse`)  
> **Режим аудита:** Static Code Analysis + проверка окружения сборки (без внесения изменений в исходный код проекта).

---

## 1. Фактический стек, версии, архитектура и структура проекта

Ниже приведены данные исключительно на основании фактического содержимого конфигурационных файлов и исходного кода репозитория (с отметками, где фактическая реализация расходится с `README.md` или объявленными в Gradle зависимостями).

### 1.1. Инструменты сборки и SDK

| Параметр | Фактическое значение | Источник |
|---|---|---|
| **Gradle Wrapper** | `9.3.1` (`gradle-9.3.1-bin.zip`) | `gradle/wrapper/gradle-wrapper.properties` |
| **Gradle Daemon Toolchain** | JDK `21` + плагин `org.gradle.toolchains.foojay-resolver-convention:1.0.0` | `gradle/gradle-daemon-jvm.properties`, `settings.gradle.kts` |
| **Android Gradle Plugin (AGP)** | `9.1.0` (`com.android.application`) | `gradle/libs.versions.toml` |
| **Kotlin** | Встроен в AGP 9.1.0 (отдельный плагин `org.jetbrains.kotlin.android` и версия Kotlin в `libs.versions.toml` / `build.gradle.kts` явно не объявлены; `kotlin.code.style=official`) | `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties` |
| **Java Compatibility** | `JavaVersion.VERSION_11` (`sourceCompatibility` и `targetCompatibility`) | `app/build.gradle.kts` |
| **Namespace / Application ID** | `com.artsiom.footballpulse` | `app/build.gradle.kts` |
| **compileSdk / targetSdk / minSdk** | `35` / `35` / `26` (Android 8.0 — Android 15) | `app/build.gradle.kts` |
| **Версия приложения** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts` |
| **Build Features** | `buildConfig = true` (**прим.:** `viewBinding` НЕ включён, хотя заявлен в `README.md`) | `app/build.gradle.kts` |
| **Release-конфигурация** | `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro` (файл правил пуст — содержит только комментарии) | `app/build.gradle.kts`, `app/proguard-rules.pro` |

### 1.2. Фактические зависимости и плагины

Часть зависимостей вынесена в Version Catalog (`gradle/libs.versions.toml`), а часть объявлена строковыми литералами напрямую в `app/build.gradle.kts` и корневом `build.gradle.kts`:

| Категория | Библиотека / Плагин | Версия | Способ объявления | Фактическое использование в коде |
|---|---|---|---|---|
| **Core / UI** | `androidx.core:core-ktx` | `1.10.1` | `libs.versions.toml` | Используется (`ContextCompat`) |
| **Core / UI** | `androidx.appcompat:appcompat` | `1.6.1` | `libs.versions.toml` | Используется (`AppCompatActivity`, `AppCompatDelegate`) |
| **Core / UI** | `com.google.android.material:material` | `1.10.0` | `libs.versions.toml` | Используется (`MaterialCardView`, `BottomNavigationView`, `FloatingActionButton`, тема `Theme.Material3.DayNight.NoActionBar`) |
| **Core / UI** | `androidx.activity:activity` | `1.8.0` | `libs.versions.toml` | Используется (`enableEdgeToEdge()`) |
| **Core / UI** | `androidx.constraintlayout:constraintlayout` | `2.1.4` | `libs.versions.toml` | Используется в `fragment_matches.xml`, `fragment_standings.xml`, `item_match.xml` |
| **Сеть** | `com.squareup.retrofit2:retrofit` | `2.9.0` | Строка в `app/build.gradle.kts` | Используется (`FootballApiService`, `RetrofitInstance`) |
| **JSON** | `com.squareup.retrofit2:converter-gson` | `2.9.0` | Строка в `app/build.gradle.kts` | Используется (`GsonConverterFactory`, `@SerializedName`) |
| **HTTP-логгер** | `com.squareup.okhttp3:logging-interceptor` | `4.12.0` | Строка в `app/build.gradle.kts` | Используется в `RetrofitInstance` |
| **Асинхронность** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.7.3` | Строка в `app/build.gradle.kts` | Используется (`StateFlow`, `MutableStateFlow`, `combine`, `launch`, `suspend`) |
| **Lifecycle** | `androidx.lifecycle:lifecycle-viewmodel-ktx` | `2.7.0` | Строка в `app/build.gradle.kts` | Используется (`ViewModel`, `viewModelScope`, `ViewModelProvider`) |
| **Lifecycle** | `androidx.lifecycle:lifecycle-runtime-ktx` | `2.7.0` | Строка в `app/build.gradle.kts` | Используется (`lifecycleScope`, `repeatOnLifecycle`) |
| **Изображения** | `io.coil-kt:coil` | `2.6.0` | Строка в `app/build.gradle.kts` | Используется (`ImageView.load(...)`), но **без** `coil-svg` |
| **БД (Room)** | `androidx.room:room-runtime` | `2.6.1` | Строка в `app/build.gradle.kts` | **НЕ используется в коде** (нет ни одного `@Entity`/`@Dao`/`@Database`, не подключён `ksp`/`kapt`) |
| **БД (Room)** | `androidx.room:room-ktx` | `2.6.1` | Строка в `app/build.gradle.kts` | **НЕ используется в коде** |
| **DI (Hilt)** | Плагин `com.google.dagger.hilt.android` | `2.50` | Строка в корневом `build.gradle.kts` (`apply false`) | **НЕ применён в `:app` и НЕ используется в коде** |
| **Unit-тесты** | `junit:junit` | `4.13.2` | `libs.versions.toml` | Только шаблонный `ExampleUnitTest` |
| **UI-тесты** | `androidx.test.ext:junit` | `1.1.5` | `libs.versions.toml` | Только шаблонный `ExampleInstrumentedTest` |
| **UI-тесты** | `androidx.test.espresso:espresso-core` | `3.5.1` | `libs.versions.toml` | Не используется (в `ExampleInstrumentedTest` только `InstrumentationRegistry`) |

### 1.3. Архитектура

Проект реализует базовый вариант **MVVM (Model — View — ViewModel)** с однонаправленным потоком состояния (`StateFlow`) и подходом **Single Activity** (`MainActivity` + 4 фрагмента), но с рядом упрощений:
* **UI Layer (Views):** Классическая View-система на XML (`ConstraintLayout`, `LinearLayout`, `FrameLayout`, `NestedScrollView`, `RecyclerView`). Привязка View во всех Activity, Fragment и Adapter выполнена через `findViewById` (вопреки заявленному в `README.md` ViewBinding). На экранах деталей (`MatchDetailsFragment`, `TeamDetailsFragment`) и в бейджах формы (`StandingsAdapter`) часть дерева View создаётся программно в Kotlin-коде через `LinearLayout(context)` и `TextView(context)`.
* **Presentation Layer (ViewModels):** 4 класса `ViewModel` (`MatchesViewModel`, `StandingsViewModel`, `MatchDetailsViewModel`, `TeamDetailsViewModel`), каждый из которых хранит состояние в `MutableStateFlow<...UiState>` (`Loading`, `Success`, `Error`). Для `MatchesViewModel` и `StandingsViewModel` область жизни привязана к `requireActivity()`, а для `MatchDetailsViewModel` и `TeamDetailsViewModel` — к самому фрагменту через кастомные `ViewModelProvider.Factory`.
* **Domain Layer:** Представлен исключительно пакетом `domain.model` с неизменяемыми `data class`-моделями и объектом `Leagues`. В слое `domain` отсутствуют UseCase-классы и интерфейсы репозиториев.
* **Data Layer:** Единственный конкретный класс `FootballRepository`, который напрямую вызывает синглтон `RetrofitInstance.api` (`FootballApiService`), выполняет HTTP-запросы к `https://api.football-data.org/v4/...`, маппит DTO в доменные модели и рассчитывает текущий тур сезона и форму команд за последние 5 матчей. Локальное хранилище (БД/кеш) отсутствует (кроме `SharedPreferences` в `MainActivity` для флага тёмной темы).

### 1.4. Структура модулей и пакетов

Проект **одномодульный** (`settings.gradle.kts` включает только `:app`).

```text
FootballPulse/
├── build.gradle.kts
├── settings.gradle.kts               # include(":app")
├── gradle/
│   ├── libs.versions.toml
│   ├── gradle-daemon-jvm.properties  # toolchainVersion=21
│   └── wrapper/
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/artsiom/footballpulse/
        │   │   ├── MainActivity.kt
        │   │   ├── data/
        │   │   │   ├── FootballRepository.kt        # + data class MatchdayResult
        │   │   │   └── remote/
        │   │   │       ├── FootballApiService.kt
        │   │   │       ├── RetrofitInstance.kt
        │   │   │       ├── MatchesResponse.kt
        │   │   │       ├── MatchDto.kt
        │   │   │       ├── MatchDetailDto.kt        # + PlayerDto, LineupTeamDto
        │   │   │       ├── ScoreDto.kt              # + FullTimeDto, HalfTimeDto
        │   │   │       ├── StandingsDto.kt          # StandingsResponse, StandingDto, TableEntryDto
        │   │   │       ├── TeamDetailsDto.kt        # + SquadPlayerDto
        │   │   │       └── TeamDto.kt
        │   │   ├── domain/
        │   │   │   └── model/
        │   │   │       ├── League.kt
        │   │   │       ├── Leagues.kt
        │   │   │       ├── Match.kt
        │   │   │       ├── MatchDetail.kt           # + Player
        │   │   │       ├── Standing.kt
        │   │   │       └── TeamDetails.kt           # + SquadPlayer
        │   │   └── ui/
        │   │       ├── matches/
        │   │       │   ├── MatchesFragment.kt
        │   │       │   ├── MatchesViewModel.kt
        │       │   │   ├── MatchesUiState.kt
        │   │       │   ├── MatchesAdapter.kt        # + sealed class MatchListItem
        │   │       │   └── LeaguesAdapter.kt
        │   │       ├── standings/
        │   │       │   ├── StandingsFragment.kt
        │   │       │   ├── StandingsViewModel.kt
        │   │       │   ├── StandingsUiState.kt
        │   │       │   ├── StandingsAdapter.kt
        │   │       │   └── ViewMode.kt
        │   │       ├── matchdetails/
        │   │       │   ├── MatchDetailsFragment.kt
        │   │       │   ├── MatchDetailsViewModel.kt # + MatchDetailsViewModelFactory
        │   │       │   └── MatchDetailsUiState.kt
        │   │       └── teamdetails/
        │   │           ├── TeamDetailsFragment.kt
        │   │           └── TeamDetailsViewModel.kt  # + TeamDetailsUiState, TeamDetailsViewModelFactory
        │   └── res/
        │       ├── drawable/                        # 7 векторных XML-иконок
        │       ├── layout/                          # 10 XML-разметок (activity, 4 fragment, 5 item)
        │       ├── menu/                            # bottom_nav_menu.xml
        │       ├── values/                          # colors.xml, strings.xml, themes.xml
        │       └── values-night/                    # colors.xml, themes.xml
        ├── test/java/com/artsiom/footballpulse/
        │   └── ExampleUnitTest.kt
        └── androidTest/java/com/artsiom/footballpulse/
            └── ExampleInstrumentedTest.kt
```

---

## 2. Команды сборки, тестов и линта (результаты запуска)

Стандартные команды Gradle для проекта:
* Сборка debug APK: `./gradlew assembleDebug`
* Запуск unit-тестов: `./gradlew test` (или `./gradlew testDebugUnitTest`)
* Запуск Android Lint: `./gradlew lint` (или `./gradlew lintDebug`)

### Фактический результат выполнения `./gradlew assembleDebug` и `./gradlew test`

При запуске команд в рамках аудита выявлены **3 последовательных блокера** (два на уровне репозитория/скриптов сборки и один на уровне изолированного окружения):

1. **Отсутствует бит исполнения (`+x`) у файла `gradlew` в Git:**
   * В индексе Git файл `gradlew` сохранён с правами `100644` вместо `100755` (`git ls-files -s gradlew` -> `100644 ef07e016... gradlew`).
   * Прямой вызов `./gradlew assembleDebug` и `./gradlew test` на Linux/macOS завершается с **кодом возврата `126`**:
     ```text
     /bin/bash: line 1: ./gradlew: Permission denied
     ```
2. **Безусловное чтение `local.properties` в `app/build.gradle.kts` ломает конфигурацию на чистом клоне / в CI:**
   * В строках 3–4 файла [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L3-L4) написано:
     ```kotlin
     val localProperties = Properties()
     localProperties.load(rootProject.file("local.properties").inputStream())
     ```
   * Так как `local.properties` добавлен в `.gitignore` и отсутствует в репозитории, при запуске любой задачи Gradle (`assembleDebug`, `test`, `lint`) на новой машине или в CI/CD стадия конфигурации упадёт с `java.io.FileNotFoundException: .../local.properties (No such file or directory)` ещё до начала компиляции или запуска тестов.
3. **Сетевое ограничение контейнера при скачивании Gradle 9.3.1:**
   * При обходе проблемы прав через `bash ./gradlew assembleDebug` и `bash ./gradlew test` Gradle Wrapper пытается скачать `https://services.gradle.org/distributions/gradle-9.3.1-bin.zip` и падает в текущей песочнице с `javax.net.ssl.SSLException: Unexpected error: java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty` (системный `cacerts` для OpenJDK 17 в песочнице пуст, а предкешированного Gradle 9.3.1 и Android SDK 35 в контейнере нет).

---

## 3. Выявленные проблемы в коде

### 3.1. Устаревшие, дублирующиеся и «мёртвые» зависимости + критический баг R8 в Release

1. **Неиспользуемые («мёртвые») зависимости в сборке:**
   * В корневом [build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/build.gradle.kts#L4) подключён плагин `com.google.dagger.hilt.android` версии `2.50` (`apply false`), но в `:app` он не применён и в коде не используется.
   * В [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L58-L59) подключены `androidx.room:room-runtime:2.6.1` и `androidx.room:room-ktx:2.6.1` (причём без `room-compiler` / `ksp`), которые нигде в коде не используются и лишь увеличивают размер графа зависимостей.
2. **Непоследовательное использование Version Catalog (`libs.versions.toml`):**
   * В [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml#L1-L24) объявлены только базовые библиотеки из шаблона Android Studio, тогда как Retrofit, OkHttp, Coroutines, Lifecycle, Room и Coil прописаны строками прямо в `app/build.gradle.kts`.
3. **Устаревшие версии библиотек и целевой JVM:**
   * `retrofit:2.9.0` и `converter-gson:2.9.0` (выпущены в 2020 г.; актуальная ветка `2.11.0+`).
   * `core-ktx:1.10.1`, `appcompat:1.6.1`, `material:1.10.0`, `activity:1.8.0`, `constraintlayout:2.1.4`, `lifecycle-*:2.7.0`, `kotlinx-coroutines-android:1.7.3` — все имеют более свежие стабильные релизы.
   * `sourceCompatibility` / `targetCompatibility` выставлены в `JavaVersion.VERSION_11`, хотя AGP `9.1.0` и демон Gradle уже работают на JDK 21 (стандарт для современных Android-проектов — JDK 17).
4. **Отсутствие `coil-svg` для загрузки эмблем клубов:**
   * Подключён только базовый артефакт `io.coil-kt:coil:2.6.0`. API `football-data.org` для многих клубов (особенно Bundesliga, La Liga, Serie A) возвращает URL эмблем в формате `.svg` (например, `https://crests.football-data.org/5.svg`). Без `io.coil-kt:coil-svg` и зарегистрированного `SvgDecoder` такие эмблемы не декодируются и уходят в `error(R.drawable.ic_placeholder_crest)`.
5. **КРИТИЧНО: сломанная десериализация JSON в Release-сборке из-за R8 и отсутствия `@SerializedName` / ProGuard-правил:**
   * В [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L29-L36) для `release` включены `isMinifyEnabled = true` и `isShrinkResources = true`, но файл [app/proguard-rules.pro](file:///home/antigravity/karzhaneuski/FootballPulse/app/proguard-rules.pro#L1-L21) полностью пустой.
   * Аннотации `@SerializedName` присутствуют **только** в [MatchDetailDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchDetailDto.kt#L5-L27) и [TeamDetailsDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/TeamDetailsDto.kt#L5-L24).
   * В файлах [MatchesResponse.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchesResponse.kt#L3-L5), [MatchDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchDto.kt#L3-L11), [TeamDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/TeamDto.kt#L3-L7), [ScoreDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/ScoreDto.kt#L3-L16) (`ScoreDto` также не является `data class`) и [StandingsDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/StandingsDto.kt#L3-L23) аннотации `@SerializedName` **отсутствуют**.
   * При сборке `release` обфускатор R8 переименует поля этих DTO в `a`, `b`, `c`, после чего `Gson` не сможет сопоставить JSON-ключи с полями классов — приложение в Release-сборке либо покажет пустые списки, либо упадёт с `NullPointerException`.

### 3.2. Отсутствие тестов и проблемы тестируемости

1. **Полное отсутствие реальных тестов:**
   * В `src/test` есть только шаблонный [ExampleUnitTest.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/test/java/com/artsiom/footballpulse/ExampleUnitTest.kt#L12-L17) (`2 + 2 == 4`), а в `src/androidTest` — только [ExampleInstrumentedTest.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/androidTest/java/com/artsiom/footballpulse/ExampleInstrumentedTest.kt#L16-L24).
   * Нет ни одного теста для бизнес-логики репозитория (`computeFormByTeam`, выбор текущего тура в `getCurrentMatchdayResult`, `computeCurrentSeason`, маппинг DTO -> Domain) и `ViewModel`.
2. **Жёсткая связанность (Tight Coupling), блокирующая написание unit-тестов:**
   * Все 4 `ViewModel` создают экземпляр `private val repository = FootballRepository()` внутри себя (см. [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt#L13), [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt#L13), [MatchDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsViewModel.kt#L13), [TeamDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsViewModel.kt#L20)), а не получают интерфейс через конструктор.
   * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L23) обращается напрямую к глобальному синглтону `RetrofitInstance.api` и использует системный `Calendar.getInstance()`.
   * В тестовых зависимостях отсутствуют `kotlinx-coroutines-test`, `mockk` / `mockito`, `turbine`, `mockwebserver`.

### 3.3. Ключи API и конфигурация безопасности

1. **Положительный момент:** В текущем коде и во всей истории коммитов Git нет захардкоженного в открытом виде токена `football-data.org`. В коммите `188bfe1` было удалено логирование ключа в Logcat (`Log.d("AUTH_DEBUG", "API key = ...")`), добавлен `redactHeader("X-Auth-Token")`, а `HttpLoggingInterceptor.Level.BODY` ограничен флагом `BuildConfig.DEBUG` (см. [RetrofitInstance.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt#L11-L26)).
2. **Проблема 1 — Падение сборки при отсутствии `local.properties` и подстановка `"null"`:**
   * В [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L3-L4) нет проверки `.exists()` перед чтением `local.properties` и нет fallback на переменную окружения `System.getenv("FOOTBALL_API_KEY")`.
   * Если файл `local.properties` существует, но в нём не задан `FOOTBALL_API_KEY`, строка `buildConfigField("String", "FOOTBALL_API_KEY", "\"${localProperties["FOOTBALL_API_KEY"]}\"")` (строка 21) генерирует в `BuildConfig.java` строковый литерал `"null"`. В результате приложение молча отправляет заголовок `X-Auth-Token: null`.
3. **Проблема 2 — Извлекаемость ключа из клиентского APK:**
   * Любое значение в `BuildConfig.FOOTBALL_API_KEY` компилируется в открытую константу внутри `classes.dex` и легко извлекается через `jadx` или `strings`. Для учебного/портфолио-проекта это допустимо при бесплатном тарифе, но должно быть явно задокументировано или закрыто проверкой пустого ключа.

### 3.4. Утечки слоёв, нарушение принципов архитектуры и баги UI/навигации

1. **Утечка слоя `data` в `ui` (отсутствие инверсии зависимостей):**
   * Классы `ViewModel` в пакете `ui.*` напрямую импортируют и инстанцируют класс `com.artsiom.footballpulse.data.FootballRepository`. В пакете `domain` нет интерфейса репозитория.
2. **Утечка доменной и презентационной логики в UI-компоненты (Fragments и RecyclerView.Adapter):**
   * **В [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt#L20-L34):** захардкожено количество туров для каждой лиги (`leagueMaxMatchdays`: 38 или 34) и дефолтный год сезона (`private var currentSeason: Int = 2025`).
   * **В [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt#L39-L57):** прямо внутри `submitList()` на главном (UI) потоке выполняется парсинг ISO-дат через `SimpleDateFormat`, группировка `groupBy`, сортировка `toSortedMap()` и трансформация доменных моделей в UI-элементы `MatchListItem`.
   * **В [StandingsAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsAdapter.kt#L39-L42) и строках 160–168:** в UI-адаптере захардкожены правила еврокубковых зон и зоны вылета (`CHAMPIONS_LEAGUE_SPOTS = 4`, `EUROPA_LEAGUE_POSITION = 5`, `RELEGATION_ZONE_SIZE = 3`) одинаково для всех 5 лиг (хотя в Бундеслиге и Лиге 1 по 18 команд и правила зоны вылета/стыков отличаются от лиг с 20 командами). Там же в адаптере парсится строка формы `standing.form` (строки 214–225).
   * **В [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt#L197-L347):** прямо во фрагменте находятся бизнес-правила группировки позиций игроков (`normalizePosition`), сортировки по игровому номеру, вычисления возраста (`calculateAge` через `LocalDate.parse(dateOfBirth)` **без `try/catch`**, что приведёт к крашу `DateTimeParseException` при нестандартном формате даты) и словарь `countryFlag` на 50 стран.
3. **Разнобой API для работы с датами и потоконебезопасный `SimpleDateFormat`:**
   * В проекте одновременно используются **3 разных API дат**:
     * `java.util.Calendar` в [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L206);
     * `java.text.SimpleDateFormat` в `companion object` (статические синглтоны, не являющиеся потокобезопасными!) в [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt#L29-L34) и [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt#L41-L45);
     * `java.time.LocalDate` / `Period` в [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt#L289-L293) (при `minSdk = 26` пакет `java.time` доступен нативно во всём проекте).
4. **Баги ручной навигации фрагментов и переключения темы в `MainActivity`:**
   * В [MainActivity.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/MainActivity.kt#L22-L84) фрагменты вкладок создаются как поля Activity (`private val matchesFragment by lazy { MatchesFragment() }`) и переключаются через `add`/`hide`/`show` (`showFragment()`).
   * При переходе на детали матча или команды вызывается `.replace(R.id.fragmentContainer, ...).addToBackStack(null)`. Вызов `replace` на том же контейнере удаляет из него оба фрагмента вкладок (`matchesFragment` и `standingsFragment`), при этом `BottomNavigationView` и `FloatingActionButton` смены темы остаются видимыми поверх экранов деталей. Если на экране деталей нажать на вкладку в `BottomNavigationView`, вызовется `showFragment()`, который скроет экран деталей, но оставит его в `BackStack` — кнопка «Назад» после этого работает некорректно.
   * При переключении темы (`AppCompatDelegate.setDefaultNightMode(...)`) или повороте экрана `MainActivity` пересоздаётся. `FragmentManager` автоматически восстанавливает старые экземпляры фрагментов, но поля `by lazy` в новом экземпляре `MainActivity` создают **новые** экземпляры `MatchesFragment` и `StandingsFragment`, из-за чего при переключении вкладки в контейнер добавляются дублирующиеся фрагменты.
5. **Незавершённая функциональность («мёртвый» код составов матча):**
   * В [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L127-L143) парсятся и сортируются `homeLineup`, `awayLineup`, `homeBench`, `awayBench`, в `res/layout/` создан файл [item_player_row.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/item_player_row.xml#L1-L35), а в [fragment_match_details.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_match_details.xml#L233-L275) есть `benchCard` и `benchContainer`. Однако в [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt#L76) стоит `benchCard.visibility = View.GONE`, а `item_player_row.xml` нигде не используется.
6. **Хардкод строк в коде и XML + использование `notifyDataSetChanged()`:**
   * В [strings.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/values/strings.xml#L1-L3) объявлена только одна строка (`app_name`). Все остальные надписи в XML и Kotlin-коде (`"Matchday $matchday"`, `"Retry"`, `"← Back"`, `"Club Info"`, `"Squad"`, `"First Half"`, `"Full Time"` и т.д.) захардкожены.
   * Все 3 адаптера (`MatchesAdapter`, `StandingsAdapter`, `LeaguesAdapter`) используют `notifyDataSetChanged()` вместо `ListAdapter` и `DiffUtil`.

### 3.5. Обработка ошибок сети и неэффективная работа с API

1. **Перехват `Exception` ломает Structured Concurrency (`CancellationException`) и создаёт гонки запросов (Race Conditions):**
   * Во всех четырёх `ViewModel` и в [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L90) используется блок `try { ... } catch (e: Exception)`, который перехватывает в том числе `kotlinx.coroutines.CancellationException`.
   * В [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt#L41-L85) и [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt#L46-L56) при быстром переключении вкладок лиг или кликах по кнопкам `‹` / `›` (смена тура) предыдущая `Job` не отменяется — запускается новая корутина параллельно старой, что приводит к гонке состояний (`_uiState.value` перезаписывается тем ответом, который пришёл позже).
2. **Сырые технические ошибки в UI и отсутствие кнопки `Retry` на главном экране (`MatchesFragment`):**
   * Репозиторий выбрасывает `Exception("HTTP ${response.code()}: ${response.message()}")` (где `response.message()` в HTTP/2 часто пустой), а при отсутствии интернета выбрасывается `UnknownHostException` / `SocketTimeoutException`. Пользователь видит на экране сырой текст исключения JVM/HTTP.
   * На экранах `StandingsFragment`, `MatchDetailsFragment` и `TeamDetailsFragment` есть кнопка `Retry`, но на главном экране [fragment_matches.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_matches.xml#L102-L110) есть только `TextView` (`errorText`) **без кнопки `Retry`**.
3. **Избыточные сетевые запросы, быстро исчерпывающие лимит бесплатного API (10 запросов/мин на `football-data.org`):**
   * Метод `getCurrentMatchdayResult(leagueCode)` в [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L21-L54) скачивает **все матчи сезона** (`GET v4/competitions/{code}/matches?season=...` — 380 матчей для АПЛ/Ла Лиги/Серии А), выбирает из них 10 матчей текущего тура, а остальные **370 матчей выбрасывает** (не сохраняя даже в памяти). При нажатии кнопки следующего/предыдущего тура (`getMatchesByMatchday`) приложение снова идёт в сеть за данными, которые только что скачало.
   * Метод `getStandings(leagueCode)` в [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L76-L92) при каждом открытии таблицы делает **сразу 2 сетевых запроса**: `getStandings(leagueCode)` и повторно скачивает все 380 матчей сезона через `getMatches(leagueCode, season)` ради вычисления формы (`computeFormByTeam`). Достаточно 3–4 переключений между лигами, чтобы получить ошибку `HTTP 429 (Too Many Requests)`.
4. **Логическая ошибка расчёта формы команд при перенесённых матчах:**
   * В [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L174-L176) завершённые матчи сортируются по номеру тура (`sortedBy { it.matchday ?: Int.MAX_VALUE }`), а не по фактической дате проведения (`utcDate`). Если матч 10-го тура был перенесён и сыгран после 25-го тура, сортировка по `matchday` поместит его в прошлое, и форма последних 5 матчей (`takeLast(5)`) будет рассчитана неверно.
5. **Отсутствие таймаутов в `OkHttpClient`:**
   * В [RetrofitInstance.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt#L17-L28) используются только дефолтные настройки `OkHttpClient.Builder()` без явной конфигурации таймаутов и кеша HTTP-ответов.

---

## 4. Топ-12 улучшений для усиления проекта в портфолио Android-разработчика

Ниже представлен приоритизированный список из 12 конкретных доработок, которые превратят проект из учебного прототипа в сильный портфолио-кейс уровня Middle/Senior Android Developer.

### 1. Исправление конфигурации сборки (`local.properties`, `gradlew`) и защита DTO для R8 в Release
* **Зачем:** Сейчас проект не соберётся на чистом клоне или в CI без файла `local.properties`, `gradlew` не имеет прав на запуск, а Release-сборка с `isMinifyEnabled = true` сломана из-за обфускации DTO без `@SerializedName`. Работодатели часто проверяют как чистую сборку из репозитория, так и корректность ProGuard/R8 правил.
* **Примерный объём:** **S**
* **Затрагиваемые файлы:**
  * `gradlew` (права `100755` в Git)
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts)
  * [app/proguard-rules.pro](file:///home/antigravity/karzhaneuski/FootballPulse/app/proguard-rules.pro)
  * [MatchesResponse.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchesResponse.kt), [MatchDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchDto.kt), [TeamDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/TeamDto.kt), [ScoreDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/ScoreDto.kt), [StandingsDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/StandingsDto.kt)
* **Критерий готовности (DoD):**
  * `local.properties` читается безопасно (`if (localPropertiesFile.exists())`), с fallback на `System.getenv("FOOTBALL_API_KEY") ?: ""`.
  * Все классы в `data/remote/` помечены `@SerializedName` (а `ScoreDto`, `FullTimeDto`, `HalfTimeDto` переведены в `data class`), в `proguard-rules.pro` добавлены правила `-keep class com.artsiom.footballpulse.data.remote.** { *; }` и правила для Gson/Retrofit.
  * `./gradlew assembleRelease` и `./gradlew assembleDebug` успешно выполняются даже при отсутствии `local.properties`.

### 2. Полноценное внедрение зависимостей (Dagger Hilt) и инверсия зависимостей (Domain Repository Interface)
* **Зачем:** В корневом `build.gradle.kts` уже объявлен плагин Hilt, но DI не настроен, а `ViewModel` сами создают `FootballRepository()`. Полноценный DI с разделением интерфейса `FootballRepository` (в `domain/repository/`) и реализации `FootballRepositoryImpl` (в `data/repository/`) устраняет утечку слоёв и открывает возможность модульного тестирования.
* **Примерный объём:** **M**
* **Затрагиваемые файлы:**
  * [build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/build.gradle.kts), [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml)
  * [AndroidManifest.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/AndroidManifest.xml) + новый класс `FootballPulseApp : Application()` (`@HiltAndroidApp`)
  * Новые модули `di/NetworkModule.kt`, `di/RepositoryModule.kt` (вместо синглтона [RetrofitInstance.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt))
  * Новый интерфейс `domain/repository/FootballRepository.kt` и реализация в [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt)
  * Все 4 `ViewModel` (`@HiltViewModel` + `SavedStateHandle` вместо ручных `MatchDetailsViewModelFactory` и `TeamDetailsViewModelFactory`) и все 4 `Fragment` + `MainActivity` (`@AndroidEntryPoint`)
* **Критерий готовности (DoD):**
  * Удалён `object RetrofitInstance` и ручные `ViewModelProvider.Factory`.
  * Все `ViewModel` получают интерфейс `FootballRepository` и `SavedStateHandle` через `@Inject constructor(...)`.
  * Пакет `ui` не содержит ни одного импорта из пакета `data`.

### 3. Покрытие Unit-тестами слоя Data и Presentation (Repository, Mappers, ViewModels)
* **Зачем:** Наличие качественных unit-тестов на корутинах (`runTest`, `StandardTestDispatcher`, `Turbine`, `MockWebServer` / `MockK`) — один из главных маркеров зрелого разработчика при ревью портфолио.
* **Примерный объём:** **M**
* **Затрагиваемые файлы:**
  * [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml), [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (добавление `kotlinx-coroutines-test`, `mockk`, `turbine`, `okhttp3:mockwebserver`)
  * Новые тесты в `app/src/test/java/com/artsiom/footballpulse/`:
    * `data/FootballRepositoryTest.kt` (тесты расчёта текущего тура, расчёта формы 5 матчей, сортировки по `utcDate`, обработки HTTP-ошибок)
    * `ui/matches/MatchesViewModelTest.kt`
    * `ui/standings/StandingsViewModelTest.kt`
    * `ui/matchdetails/MatchDetailsViewModelTest.kt`
    * `ui/teamdetails/TeamDetailsViewModelTest.kt`
* **Критерий готовности (DoD):**
  * Удалён шаблонный `ExampleUnitTest.kt`, написано не менее 20 unit-тестов, проверяющих переходы состояний `Loading -> Success` и `Loading -> Error`, переключение туров/лиг и расчёт формы команд.
  * `./gradlew testDebugUnitTest` проходит со 100% успешным результатом.

### 4. Offline-first кеширование в Room и устранение дублирующихся запросов к API
* **Зачем:** Сейчас `room-runtime` и `room-ktx` уже лежат в `app/build.gradle.kts` мёртвым грузом, а приложение скачивает все 380 матчей сезона на каждый чих (и при открытии матчей, и при открытии таблицы), моментально упираясь в лимит `football-data.org` (10 запросов/мин). Кеширование матчей и таблиц в Room позволит мгновенно переключать туры без сети и работать офлайн.
* **Примерный объём:** **L**
* **Затрагиваемые файлы:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (подключение `ksp` и `androidx.room:room-compiler`)
  * Новый пакет `data/local/` (`FootballDatabase.kt`, `MatchDao.kt`, `StandingDao.kt`, `MatchEntity.kt`, `StandingEntity.kt`)
  * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt)
  * [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt), [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt)
* **Критерий готовности (DoD):**
  * Скачанные матчи сезона сохраняются в Room; переключение туров (`goToPreviousMatchday` / `goToNextMatchday`) и расчёт формы в `getStandings` читают данные из локальной БД без повторного скачивания всех матчей сезона.
  * При отсутствии интернета пользователь видит закешированные матчи и турнирную таблицу с индикацией офлайн-режима.

### 5. Типизированная обработка сетевых ошибок (`AppError`), отмена гонок корутин и кнопка `Retry` на экране матчей
* **Зачем:** Сейчас `catch (e: Exception)` перехватывает `CancellationException`, параллельные клики по турам создают гонки корутин, в UI выводятся сырые технические сообщения (`HTTP 429`), а на главном экране матчей нет кнопки `Retry`.
* **Примерный объём:** **S**
* **Затрагиваемые файлы:**
  * Новый класс `domain/model/AppError.kt` (`NoInternet`, `RateLimitExceeded` для HTTP 429, `InvalidApiKey` для HTTP 401/403, `ServerError`, `Unknown`)
  * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt) (проброс `CancellationException` и маппинг кодов HTTP / `IOException` в `AppError`)
  * [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt), [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt), [MatchDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsViewModel.kt), [TeamDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsViewModel.kt) (хранение `private var fetchJob: Job? = null` с `fetchJob?.cancel()` перед новым запросом)
  * [fragment_matches.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_matches.xml) и [MatchesFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesFragment.kt) (добавление `errorLayout` с кнопкой `Retry`)
  * [strings.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/values/strings.xml)
* **Критерий готовности (DoD):**
  * `CancellationException` никогда не поглощается в `catch`.
  * При HTTP 429 пользователь видит понятное сообщение о лимите запросов (Rate Limit), при отсутствии сети — сообщение о проверке подключения, и на всех 4 экранах работает кнопка `Retry`.
  * Быстрое переключение вкладок/туров отменяет предыдущий незавершённый запрос.

### 6. Переход на Jetpack Navigation Component (или корректный FragmentManager) и устранение бага дублирования фрагментов
* **Зачем:** Смешивание `add`/`hide`/`show` и `replace` в `MainActivity` в сочетании с `by lazy` экземплярами фрагментов приводит к наложению фрагментов после смены темы/поворота экрана и некорректной работе стека «Назад» и `BottomNavigationView`.
* **Примерный объём:** **M**
* **Затрагиваемые файлы:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml) (`androidx.navigation:navigation-fragment-ktx`, `navigation-ui-ktx`, плагин Safe Args)
  * Новый навигационный граф `app/src/main/res/navigation/nav_graph.xml`
  * [activity_main.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/activity_main.xml) (`FragmentContainerView` + `NavHostFragment`)
  * [MainActivity.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/MainActivity.kt), [MatchesFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesFragment.kt), [StandingsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsFragment.kt), [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt), [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt)
* **Критерий готовности (DoD):**
  * Убраны касты `(requireActivity() as MainActivity).navigateTo...` и ленивые поля фрагментов в `MainActivity`.
  * Состояние экранов и `BackStack` корректно сохраняются при повороте экрана и переключении темы (Dark/Light).
  * На экранах деталей (`MatchDetails`, `TeamDetails`) `BottomNavigationView` автоматически скрывается или корректно очищает стек при переключении вкладки.

### 7. Включение ViewBinding и перевод RecyclerView-адаптеров на `ListAdapter` + `DiffUtil`
* **Зачем:** В `README.md` заявлен `ViewBinding`, но фактически везде используется `findViewById` и `notifyDataSetChanged()`. Перевод на `ViewBinding` и `ListAdapter` с `DiffUtil.ItemCallback` делает код типобезопасным, избавляет от лишней перерисовки списков и приводит код в соответствие с `README.md`.
* **Примерный объём:** **M**
* **Затрагиваемые файлы:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (`buildFeatures { viewBinding = true }`)
  * [MainActivity.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/MainActivity.kt) и все 4 фрагмента (`MatchesFragment`, `StandingsFragment`, `MatchDetailsFragment`, `TeamDetailsFragment`)
  * [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt), [StandingsAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsAdapter.kt), [LeaguesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/LeaguesAdapter.kt) (плюс перенос `LeaguesAdapter` в общий пакет `ui/common/`)
* **Критерий готовности (DoD):**
  * В проекте не осталось ни одного вызова `findViewById` и `notifyDataSetChanged()`.
  * Все адаптеры наследуются от `ListAdapter` с корректно реализованными `areItemsTheSame` / `areContentsTheSame`.

### 8. Миграция UI на Jetpack Compose (или гибридная миграция начиная с экранов деталей)
* **Зачем:** Jetpack Compose указан в Roadmap проекта (`README.md`) и является главным требованием в большинстве современных вакансий Android-разработчика. Экраны `MatchDetailsFragment` и `TeamDetailsFragment` сейчас собирают строки таблиц вручную через `LinearLayout(context)` и `TextView(context)` на ~200 строк императивного кода — на Compose они записываются декларативно, компактно и с `@Preview`.
* **Примерный объём:** **L** (полная миграция) / **M** (гибридная миграция `MatchDetails` и `TeamDetails` через `ComposeView`)
* **Затрагиваемые файлы:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml) (Compose BOM, `material3`, `ui-tooling-preview`, `lifecycle-runtime-compose`, `coil-compose`)
  * [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt), [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt) (и далее `MatchesScreen`, `StandingsScreen`)
* **Критерий готовности (DoD):**
  * Экраны реализованы на Jetpack Compose с использованием `collectAsStateWithLifecycle()`, `LazyColumn`, Material 3 компонентов и содержат `@Preview` для светлой и тёмной тем.
  * Удалены ручные генераторы `LinearLayout`/`TextView` в Kotlin-коде.

### 9. Перенос доменной логики и форматирования дат из UI в Domain / ViewModel и унификация на `java.time`
* **Зачем:** Сейчас группировка матчей по датам выполняется на UI-потоке внутри `MatchesAdapter.submitList()`, расчёт возраста и группировка состава по амплуа — внутри `TeamDetailsFragment`, зоны ЛЧ/ЛЕ/вылета захардкожены в `StandingsAdapter` одинаково для всех лиг, а в `FootballRepository.computeFormByTeam` сортировка идёт по `matchday` вместо даты матча.
* **Примерный объём:** **M**
* **Затрагиваемые файлы:**
  * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt) (замена `Calendar` на `java.time.LocalDate`/`YearMonth`, сортировка в `computeFormByTeam` по `utcDate`)
  * [League.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/domain/model/League.kt) и [Leagues.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/domain/model/Leagues.kt) (хранение `maxMatchdays`, `clSpots`, `elSpots`, `relegationSpots` как свойств конфигурации каждой лиги)
  * [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt) и [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt) (перенос группировки в `ViewModel` / маппер с использованием `java.time.Instant` и `DateTimeFormatter`)
  * [TeamDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsViewModel.kt) и [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt) (перенос группировки игроков и безопасного `runCatching { LocalDate.parse(...) }` в `ViewModel`/маппер)
  * [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt)
* **Критерий готовности (DoD):**
  * Полностью удалены `java.text.SimpleDateFormat` и `java.util.Calendar`; везде используется потокобезопасный `java.time.*`.
  * Адаптеры и фрагменты получают уже сгруппированные и отформатированные UI-модели из `ViewModel`.
  * Зоны еврокубков и вылета корректно учитывают размер лиги (18 команд в Бундеслиге и Лиге 1 против 20 команд в АПЛ, Ла Лиге и Серии А).

### 10. Отображение составов и запасных (Lineups & Bench) на экране деталей матча
* **Зачем:** Данные `homeLineup`, `awayLineup`, `homeBench`, `awayBench` уже парсятся в `FootballRepository.getMatchDetail`, а в ресурсах уже лежат готовые [item_player_row.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/item_player_row.xml) и `benchCard` в [fragment_match_details.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_match_details.xml#L233-L275), но карточка скрыта (`benchCard.visibility = View.GONE`). Доведение этой фичи до конца (или корректный fallback, если бесплатный тариф API не присылает составы) уберёт мёртвый код и обогатит экран матча.
* **Примерный объём:** **S**
* **Затрагиваемые файлы:**
  * [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt)
  * [fragment_match_details.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_match_details.xml)
  * [item_player_row.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/item_player_row.xml)
* **Критерий готовности (DoD):**
  * Если `homeLineup`/`awayLineup` или `homeBench`/`awayBench` не пусты, на экране деталей матча отображаются карточки стартовых составов и запасных с использованием `item_player_row.xml`; если список пуст — карточки аккуратно скрыты без мёртвых переменных в коде.

### 11. Поддержка SVG-эмблем клубов (`coil-svg`) и единый `ImageLoader` с дисковым кешем
* **Зачем:** Многие эмблемы клубов в `football-data.org` отдаются в формате `.svg`. Сейчас без `coil-svg` вместо них отображается серый щит-заглушка (`ic_placeholder_crest`).
* **Примерный объём:** **S**
* **Затрагиваемые файлы:**
  * [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml), [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (добавление `io.coil-kt:coil-svg:2.6.0` или актуальной версии)
  * `Application`-класс (`ImageLoaderFactory` с регистрацией `SvgDecoder.Factory()` и настройкой `MemoryCache`/`DiskCache`)
* **Критерий готовности (DoD):**
  * Эмблемы в формате `.svg` и `.png` корректно отображаются на карточках матчей, в таблице и на экранах деталей команд и матчей и кешируются на диске.

### 12. Настройка CI/CD (GitHub Actions) и статического анализатора (Detekt / Android Lint)
* **Зачем:** Зелёный бейдж GitHub Actions в `README.md` с автоматической проверкой сборки, unit-тестов и линта при каждом Push / Pull Request сразу выделяет проект среди портфолио-репозиториев.
* **Примерный объём:** **S**
* **Затрагиваемые файлы:**
  * Новый файл `.github/workflows/android_ci.yml`
  * [build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml) (плагин `io.gitlab.arturbosch.detekt`)
  * [README.md](file:///home/antigravity/karzhaneuski/FootballPulse/README.md) (исправление опечатки `%# FootballPulse` в первой строке и добавление бейджа CI)
* **Критерий готовности (DoD):**
  * При пуше в `main` или открытии PR автоматически запускается workflow, выполняющий `./gradlew lintDebug testDebugUnitTest assembleDebug` на JDK 21 с кешированием Gradle.

---

## 5. На чём остановился аудит и что осталось непроверенным

### Что проверено полностью (100% охват):
* Проанализированы **все 25 Kotlin-файлов** (`main`, `test`, `androidTest`), **все 10 XML-файлов разметки**, все файлы ресурсов (`drawable`, `menu`, `values`, `values-night`, `xml`), `AndroidManifest.xml`, `proguard-rules.pro`, конфигурационные файлы Gradle (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `libs.versions.toml`, `gradle-wrapper.properties`, `gradle-daemon-jvm.properties`) и вся история Git-коммитов (21 коммит).

### На чём остановился и что осталось непроверенным:
1. **Фактическое выполнение `./gradlew assembleDebug`, `./gradlew test` и `./gradlew lintDebug` до конца:**
   * В соответствии с условием аудита («Ничего не меняй в коде, только создай `docs/AUDIT.md`») права файла `gradlew` (`100644` -> `100755`) и скрипт `app/build.gradle.kts` (требующий отсутствующий в репозитории `local.properties`) не модифицировались.
   * Кроме того, в изолированной виртуальной машине аудита скачивание дистрибутива `gradle-9.3.1-bin.zip` блокируется пустым системным trustStore (`InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty`), поэтому компиляция APK и прогон Android Lint в runtime не могли быть завершены.
2. **Runtime-проверка на эмуляторе/устройстве с реальным ключом `FOOTBALL_API_KEY`:**
   * Не проверялось живое поведение приложения при ответах реального сервера `api.football-data.org` (в частности, фактическое время ответа и визуальное поведение Edge-to-Edge инсетов `enableEdgeToEdge()` на Android 15 / API 35, где в `fragment_matches.xml` и `fragment_standings.xml` использован фиксированный `paddingTop="36dp"` вместо `WindowInsetsCompat`).
3. **Фактический запуск Release APK после R8:**
   * Проблема с отсутствием `@SerializedName` и пустым `proguard-rules.pro` выявлена статическим анализом кода; краш/потеря данных при десериализации в `release`-сборке требуют проверки после настройки локального окружения с Android SDK.
