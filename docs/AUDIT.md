# FootballPulse Technical Audit

> **Audit date:** November 7, 2026  
> **Repository:** `com.artsiom.footballpulse` (`FootballPulse`)  
> **Audit mode:** Static code analysis + build environment check (no changes made to the project source code).

---

## 1. Actual stack, versions, architecture, and project structure

The data below is based solely on the actual contents of the repository's configuration files and source code (with notes where the real implementation diverges from `README.md` or from the dependencies declared in Gradle).

### 1.1. Build tools and SDK

| Parameter | Actual value | Source |
|---|---|---|
| **Gradle Wrapper** | `9.3.1` (`gradle-9.3.1-bin.zip`) | `gradle/wrapper/gradle-wrapper.properties` |
| **Gradle Daemon Toolchain** | JDK `21` + plugin `org.gradle.toolchains.foojay-resolver-convention:1.0.0` | `gradle/gradle-daemon-jvm.properties`, `settings.gradle.kts` |
| **Android Gradle Plugin (AGP)** | `9.1.0` (`com.android.application`) | `gradle/libs.versions.toml` |
| **Kotlin** | Built into AGP 9.1.0 (the separate `org.jetbrains.kotlin.android` plugin and a Kotlin version are not explicitly declared in `libs.versions.toml` / `build.gradle.kts`; `kotlin.code.style=official`) | `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties` |
| **Java Compatibility** | `JavaVersion.VERSION_11` (`sourceCompatibility` and `targetCompatibility`) | `app/build.gradle.kts` |
| **Namespace / Application ID** | `com.artsiom.footballpulse` | `app/build.gradle.kts` |
| **compileSdk / targetSdk / minSdk** | `35` / `35` / `26` (Android 8.0 — Android 15) | `app/build.gradle.kts` |
| **App version** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts` |
| **Build Features** | `buildConfig = true` (**note:** `viewBinding` is NOT enabled, although `README.md` claims it) | `app/build.gradle.kts` |
| **Release configuration** | `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro` (the rules file is empty — comments only) | `app/build.gradle.kts`, `app/proguard-rules.pro` |

### 1.2. Actual dependencies and plugins

Some dependencies are in the Version Catalog (`gradle/libs.versions.toml`), while others are declared as string literals directly in `app/build.gradle.kts` and the root `build.gradle.kts`:

| Category | Library / Plugin | Version | Declaration method | Actual usage in code |
|---|---|---|---|---|
| **Core / UI** | `androidx.core:core-ktx` | `1.10.1` | `libs.versions.toml` | Used (`ContextCompat`) |
| **Core / UI** | `androidx.appcompat:appcompat` | `1.6.1` | `libs.versions.toml` | Used (`AppCompatActivity`, `AppCompatDelegate`) |
| **Core / UI** | `com.google.android.material:material` | `1.10.0` | `libs.versions.toml` | Used (`MaterialCardView`, `BottomNavigationView`, `FloatingActionButton`, theme `Theme.Material3.DayNight.NoActionBar`) |
| **Core / UI** | `androidx.activity:activity` | `1.8.0` | `libs.versions.toml` | Used (`enableEdgeToEdge()`) |
| **Core / UI** | `androidx.constraintlayout:constraintlayout` | `2.1.4` | `libs.versions.toml` | Used in `fragment_matches.xml`, `fragment_standings.xml`, `item_match.xml` |
| **Network** | `com.squareup.retrofit2:retrofit` | `2.9.0` | String in `app/build.gradle.kts` | Used (`FootballApiService`, `RetrofitInstance`) |
| **JSON** | `com.squareup.retrofit2:converter-gson` | `2.9.0` | String in `app/build.gradle.kts` | Used (`GsonConverterFactory`, `@SerializedName`) |
| **HTTP logger** | `com.squareup.okhttp3:logging-interceptor` | `4.12.0` | String in `app/build.gradle.kts` | Used in `RetrofitInstance` |
| **Concurrency** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.7.3` | String in `app/build.gradle.kts` | Used (`StateFlow`, `MutableStateFlow`, `combine`, `launch`, `suspend`) |
| **Lifecycle** | `androidx.lifecycle:lifecycle-viewmodel-ktx` | `2.7.0` | String in `app/build.gradle.kts` | Used (`ViewModel`, `viewModelScope`, `ViewModelProvider`) |
| **Lifecycle** | `androidx.lifecycle:lifecycle-runtime-ktx` | `2.7.0` | String in `app/build.gradle.kts` | Used (`lifecycleScope`, `repeatOnLifecycle`) |
| **Images** | `io.coil-kt:coil` | `2.6.0` | String in `app/build.gradle.kts` | Used (`ImageView.load(...)`), but **without** `coil-svg` |
| **DB (Room)** | `androidx.room:room-runtime` | `2.6.1` | String in `app/build.gradle.kts` | **NOT used in code** (no `@Entity`/`@Dao`/`@Database`, `ksp`/`kapt` not configured) |
| **DB (Room)** | `androidx.room:room-ktx` | `2.6.1` | String in `app/build.gradle.kts` | **NOT used in code** |
| **DI (Hilt)** | Plugin `com.google.dagger.hilt.android` | `2.50` | String in root `build.gradle.kts` (`apply false`) | **NOT applied in `:app` and NOT used in code** |
| **Unit tests** | `junit:junit` | `4.13.2` | `libs.versions.toml` | Template `ExampleUnitTest` only |
| **UI tests** | `androidx.test.ext:junit` | `1.1.5` | `libs.versions.toml` | Template `ExampleInstrumentedTest` only |
| **UI tests** | `androidx.test.espresso:espresso-core` | `3.5.1` | `libs.versions.toml` | Not used (`ExampleInstrumentedTest` only uses `InstrumentationRegistry`) |

### 1.3. Architecture

The project implements a basic **MVVM (Model — View — ViewModel)** with unidirectional state flow (`StateFlow`) and a **Single Activity** approach (`MainActivity` + 4 fragments), with a number of simplifications:
* **UI Layer (Views):** The classic XML View system (`ConstraintLayout`, `LinearLayout`, `FrameLayout`, `NestedScrollView`, `RecyclerView`). View binding in all Activities, Fragments, and Adapters is done via `findViewById` (contrary to the ViewBinding claimed in `README.md`). On the details screens (`MatchDetailsFragment`, `TeamDetailsFragment`) and in the form badges (`StandingsAdapter`), part of the View tree is created programmatically in Kotlin via `LinearLayout(context)` and `TextView(context)`.
* **Presentation Layer (ViewModels):** 4 `ViewModel` classes (`MatchesViewModel`, `StandingsViewModel`, `MatchDetailsViewModel`, `TeamDetailsViewModel`), each holding its state in a `MutableStateFlow<...UiState>` (`Loading`, `Success`, `Error`). `MatchesViewModel` and `StandingsViewModel` are scoped to `requireActivity()`, while `MatchDetailsViewModel` and `TeamDetailsViewModel` are scoped to the fragment itself via custom `ViewModelProvider.Factory` implementations.
* **Domain Layer:** Consists solely of the `domain.model` package with immutable `data class` models and the `Leagues` object. The `domain` layer has no UseCase classes or repository interfaces.
* **Data Layer:** A single concrete class, `FootballRepository`, which directly calls the `RetrofitInstance.api` singleton (`FootballApiService`), makes HTTP requests to `https://api.football-data.org/v4/...`, maps DTOs to domain models, and computes the current season matchday and team form over the last 5 matches. There is no local storage (DB/cache), apart from `SharedPreferences` in `MainActivity` for the dark theme flag.

### 1.4. Module and package structure

The project is **single-module** (`settings.gradle.kts` includes only `:app`).

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
        │       ├── drawable/                        # 7 vector XML icons
        │       ├── layout/                          # 10 XML layouts (activity, 4 fragment, 5 item)
        │       ├── menu/                            # bottom_nav_menu.xml
        │       ├── values/                          # colors.xml, strings.xml, themes.xml
        │       └── values-night/                    # colors.xml, themes.xml
        ├── test/java/com/artsiom/footballpulse/
        │   └── ExampleUnitTest.kt
        └── androidTest/java/com/artsiom/footballpulse/
            └── ExampleInstrumentedTest.kt
```

---

## 2. Build, test, and lint commands (run results)

Standard Gradle commands for the project:
* Build debug APK: `./gradlew assembleDebug`
* Run unit tests: `./gradlew test` (or `./gradlew testDebugUnitTest`)
* Run Android Lint: `./gradlew lint` (or `./gradlew lintDebug`)

### Actual results of running `./gradlew assembleDebug` and `./gradlew test`

Running the commands as part of the audit revealed **3 consecutive blockers** (two at the repository/build-script level and one at the level of the isolated environment):

1. **The `gradlew` file lacks the execute bit (`+x`) in Git:**
   * In the Git index, `gradlew` is stored with mode `100644` instead of `100755` (`git ls-files -s gradlew` -> `100644 ef07e016... gradlew`).
   * Calling `./gradlew assembleDebug` and `./gradlew test` directly on Linux/macOS fails with **exit code `126`**:
     ```text
     /bin/bash: line 1: ./gradlew: Permission denied
     ```
2. **Unconditional reading of `local.properties` in `app/build.gradle.kts` breaks configuration on a clean clone / in CI:**
   * Lines 3–4 of [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L3-L4) read:
     ```kotlin
     val localProperties = Properties()
     localProperties.load(rootProject.file("local.properties").inputStream())
     ```
   * Since `local.properties` is listed in `.gitignore` and is absent from the repository, running any Gradle task (`assembleDebug`, `test`, `lint`) on a fresh machine or in CI/CD makes the configuration phase fail with `java.io.FileNotFoundException: .../local.properties (No such file or directory)` before compilation or tests even start.
3. **Container network restriction when downloading Gradle 9.3.1:**
   * When working around the permissions issue via `bash ./gradlew assembleDebug` and `bash ./gradlew test`, the Gradle Wrapper tries to download `https://services.gradle.org/distributions/gradle-9.3.1-bin.zip` and fails in the current sandbox with `javax.net.ssl.SSLException: Unexpected error: java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty` (the system `cacerts` for OpenJDK 17 in the sandbox is empty, and the container has neither a pre-cached Gradle 9.3.1 nor Android SDK 35).

---

## 3. Issues found in the code

### 3.1. Outdated, duplicated, and "dead" dependencies + critical R8 bug in Release

1. **Unused ("dead") dependencies in the build:**
   * The root [build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/build.gradle.kts#L4) declares the `com.google.dagger.hilt.android` plugin version `2.50` (`apply false`), but it is not applied in `:app` and is not used in code.
   * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L58-L59) declares `androidx.room:room-runtime:2.6.1` and `androidx.room:room-ktx:2.6.1` (without `room-compiler` / `ksp`), which are not used anywhere in code and only bloat the dependency graph.
2. **Inconsistent use of the Version Catalog (`libs.versions.toml`):**
   * [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml#L1-L24) declares only the basic libraries from the Android Studio template, whereas Retrofit, OkHttp, Coroutines, Lifecycle, Room, and Coil are written as strings directly in `app/build.gradle.kts`.
3. **Outdated library versions and JVM target:**
   * `retrofit:2.9.0` and `converter-gson:2.9.0` (released in 2020; the current line is `2.11.0+`).
   * `core-ktx:1.10.1`, `appcompat:1.6.1`, `material:1.10.0`, `activity:1.8.0`, `constraintlayout:2.1.4`, `lifecycle-*:2.7.0`, `kotlinx-coroutines-android:1.7.3` — all have newer stable releases.
   * `sourceCompatibility` / `targetCompatibility` are set to `JavaVersion.VERSION_11`, even though AGP `9.1.0` and the Gradle daemon already run on JDK 21 (the standard for modern Android projects is JDK 17).
4. **Missing `coil-svg` for loading club crests:**
   * Only the base artifact `io.coil-kt:coil:2.6.0` is included. For many clubs (especially in Bundesliga, La Liga, Serie A), the `football-data.org` API returns crest URLs in `.svg` format (e.g. `https://crests.football-data.org/5.svg`). Without `io.coil-kt:coil-svg` and a registered `SvgDecoder`, such crests cannot be decoded and fall back to `error(R.drawable.ic_placeholder_crest)`.
5. **CRITICAL: broken JSON deserialization in the Release build due to R8 and the lack of `@SerializedName` / ProGuard rules:**
   * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L29-L36) enables `isMinifyEnabled = true` and `isShrinkResources = true` for `release`, but [app/proguard-rules.pro](file:///home/antigravity/karzhaneuski/FootballPulse/app/proguard-rules.pro#L1-L21) is completely empty.
   * `@SerializedName` annotations are present **only** in [MatchDetailDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchDetailDto.kt#L5-L27) and [TeamDetailsDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/TeamDetailsDto.kt#L5-L24).
   * [MatchesResponse.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchesResponse.kt#L3-L5), [MatchDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchDto.kt#L3-L11), [TeamDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/TeamDto.kt#L3-L7), [ScoreDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/ScoreDto.kt#L3-L16) (`ScoreDto` is also not a `data class`), and [StandingsDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/StandingsDto.kt#L3-L23) have **no** `@SerializedName` annotations.
   * In a `release` build, the R8 obfuscator will rename the fields of these DTOs to `a`, `b`, `c`, after which `Gson` cannot map JSON keys to class fields — the Release build will either show empty lists or crash with a `NullPointerException`.

### 3.2. Missing tests and testability problems

1. **No real tests at all:**
   * `src/test` contains only the template [ExampleUnitTest.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/test/java/com/artsiom/footballpulse/ExampleUnitTest.kt#L12-L17) (`2 + 2 == 4`), and `src/androidTest` only [ExampleInstrumentedTest.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/androidTest/java/com/artsiom/footballpulse/ExampleInstrumentedTest.kt#L16-L24).
   * There is not a single test for the repository's business logic (`computeFormByTeam`, current matchday selection in `getCurrentMatchdayResult`, `computeCurrentSeason`, DTO -> Domain mapping) or for the `ViewModel`s.
2. **Tight coupling that blocks writing unit tests:**
   * All 4 `ViewModel`s create a `private val repository = FootballRepository()` instance internally (see [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt#L13), [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt#L13), [MatchDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsViewModel.kt#L13), [TeamDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsViewModel.kt#L20)) instead of receiving an interface through the constructor.
   * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L23) accesses the global `RetrofitInstance.api` singleton directly and uses the system `Calendar.getInstance()`.
   * The test dependencies lack `kotlinx-coroutines-test`, `mockk` / `mockito`, `turbine`, and `mockwebserver`.

### 3.3. API keys and security configuration

1. **Positive:** The current code and the entire Git commit history contain no hardcoded plain-text `football-data.org` token. Commit `188bfe1` removed logging of the key to Logcat (`Log.d("AUTH_DEBUG", "API key = ...")`), added `redactHeader("X-Auth-Token")`, and restricted `HttpLoggingInterceptor.Level.BODY` with the `BuildConfig.DEBUG` flag (see [RetrofitInstance.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt#L11-L26)).
2. **Problem 1 — Build failure when `local.properties` is missing and `"null"` substitution:**
   * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts#L3-L4) has no `.exists()` check before reading `local.properties` and no fallback to the `System.getenv("FOOTBALL_API_KEY")` environment variable.
   * If `local.properties` exists but does not define `FOOTBALL_API_KEY`, the line `buildConfigField("String", "FOOTBALL_API_KEY", "\"${localProperties["FOOTBALL_API_KEY"]}\"")` (line 21) generates the string literal `"null"` in `BuildConfig.java`. As a result, the app silently sends the header `X-Auth-Token: null`.
3. **Problem 2 — The key can be extracted from the client APK:**
   * Any value in `BuildConfig.FOOTBALL_API_KEY` is compiled into a plain constant inside `classes.dex` and is easily extracted with `jadx` or `strings`. For a learning/portfolio project on the free tier this is acceptable, but it should be explicitly documented or guarded by an empty-key check.

### 3.4. Layer leaks, architectural violations, and UI/navigation bugs

1. **Leak of the `data` layer into `ui` (no dependency inversion):**
   * The `ViewModel` classes in the `ui.*` package directly import and instantiate `com.artsiom.footballpulse.data.FootballRepository`. The `domain` package has no repository interface.
2. **Leak of domain and presentation logic into UI components (Fragments and RecyclerView.Adapter):**
   * **In [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt#L20-L34):** the number of matchdays per league (`leagueMaxMatchdays`: 38 or 34) and the default season year (`private var currentSeason: Int = 2025`) are hardcoded.
   * **In [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt#L39-L57):** right inside `submitList()`, on the main (UI) thread, ISO dates are parsed via `SimpleDateFormat`, items are grouped with `groupBy`, sorted with `toSortedMap()`, and domain models are transformed into `MatchListItem` UI elements.
   * **In [StandingsAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsAdapter.kt#L39-L42) and lines 160–168:** the rules for European competition zones and the relegation zone (`CHAMPIONS_LEAGUE_SPOTS = 4`, `EUROPA_LEAGUE_POSITION = 5`, `RELEGATION_ZONE_SIZE = 3`) are hardcoded in the UI adapter identically for all 5 leagues (even though the Bundesliga and Ligue 1 have 18 teams, and their relegation/play-off rules differ from leagues with 20 teams). The adapter also parses the form string `standing.form` (lines 214–225).
   * **In [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt#L197-L347):** the fragment itself contains business rules for grouping player positions (`normalizePosition`), sorting by shirt number, computing age (`calculateAge` via `LocalDate.parse(dateOfBirth)` **without `try/catch`**, which will crash with a `DateTimeParseException` on a non-standard date format), and a 50-country `countryFlag` dictionary.
3. **Inconsistent date APIs and a thread-unsafe `SimpleDateFormat`:**
   * The project uses **3 different date APIs** at once:
     * `java.util.Calendar` in [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L206);
     * `java.text.SimpleDateFormat` in a `companion object` (static singletons, which are not thread-safe!) in [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt#L29-L34) and [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt#L41-L45);
     * `java.time.LocalDate` / `Period` in [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt#L289-L293) (with `minSdk = 26`, the `java.time` package is natively available throughout the project).
4. **Manual fragment navigation and theme switching bugs in `MainActivity`:**
   * In [MainActivity.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/MainActivity.kt#L22-L84), the tab fragments are created as Activity fields (`private val matchesFragment by lazy { MatchesFragment() }`) and switched via `add`/`hide`/`show` (`showFragment()`).
   * When navigating to match or team details, `.replace(R.id.fragmentContainer, ...).addToBackStack(null)` is called. Calling `replace` on the same container removes both tab fragments (`matchesFragment` and `standingsFragment`) from it, while the `BottomNavigationView` and the theme-switch `FloatingActionButton` remain visible on top of the details screens. If the user taps a tab in the `BottomNavigationView` while on a details screen, `showFragment()` is called, which hides the details screen but leaves it in the `BackStack` — the Back button then behaves incorrectly.
   * When the theme is switched (`AppCompatDelegate.setDefaultNightMode(...)`) or the screen is rotated, `MainActivity` is recreated. `FragmentManager` automatically restores the old fragment instances, but the `by lazy` fields in the new `MainActivity` instance create **new** `MatchesFragment` and `StandingsFragment` instances, so switching tabs adds duplicate fragments to the container.
5. **Unfinished functionality ("dead" match lineup code):**
   * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L127-L143) parses and sorts `homeLineup`, `awayLineup`, `homeBench`, `awayBench`; the file [item_player_row.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/item_player_row.xml#L1-L35) was created in `res/layout/`, and [fragment_match_details.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_match_details.xml#L233-L275) has `benchCard` and `benchContainer`. However, [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt#L76) sets `benchCard.visibility = View.GONE`, and `item_player_row.xml` is not used anywhere.
6. **Hardcoded strings in code and XML + use of `notifyDataSetChanged()`:**
   * [strings.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/values/strings.xml#L1-L3) declares only one string (`app_name`). All other labels in XML and Kotlin code (`"Matchday $matchday"`, `"Retry"`, `"← Back"`, `"Club Info"`, `"Squad"`, `"First Half"`, `"Full Time"`, etc.) are hardcoded.
   * All 3 adapters (`MatchesAdapter`, `StandingsAdapter`, `LeaguesAdapter`) use `notifyDataSetChanged()` instead of `ListAdapter` and `DiffUtil`.

### 3.5. Network error handling and inefficient API usage

1. **Catching `Exception` breaks Structured Concurrency (`CancellationException`) and creates request races (Race Conditions):**
   * All four `ViewModel`s and [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L90) use a `try { ... } catch (e: Exception)` block, which also catches `kotlinx.coroutines.CancellationException`.
   * In [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt#L41-L85) and [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt#L46-L56), when the user quickly switches league tabs or taps the `‹` / `›` buttons (changing the matchday), the previous `Job` is not cancelled — a new coroutine is launched in parallel with the old one, causing a state race (`_uiState.value` is overwritten by whichever response arrives later).
2. **Raw technical errors in the UI and no `Retry` button on the main screen (`MatchesFragment`):**
   * The repository throws `Exception("HTTP ${response.code()}: ${response.message()}")` (where `response.message()` is often empty on HTTP/2), and with no internet it throws `UnknownHostException` / `SocketTimeoutException`. The user sees the raw JVM/HTTP exception text on screen.
   * The `StandingsFragment`, `MatchDetailsFragment`, and `TeamDetailsFragment` screens have a `Retry` button, but the main screen [fragment_matches.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_matches.xml#L102-L110) has only a `TextView` (`errorText`) **without a `Retry` button**.
3. **Excessive network requests that quickly exhaust the free API limit (10 requests/min on `football-data.org`):**
   * The `getCurrentMatchdayResult(leagueCode)` method in [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L21-L54) downloads **all matches of the season** (`GET v4/competitions/{code}/matches?season=...` — 380 matches for the Premier League/La Liga/Serie A), picks the 10 matches of the current matchday, and **discards the other 370 matches** (not even keeping them in memory). When the next/previous matchday button is pressed (`getMatchesByMatchday`), the app goes to the network again for data it has just downloaded.
   * The `getStandings(leagueCode)` method in [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L76-L92) makes **2 network requests at once** every time the table is opened: `getStandings(leagueCode)` and, again, a download of all 380 season matches via `getMatches(leagueCode, season)` to compute form (`computeFormByTeam`). Just 3–4 switches between leagues are enough to get an `HTTP 429 (Too Many Requests)` error.
4. **Logic error in team form calculation with postponed matches:**
   * In [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt#L174-L176), finished matches are sorted by matchday number (`sortedBy { it.matchday ?: Int.MAX_VALUE }`) rather than by actual play date (`utcDate`). If a matchday-10 match was postponed and played after matchday 25, sorting by `matchday` places it in the past, and the form over the last 5 matches (`takeLast(5)`) is computed incorrectly.
5. **No timeouts in `OkHttpClient`:**
   * [RetrofitInstance.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt#L17-L28) uses only the default `OkHttpClient.Builder()` settings, with no explicit timeout configuration or HTTP response cache.

---

## 4. Top 12 improvements to strengthen the project in an Android developer portfolio

Below is a prioritized list of 12 concrete improvements that would turn the project from a learning prototype into a strong portfolio case at the Middle/Senior Android Developer level.

### 1. Fix the build configuration (`local.properties`, `gradlew`) and protect DTOs for R8 in Release
* **Why:** Right now the project will not build on a clean clone or in CI without a `local.properties` file, `gradlew` has no execute permission, and the Release build with `isMinifyEnabled = true` is broken because DTOs are obfuscated without `@SerializedName`. Employers often check both a clean build from the repository and the correctness of ProGuard/R8 rules.
* **Estimated size:** **S**
* **Affected files:**
  * `gradlew` (mode `100755` in Git)
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts)
  * [app/proguard-rules.pro](file:///home/antigravity/karzhaneuski/FootballPulse/app/proguard-rules.pro)
  * [MatchesResponse.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchesResponse.kt), [MatchDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/MatchDto.kt), [TeamDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/TeamDto.kt), [ScoreDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/ScoreDto.kt), [StandingsDto.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/StandingsDto.kt)
* **Definition of Done (DoD):**
  * `local.properties` is read safely (`if (localPropertiesFile.exists())`), with a fallback to `System.getenv("FOOTBALL_API_KEY") ?: ""`.
  * All classes in `data/remote/` are annotated with `@SerializedName` (and `ScoreDto`, `FullTimeDto`, `HalfTimeDto` are converted to `data class`), and `proguard-rules.pro` gets the rules `-keep class com.artsiom.footballpulse.data.remote.** { *; }` plus rules for Gson/Retrofit.
  * `./gradlew assembleRelease` and `./gradlew assembleDebug` succeed even when `local.properties` is missing.

### 2. Full dependency injection (Dagger Hilt) and dependency inversion (Domain Repository Interface)
* **Why:** The root `build.gradle.kts` already declares the Hilt plugin, but DI is not set up, and the `ViewModel`s create `FootballRepository()` themselves. Proper DI with the `FootballRepository` interface split out (in `domain/repository/`) and the `FootballRepositoryImpl` implementation (in `data/repository/`) removes the layer leak and enables unit testing.
* **Estimated size:** **M**
* **Affected files:**
  * [build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/build.gradle.kts), [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml)
  * [AndroidManifest.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/AndroidManifest.xml) + a new `FootballPulseApp : Application()` class (`@HiltAndroidApp`)
  * New modules `di/NetworkModule.kt`, `di/RepositoryModule.kt` (replacing the [RetrofitInstance.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt) singleton)
  * A new interface `domain/repository/FootballRepository.kt` and its implementation in [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt)
  * All 4 `ViewModel`s (`@HiltViewModel` + `SavedStateHandle` instead of the manual `MatchDetailsViewModelFactory` and `TeamDetailsViewModelFactory`) and all 4 `Fragment`s + `MainActivity` (`@AndroidEntryPoint`)
* **Definition of Done (DoD):**
  * `object RetrofitInstance` and the manual `ViewModelProvider.Factory` classes are removed.
  * All `ViewModel`s receive the `FootballRepository` interface and `SavedStateHandle` via `@Inject constructor(...)`.
  * The `ui` package contains no imports from the `data` package.

### 3. Unit test coverage of the Data and Presentation layers (Repository, Mappers, ViewModels)
* **Why:** High-quality coroutine unit tests (`runTest`, `StandardTestDispatcher`, `Turbine`, `MockWebServer` / `MockK`) are one of the main markers of a mature developer when a portfolio is reviewed.
* **Estimated size:** **M**
* **Affected files:**
  * [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml), [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (adding `kotlinx-coroutines-test`, `mockk`, `turbine`, `okhttp3:mockwebserver`)
  * New tests in `app/src/test/java/com/artsiom/footballpulse/`:
    * `data/FootballRepositoryTest.kt` (tests for current matchday calculation, 5-match form calculation, sorting by `utcDate`, HTTP error handling)
    * `ui/matches/MatchesViewModelTest.kt`
    * `ui/standings/StandingsViewModelTest.kt`
    * `ui/matchdetails/MatchDetailsViewModelTest.kt`
    * `ui/teamdetails/TeamDetailsViewModelTest.kt`
* **Definition of Done (DoD):**
  * The template `ExampleUnitTest.kt` is removed and at least 20 unit tests are written, covering `Loading -> Success` and `Loading -> Error` state transitions, matchday/league switching, and team form calculation.
  * `./gradlew testDebugUnitTest` passes with a 100% success rate.

### 4. Offline-first caching in Room and elimination of duplicate API requests
* **Why:** Right now `room-runtime` and `room-ktx` sit in `app/build.gradle.kts` as dead weight, while the app downloads all 380 season matches at every turn (both when opening matches and when opening the table), instantly hitting the `football-data.org` limit (10 requests/min). Caching matches and tables in Room would allow switching matchdays instantly without the network and working offline.
* **Estimated size:** **L**
* **Affected files:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (adding `ksp` and `androidx.room:room-compiler`)
  * New package `data/local/` (`FootballDatabase.kt`, `MatchDao.kt`, `StandingDao.kt`, `MatchEntity.kt`, `StandingEntity.kt`)
  * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt)
  * [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt), [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt)
* **Definition of Done (DoD):**
  * Downloaded season matches are stored in Room; matchday switching (`goToPreviousMatchday` / `goToNextMatchday`) and form calculation in `getStandings` read data from the local DB without re-downloading all season matches.
  * With no internet, the user sees cached matches and the standings table with an offline mode indicator.

### 5. Typed network error handling (`AppError`), cancelling coroutine races, and a `Retry` button on the matches screen
* **Why:** Right now `catch (e: Exception)` catches `CancellationException`, parallel taps on matchdays create coroutine races, raw technical messages (`HTTP 429`) are shown in the UI, and the main matches screen has no `Retry` button.
* **Estimated size:** **S**
* **Affected files:**
  * New class `domain/model/AppError.kt` (`NoInternet`, `RateLimitExceeded` for HTTP 429, `InvalidApiKey` for HTTP 401/403, `ServerError`, `Unknown`)
  * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt) (rethrowing `CancellationException` and mapping HTTP codes / `IOException` to `AppError`)
  * [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt), [StandingsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt), [MatchDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsViewModel.kt), [TeamDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsViewModel.kt) (keeping `private var fetchJob: Job? = null` and calling `fetchJob?.cancel()` before each new request)
  * [fragment_matches.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_matches.xml) and [MatchesFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesFragment.kt) (adding an `errorLayout` with a `Retry` button)
  * [strings.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/values/strings.xml)
* **Definition of Done (DoD):**
  * `CancellationException` is never swallowed in `catch`.
  * On HTTP 429 the user sees a clear rate limit message, with no network a message asking to check the connection, and the `Retry` button works on all 4 screens.
  * Quickly switching tabs/matchdays cancels the previous unfinished request.

### 6. Move to Jetpack Navigation Component (or a correct FragmentManager setup) and fix the duplicate fragment bug
* **Why:** Mixing `add`/`hide`/`show` and `replace` in `MainActivity`, combined with `by lazy` fragment instances, causes fragments to overlap after a theme change/screen rotation and breaks the Back stack and `BottomNavigationView`.
* **Estimated size:** **M**
* **Affected files:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml) (`androidx.navigation:navigation-fragment-ktx`, `navigation-ui-ktx`, Safe Args plugin)
  * New navigation graph `app/src/main/res/navigation/nav_graph.xml`
  * [activity_main.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/activity_main.xml) (`FragmentContainerView` + `NavHostFragment`)
  * [MainActivity.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/MainActivity.kt), [MatchesFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesFragment.kt), [StandingsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsFragment.kt), [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt), [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt)
* **Definition of Done (DoD):**
  * The `(requireActivity() as MainActivity).navigateTo...` casts and the lazy fragment fields in `MainActivity` are removed.
  * Screen state and the `BackStack` are preserved correctly on screen rotation and theme switching (Dark/Light).
  * On the details screens (`MatchDetails`, `TeamDetails`), the `BottomNavigationView` is either hidden automatically or correctly clears the stack when a tab is switched.

### 7. Enable ViewBinding and migrate RecyclerView adapters to `ListAdapter` + `DiffUtil`
* **Why:** `README.md` claims `ViewBinding`, but in practice `findViewById` and `notifyDataSetChanged()` are used everywhere. Moving to `ViewBinding` and `ListAdapter` with `DiffUtil.ItemCallback` makes the code type-safe, avoids needless list redraws, and brings the code in line with `README.md`.
* **Estimated size:** **M**
* **Affected files:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (`buildFeatures { viewBinding = true }`)
  * [MainActivity.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/MainActivity.kt) and all 4 fragments (`MatchesFragment`, `StandingsFragment`, `MatchDetailsFragment`, `TeamDetailsFragment`)
  * [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt), [StandingsAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsAdapter.kt), [LeaguesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/LeaguesAdapter.kt) (plus moving `LeaguesAdapter` to the shared `ui/common/` package)
* **Definition of Done (DoD):**
  * No `findViewById` or `notifyDataSetChanged()` calls remain in the project.
  * All adapters extend `ListAdapter` with correctly implemented `areItemsTheSame` / `areContentsTheSame`.

### 8. Migrate the UI to Jetpack Compose (or a hybrid migration starting with the details screens)
* **Why:** Jetpack Compose is listed in the project Roadmap (`README.md`) and is the top requirement in most modern Android developer job postings. The `MatchDetailsFragment` and `TeamDetailsFragment` screens currently build table rows by hand via `LinearLayout(context)` and `TextView(context)` in ~200 lines of imperative code — in Compose this is written declaratively, compactly, and with `@Preview`.
* **Estimated size:** **L** (full migration) / **M** (hybrid migration of `MatchDetails` and `TeamDetails` via `ComposeView`)
* **Affected files:**
  * [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml) (Compose BOM, `material3`, `ui-tooling-preview`, `lifecycle-runtime-compose`, `coil-compose`)
  * [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt), [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt) (and later `MatchesScreen`, `StandingsScreen`)
* **Definition of Done (DoD):**
  * The screens are implemented in Jetpack Compose using `collectAsStateWithLifecycle()`, `LazyColumn`, and Material 3 components, and include `@Preview` for light and dark themes.
  * The manual `LinearLayout`/`TextView` generators in Kotlin code are removed.

### 9. Move domain logic and date formatting from the UI to Domain / ViewModel and unify on `java.time`
* **Why:** Currently, grouping matches by date runs on the UI thread inside `MatchesAdapter.submitList()`, age calculation and grouping the squad by position happen inside `TeamDetailsFragment`, the Champions League/Europa League/relegation zones are hardcoded in `StandingsAdapter` identically for all leagues, and `FootballRepository.computeFormByTeam` sorts by `matchday` instead of match date.
* **Estimated size:** **M**
* **Affected files:**
  * [FootballRepository.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/data/FootballRepository.kt) (replace `Calendar` with `java.time.LocalDate`/`YearMonth`, sort by `utcDate` in `computeFormByTeam`)
  * [League.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/domain/model/League.kt) and [Leagues.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/domain/model/Leagues.kt) (storing `maxMatchdays`, `clSpots`, `elSpots`, `relegationSpots` as per-league configuration properties)
  * [MatchesViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt) and [MatchesAdapter.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesAdapter.kt) (moving the grouping into the `ViewModel` / a mapper using `java.time.Instant` and `DateTimeFormatter`)
  * [TeamDetailsViewModel.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsViewModel.kt) and [TeamDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsFragment.kt) (moving player grouping and a safe `runCatching { LocalDate.parse(...) }` into the `ViewModel`/mapper)
  * [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt)
* **Definition of Done (DoD):**
  * `java.text.SimpleDateFormat` and `java.util.Calendar` are removed entirely; thread-safe `java.time.*` is used everywhere.
  * Adapters and fragments receive already grouped and formatted UI models from the `ViewModel`.
  * The European competition and relegation zones correctly account for league size (18 teams in the Bundesliga and Ligue 1 vs. 20 teams in the Premier League, La Liga, and Serie A).

### 10. Show lineups and bench (Lineups & Bench) on the match details screen
* **Why:** The `homeLineup`, `awayLineup`, `homeBench`, `awayBench` data is already parsed in `FootballRepository.getMatchDetail`, and the resources already contain a ready-made [item_player_row.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/item_player_row.xml) and a `benchCard` in [fragment_match_details.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_match_details.xml#L233-L275), but the card is hidden (`benchCard.visibility = View.GONE`). Finishing this feature (or a proper fallback if the free API tier does not return lineups) would remove dead code and enrich the match screen.
* **Estimated size:** **S**
* **Affected files:**
  * [MatchDetailsFragment.kt](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsFragment.kt)
  * [fragment_match_details.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/fragment_match_details.xml)
  * [item_player_row.xml](file:///home/antigravity/karzhaneuski/FootballPulse/app/src/main/res/layout/item_player_row.xml)
* **Definition of Done (DoD):**
  * If `homeLineup`/`awayLineup` or `homeBench`/`awayBench` are non-empty, the match details screen shows starting lineup and bench cards using `item_player_row.xml`; if a list is empty, the cards are cleanly hidden with no dead variables left in the code.

### 11. SVG club crest support (`coil-svg`) and a single `ImageLoader` with a disk cache
* **Why:** Many club crests on `football-data.org` are served as `.svg`. Currently, without `coil-svg`, a gray shield placeholder (`ic_placeholder_crest`) is shown instead.
* **Estimated size:** **S**
* **Affected files:**
  * [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml), [app/build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/app/build.gradle.kts) (adding `io.coil-kt:coil-svg:2.6.0` or the current version)
  * `Application` class (`ImageLoaderFactory` registering `SvgDecoder.Factory()` and configuring `MemoryCache`/`DiskCache`)
* **Definition of Done (DoD):**
  * Crests in `.svg` and `.png` format display correctly on match cards, in the table, and on the team and match details screens, and are cached on disk.

### 12. Set up CI/CD (GitHub Actions) and a static analyzer (Detekt / Android Lint)
* **Why:** A green GitHub Actions badge in `README.md`, with an automatic build, unit test, and lint check on every Push / Pull Request, immediately sets the project apart among portfolio repositories.
* **Estimated size:** **S**
* **Affected files:**
  * New file `.github/workflows/android_ci.yml`
  * [build.gradle.kts](file:///home/antigravity/karzhaneuski/FootballPulse/build.gradle.kts), [gradle/libs.versions.toml](file:///home/antigravity/karzhaneuski/FootballPulse/gradle/libs.versions.toml) (the `io.gitlab.arturbosch.detekt` plugin)
  * [README.md](file:///home/antigravity/karzhaneuski/FootballPulse/README.md) (fixing the `%# FootballPulse` typo on the first line and adding a CI badge)
* **Definition of Done (DoD):**
  * On a push to `main` or when a PR is opened, a workflow runs automatically that executes `./gradlew lintDebug testDebugUnitTest assembleDebug` on JDK 21 with Gradle caching.

---

## 5. Where the audit stopped and what remains unchecked

### Fully checked (100% coverage):
* Analyzed **all 25 Kotlin files** (`main`, `test`, `androidTest`), **all 10 XML layout files**, all resource files (`drawable`, `menu`, `values`, `values-night`, `xml`), `AndroidManifest.xml`, `proguard-rules.pro`, the Gradle configuration files (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `libs.versions.toml`, `gradle-wrapper.properties`, `gradle-daemon-jvm.properties`), and the entire Git commit history (21 commits).

### Where it stopped and what remains unchecked:
1. **Actually running `./gradlew assembleDebug`, `./gradlew test`, and `./gradlew lintDebug` to completion:**
   * In accordance with the audit brief ("Change nothing in the code, only create `docs/AUDIT.md`"), the `gradlew` file mode (`100644` -> `100755`) and the `app/build.gradle.kts` script (which requires a `local.properties` absent from the repository) were not modified.
   * In addition, in the audit's isolated virtual machine, downloading the `gradle-9.3.1-bin.zip` distribution is blocked by an empty system trustStore (`InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty`), so APK compilation and the Android Lint run could not be completed at runtime.
2. **Runtime check on an emulator/device with a real `FOOTBALL_API_KEY`:**
   * The live behavior of the app against real `api.football-data.org` responses was not verified (in particular, actual response time and the visual behavior of Edge-to-Edge insets from `enableEdgeToEdge()` on Android 15 / API 35, where `fragment_matches.xml` and `fragment_standings.xml` use a fixed `paddingTop="36dp"` instead of `WindowInsetsCompat`).
3. **Actually running the Release APK after R8:**
   * The problem of missing `@SerializedName` and an empty `proguard-rules.pro` was identified by static code analysis; the crash/data loss during deserialization in the `release` build needs verification after setting up a local environment with the Android SDK.
