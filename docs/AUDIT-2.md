# FootballPulse — Second Technical Audit (Post-Modernization)

> **Audit date:** November 11, 2026  
> **Evaluated revision:** `origin/main` at commit `083cd60` (post-Jetpack Compose migration `#76`–`#81`, manual DI `#33`, unit test suite `#15`, presentation mappers `#14`, Retrofit 3 / dependency upgrades `#38`–`#44`)  
> **In-flight PRs accounted for (not re-reported as defects):**
> - **PR #92 (`#82`):** Upgrades AGP `9.1.0` → `9.5.1`, `compileSdk` `36` → `37`, `core-ktx` `1.18.0` → `1.19.1`, Lifecycle `2.10.0` → `2.11.0`, and removes `compileSdk 37` Dependabot ignore rules.
> - **PR #93:** Deletes template `ExampleInstrumentedTest.kt`, updates `SystemBarInsetsTest.kt` to `junit4.v2.createAndroidComposeRule`, and adds `@param:` targets to `Tab` resource annotations in `AppShell.kt`.
> - **PR #94 (`#17`):** Architectural proposal `docs/proposals/17-offline-cache.md` for two-phase caching (in-memory + Room).
> - **PR #95 (`#34`):** Migrates manual DI (`AppContainer`) to Dagger Hilt `2.59.2` + KSP `2.3.13` (`@HiltAndroidApp`, `@AndroidEntryPoint`, `NetworkModule`, `RepositoryModule`, `@HiltViewModel` with `SavedStateHandle`).
> - **PR #97 (`#45`):** Raises `targetSdk` from `35` to `36`.
> - **PR #98:** Adds 23 instrumented Compose UI tests across `MatchesScreenTest`, `StandingsScreenTest`, `MatchDetailsScreenTest`, and `TeamDetailsScreenTest`.
>
> **Open tracked issues noted where relevant:** `#1` (DTO R8 protection — already implemented on `main`, issue still open), `#12` (`AppError` typed error mapping), `#17` (offline cache decision), `#96` (`colorResource` ignoring in-app dark theme override).

---

## Executive Summary

Since the first audit (`docs/AUDIT.md`), the codebase has undergone a major modernization: the XML View layer (`69` `findViewById` calls, `3` `RecyclerView` adapters, and `4` `Fragment`s) has been completely replaced with Jetpack Compose and Navigation Compose (`083cd60`), `java.util.Calendar` and `SimpleDateFormat` have been replaced with `java.time`, `FootballRepository` has been inverted behind a domain interface with 80 JVM unit tests, and Hilt (`#95`), AGP `9.5.1` / `compileSdk 37` (`#92`), `targetSdk 36` (`#97`), and screen-level Compose tests (`#98`) are in open PRs.

This second audit evaluates the resulting Compose + Coroutines + Retrofit 3 codebase across six dimensions: **Security & Release Readiness**, **Networking & API Limits**, **Compose Quality**, **Accessibility**, **Architecture & Code Health**, and **Testing Gaps**. Across these categories, we identified **31 concrete findings** (**7 High**, **15 Medium**, **9 Low**) and compiled a prioritized backlog of **14 actionable follow-up issues**.

---

## 1. Security & Release Readiness

### Summary of Current Setup
- **API key resolution (`app/build.gradle.kts:3-13`, `32`):** Resolved in priority order `local.properties` → `System.getenv("FOOTBALL_API_KEY")` → `""`, and injected via `buildConfigField("String", "FOOTBALL_API_KEY", "\"$footballApiKey\"")`.
- **Log redaction (`app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt:19-23`, `31-33`):** `HttpLoggingInterceptor` is set to `Level.BODY` only when `BuildConfig.DEBUG` is `true` (`Level.NONE` in `release`), `redactHeader("X-Auth-Token")` is enabled, and `Log.d("AUTH_DEBUG", ...)` is guarded by `if (BuildConfig.DEBUG)`.

### Findings

#### SEC-1 — API key is compiled into `classes.dex` in plain text and is trivially extractable from any APK
- **Severity:** **Medium** *(inherent to client-side free-tier keys, but undocumented in release config and unguarded against raw extraction)*
- **Evidence:**
  - `app/build.gradle.kts:32` generates `public static final String FOOTBALL_API_KEY = "..."` in `com.artsiom.footballpulse.BuildConfig`.
  - `app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt:34-36` references `BuildConfig.FOOTBALL_API_KEY` directly in the OkHttp interceptor:
    ```kotlin
    val request = chain.request().newBuilder()
        .addHeader("X-Auth-Token", BuildConfig.FOOTBALL_API_KEY)
        .build()
    ```
  - Under R8 full mode (`app/build.gradle.kts:42`), R8 inlines the constant string literal directly into the compiled interceptor method in `classes.dex`. Running `strings classes.dex | grep -E '^[a-f0-9]{32}$'` or opening the APK in `jadx` immediately reveals the token.
- **Fix suggestion:**
  - For a public Play Store or GitHub release APK, never ship a personal `football-data.org` key in the binary: either proxy requests through a lightweight serverless worker (e.g., Cloudflare Worker / Firebase Functions holding the secret and enforcing per-IP caching) or document in `README.md` / release notes that public release artifacts are built with an empty key unless a proxy is configured.
  - Trim and sanitize `footballApiKey` in `app/build.gradle.kts:10-13` (`.trim().replace("\\", "\\\\").replace("\"", "\\\"")`) so accidental quotes or trailing whitespace in `local.properties` do not break compilation or send malformed `X-Auth-Token` headers.

#### SEC-2 — Overly broad R8 `-keep` rule prevents shrinking/obfuscation of `RetrofitInstance`, while DTOs lack no-arg constructors (`Unsafe` allocation) and stack-trace line numbers are stripped
- **Severity:** **Medium**
- **Evidence:**
  - `app/proguard-rules.pro:24` specifies:
    ```proguard
    -keep class com.artsiom.footballpulse.data.remote.** { *; }
    ```
    Because `RetrofitInstance` (`app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt:11`) and `FootballApiService` (`FootballApiService.kt:7`) live in `com.artsiom.footballpulse.data.remote` alongside the DTOs, this wildcard rule forces R8 to preserve `RetrofitInstance`, its companion constants, its interceptor lambdas, and all unused `copy`/`componentN` methods on the 12 DTO classes (`MatchDetailDto.kt:5`, `MatchDto.kt:5`, `MatchesResponse.kt:5`, `ScoreDto.kt:5-18`, `StandingsDto.kt:5-25`, `TeamDetailsDto.kt:5-24`, `TeamDto.kt:5`) without shrinking or obfuscation.
  - All 12 DTO classes declare non-nullable constructor properties (`val utcDate: String`, `val status: String`, `val name: String`, etc.) **without default values**. Consequently, Kotlin does not generate a no-arg constructor `<init>()`, forcing Gson (`converter-gson:3.0.0`, `gradle/libs.versions.toml:35`) to instantiate DTOs via `sun.misc.Unsafe.allocateInstance(...)`. Because `Unsafe` bypasses Kotlin's constructor null checks, if the API ever returns `"utcDate": null` or `"status": null`, Gson silently injects `null` into a non-null Kotlin property, causing a delayed `NullPointerException` in `FootballRepositoryImpl.kt:32` or `173` instead of a clean deserialization failure.
  - `app/proguard-rules.pro:17-21` leaves `-keepattributes SourceFile,LineNumberTable` and `-renamesourcefileattribute SourceFile` commented out, so R8 release crash stack traces have no line numbers.
- **Fix suggestion:**
  - Narrow `app/proguard-rules.pro:24` to keep only DTO fields and constructors (or move DTOs to `data.remote.dto`), e.g. `-keepclassmembers class com.artsiom.footballpulse.data.remote.*Dto, com.artsiom.footballpulse.data.remote.*Response { <fields>; <init>(...); }`.
  - Add `-keep class * extends com.google.gson.reflect.TypeToken` and uncomment `-keepattributes SourceFile,LineNumberTable` + `-renamesourcefileattribute SourceFile` in `app/proguard-rules.pro:17-21`.
  - Close issue `#1` (whose `@SerializedName` and initial ProGuard tasks are already on `main`) and track the R8 refinement separately.

#### SEC-3 — Missing release signing configuration, static `versionCode = 1`, and no `.aab` bundle verification
- **Severity:** **Medium**
- **Evidence:**
  - `app/build.gradle.kts:29-30` hardcodes `versionCode = 1` and `versionName = "1.0"`.
  - `app/build.gradle.kts:40-49` defines `buildTypes.release` with `isMinifyEnabled = true` and `isShrinkResources = true`, but configures **no `signingConfigs`**. Running `./gradlew assembleRelease` (including in `.github/workflows/android-ci.yml:37`) produces an unsigned `app-release-unsigned.apk` that cannot be installed on a device or uploaded to Google Play.
  - `debug` build type (`app/build.gradle.kts:40-49`) does not set `applicationIdSuffix = ".debug"`, so a developer or reviewer cannot install `debug` and `release` builds side by side on the same device.
- **Fix suggestion:**
  - Add a conditional `signingConfigs.create("release")` in `app/build.gradle.kts` that reads `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` from `local.properties` or environment variables when present (falling back cleanly to unsigned in fork PRs).
  - Add `applicationIdSuffix = ".debug"` to `buildTypes.debug`, and allow overriding `versionCode` / `versionName` via Gradle project properties (`-PversionCode=...`) in CI.

#### SEC-4 — Play Store readiness gaps: cleartext traffic allowed on API 26–27, template backup rules, and no `lintRelease` / `bundleRelease` check
- **Severity:** **Low**
- **Evidence:**
  - `app/src/main/AndroidManifest.xml:7-16` does not set `android:usesCleartextTraffic="false"` or `android:networkSecurityConfig`. Because `minSdk = 26` (`app/build.gradle.kts:27`), Android 8.0 and 8.1 (API 26–27) permit cleartext `http://` traffic by default (which affects dynamic crest image URLs loaded via Coil in `ClubCrest.kt:35`, and is even exploited with `http://crests.example.test/...` in `CrestImageLoaderTest.kt:59`).
  - `app/src/main/AndroidManifest.xml:9-11` enables `android:allowBackup="true"` pointing to `app/src/main/res/xml/backup_rules.xml:1-13` and `app/src/main/res/xml/data_extraction_rules.xml:1-19`, both of which contain only commented-out `TODO` template placeholders.
  - `.github/workflows/android-ci.yml:37` runs `lintDebug` and `assembleRelease`, but does not run `bundleRelease` (Play Store's required `.aab` format) or `lintRelease`.
- **Fix suggestion:**
  - Set `android:usesCleartextTraffic="false"` on `<application>` in `app/src/main/AndroidManifest.xml:7` (using a `debug/AndroidManifest.xml` override if `androidTest` needs localhost HTTP).
  - Explicitly include only `settings.xml` (`SharedPreferences`) and exclude cache files in `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`.

---

## 2. Networking & API Limits

### Per-Screen Request Inventory (`https://api.football-data.org/v4/`, free tier: **10 requests/min**)

| Screen / User Action | Entry Point | HTTP Request(s) Triggered | Payload Size / Notes |
|---|---|---|---|
| **App launch (Matches tab, default `"PL"`)** | `MatchesViewModel.init` (`MatchesViewModel.kt:48-50`) → `getCurrentMatchdayResult("PL")` (`FootballRepositoryImpl.kt:22-55`) | **1 request:** `GET v4/competitions/PL/matches?season=2025` | Downloads **all 380 season matches** (~350 KB JSON), extracts ~10 matches for `currentMatchday`, and **discards the other 370 matches**. |
| **Matches: tap another league chip** | `MatchesViewModel.loadLeague(code)` (`MatchesViewModel.kt:53-69`) | **1 request:** `GET v4/competitions/{code}/matches?season=YYYY` | Downloads all 306–380 season matches again; zero in-memory retention when switching back. |
| **Matches: tap the *already selected* league chip** | `MatchesViewModel.loadLeague(code)` (`MatchesViewModel.kt:53-69`) | **1 request (unnecessary):** `GET v4/competitions/{code}/matches?season=YYYY` | **No same-league guard** (unlike `StandingsViewModel.kt:36`). Re-downloads the entire season on every tap. |
| **Matches: tap `‹` or `›` (previous / next matchday)** | `MatchesViewModel.goToPreviousMatchday()` / `goToNextMatchday()` (`MatchesViewModel.kt:75-111`) → `getMatchesByMatchday` (`FootballRepositoryImpl.kt:57-75`) | **1 request per tap:** `GET v4/competitions/{code}/matches?matchday=N&season=YYYY` | **100% redundant:** all 34–38 matchdays were already downloaded in `getCurrentMatchdayResult`. |
| **Standings: open tab (default `"PL"`) or switch league** | `StandingsViewModel.init` / `loadLeague` (`StandingsViewModel.kt:30-39`) → `getStandings(code)` (`FootballRepositoryImpl.kt:77-114`) | **2 sequential requests:**<br>1. `GET v4/competitions/{code}/standings`<br>2. `GET v4/competitions/{code}/matches?season=YYYY` | Request #2 downloads **all 306–380 season matches again** solely to compute 5-match form (`computeForm`, `FootballRepositoryImpl.kt:168-202`), duplicating `MatchesViewModel`'s download. |
| **Standings: switch mode (`Full` / `Short` / `Form`)** | `StandingsViewModel.setViewMode` (`StandingsViewModel.kt:42-44`) | **0 requests** | Re-renders existing `StandingsUiState.Success` in memory. |
| **Match Details: open match card** | `MatchDetailsViewModel.init` (`MatchDetailsViewModel.kt:19-21`) → `getMatchDetail(id)` (`FootballRepositoryImpl.kt:122-144`) | **1 request per visit:** `GET v4/matches/{id}` | **100% redundant data:** `MatchDetailDto` (`MatchDetailDto.kt:5-12`) has the exact same fields as `MatchDto` (`MatchDto.kt:5-13`) already fetched in the match list. Scoped to `NavBackStackEntry` (`AppNavHost.kt:98-105`), so popping and re-opening the same match re-fetches from the network. |
| **Team Details: open team from Standings** | `TeamDetailsViewModel.init` (`TeamDetailsViewModel.kt:29-31`) → `getTeamById(id)` (`FootballRepositoryImpl.kt:146-163`) | **1 request per visit:** `GET v4/teams/{id}` | Scoped to `NavBackStackEntry` (`AppNavHost.kt:108-115`); popping and tapping the same team re-fetches from the network. |

### Findings

#### NET-1 — Normal browsing exhausts the 10 req/min rate limit in ~25 seconds due to redundant full-season downloads (`#17`)
- **Severity:** **High**
- **Evidence:**
  - `FootballRepositoryImpl.kt:24` (`getCurrentMatchdayResult`) and `FootballRepositoryImpl.kt:87` (`getStandings`) both call `api.getMatches(leagueCode, season)` (`GET v4/competitions/{code}/matches?season=YYYY`), downloading all 306–380 fixtures of the season without storing them in memory or on disk.
  - `FootballRepositoryImpl.kt:58` (`getMatchesByMatchday`) makes a separate network call on every `‹` / `›` click even though all matchdays were already returned by `getCurrentMatchdayResult`.
  - A routine user flow — launch app (`1` req) → tap `‹` twice (`+2` reqs) → check 2 other leagues on Matches (`+2` reqs) → switch to Standings (`+2` reqs) → tap 2 other leagues on Standings (`+4` reqs) — issues **11 requests in under 30 seconds**, hitting `HTTP 429 Too Many Requests`.
- **Fix suggestion:**
  - Execute the two-phase caching strategy proposed in PR `#94` (Issue `#17`): first add a `Clock`-aware in-memory season-matches cache in `FootballRepositoryImpl` shared by `getCurrentMatchdayResult`, `getMatchesByMatchday`, and `getStandings` (cutting matchday browsing and Standings form requests to `0` extra network calls), followed by Room persistence after PR `#95` (`#34`) merges.

#### NET-2 — `MatchesViewModel.loadLeague` lacks a same-league guard, re-downloading 380 matches when tapping the active league tab
- **Severity:** **Medium**
- **Evidence:**
  - In `app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsViewModel.kt:35-36`, tapping the already-selected league chip is a no-op when data is loaded:
    ```kotlin
    fun loadLeague(code: String) {
        if (_currentLeagueCode.value == code && _uiState.value is StandingsUiState.Success) return
    ```
  - In `app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesViewModel.kt:53-69`, `loadLeague(leagueCode: String)` has **no such check**:
    ```kotlin
    fun loadLeague(leagueCode: String) {
        lastRequest = { loadLeague(leagueCode) }
        _currentLeagueCode.value = leagueCode
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MatchesUiState.Loading
            ...
    ```
  - Tapping the active `"PL"` chip on `MatchesScreen` (`LeagueSelector.kt:44`) resets the screen to `Loading`, discards the user's current matchday selection, and fires a fresh `GET v4/competitions/PL/matches?season=2025` request.
- **Fix suggestion:**
  - Add `if (_currentLeagueCode.value == leagueCode && _uiState.value is MatchesUiState.Success) return` at the start of `MatchesViewModel.loadLeague` (`MatchesViewModel.kt:54`) and add a unit test mirroring `StandingsViewModelTest.kt:115`.

#### NET-3 — Silent data degradation in `getStandings`: `HTTP 429` on the secondary form request is swallowed and permanently cached in `StandingsViewModel`
- **Severity:** **Medium**
- **Evidence:**
  - In `app/src/main/java/com/artsiom/footballpulse/data/FootballRepositoryImpl.kt:86-94`:
    ```kotlin
    val formByTeamId: Map<Int, String> = try {
        val matchesResponse = api.getMatches(leagueCode, FootballSeason.current(clock))
        if (matchesResponse.isSuccessful) {
            computeForm(matchesResponse.body()?.matches ?: emptyList())
        } else emptyMap()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        emptyMap()
    }
    ```
  - When `api.getStandings(leagueCode)` succeeds (`200 OK`, request #10 in the minute window) and `api.getMatches(...)` immediately hits the rate limit (`429 Too Many Requests`, request #11), `getStandings` silently falls back to `emptyMap()` and emits `StandingsUiState.Success` where every team's `form` is `null` (`StandingsUiMapper.kt:46`: `List(5) { null }`).
  - When the user switches to `ViewMode.FORM` (`StandingsScreen.kt:98`), the entire `Form` column renders as invisible `Color.Transparent` circles (`StandingsTable.kt:117`, `123`) with no explanation and no `Retry` button, and because `_uiState.value is StandingsUiState.Success`, re-tapping the league tab (`StandingsViewModel.kt:36`) will not retry either.
- **Fix suggestion:**
  - Sharing the cached season matches (Issue `#17`) eliminates the second request whenever that league's matches were already loaded. Additionally, in `StandingsTable.kt:94-97` / `StandingForm` (`StandingsTable.kt:107-129`), render a visible neutral dash (`"—"`) when `row.form.all { it == null }` so unavailable form data does not appear as a blank layout glitch.

#### NET-4 — Raw JVM/HTTP exception strings (`"HTTP 403: "`, `"HTTP 429: "`, `UnknownHostException`) are displayed directly to users, and empty API keys still trigger network round-trips (`#12`)
- **Severity:** **High** *(tracked in open Issue `#12`)*
- **Evidence:**
  - `FootballRepositoryImpl.kt:26`, `60`, `80`, `125`, and `148` throw `Exception("HTTP ${response.code()}: ${response.message()}")`. Because `api.football-data.org` serves responses over HTTP/2 (which omits textual reason phrases), `response.message()` is `""`.
  - All 4 ViewModels (`MatchesViewModel.kt:66`, `108`; `StandingsViewModel.kt:61`; `MatchDetailsViewModel.kt:33`; `TeamDetailsViewModel.kt:43`) pass `e.message ?: "Unknown error"` directly into `UiState.Error(message)`, and `ErrorState.kt:39` renders `Text(message)` verbatim:
    - **401 / 403 (missing or invalid `FOOTBALL_API_KEY`):** Even when `BuildConfig.FOOTBALL_API_KEY` is blank, `RetrofitInstance.kt:35` sends `X-Auth-Token: ` over the network and displays **`"HTTP 403: "`** (or **`"HTTP 401: "`**) with no mention of `FOOTBALL_API_KEY`.
    - **429 (rate limit):** Displays **`"HTTP 429: "`**, ignoring both the `X-RequestCounter-Reset` response header and the JSON body (`{"message":"You reached your request limit. Wait N seconds."}`).
    - **5xx (server error):** Displays **`"HTTP 500: "`**.
    - **Offline (`UnknownHostException`) / Timeout (`SocketTimeoutException`):** Displays **`"Unable to resolve host \"api.football-data.org\": No address associated with hostname"`** or **`"timeout"`**.
    - **Null message fallback:** Hardcodes `"Unknown error"` in Kotlin instead of `res/values/strings.xml` (violating `AGENTS.md` §5).
- **Fix suggestion:**
  - Implement open Issue `#12` (`AppError` sealed hierarchy + localized strings in `res/values/strings.xml` + fast-fail on blank `BuildConfig.FOOTBALL_API_KEY`). As an enhancement when implementing `#12`, parse `response.headers()["X-RequestCounter-Reset"]?.toIntOrNull()` on HTTP `429` so `AppError.RateLimited(retryAfterSeconds: Int?)` can tell the user how many seconds remain.

#### NET-5 — `MatchDetailDto` fetches zero additional data beyond `MatchDto`, and `RetrofitInstance` does not share its `OkHttpClient` with Coil
- **Severity:** **Low**
- **Evidence:**
  - Compare `app/src/main/java/com/artsiom/footballpulse/data/remote/MatchDetailDto.kt:5-12` (`id`, `utcDate`, `status`, `homeTeam`, `awayTeam`, `score`) with `MatchDto.kt:5-13` (`id`, `matchday`, `utcDate`, `status`, `homeTeam`, `awayTeam`, `score`): every field displayed on `MatchDetailsScreen` (`MatchDetailsScreen.kt:85-219`) was already downloaded in `getCurrentMatchdayResult` / `getMatchesByMatchday`, yet opening a match card always makes a fresh `GET v4/matches/{id}` call (`FootballRepositoryImpl.kt:123`).
  - `RetrofitInstance.kt:25-40` constructs its own `OkHttpClient`, while `FootballPulseApp.kt:13-21` builds a `Coil` `ImageLoader` without calling `.okHttpClient(...)`, creating two separate connection pools and thread pools.
- **Fix suggestion:**
  - Once in-memory match caching (`#17`) is in place, `getMatchDetail(matchId)` can resolve directly from cached `MatchDto` entries (or use them as an instant fallback on HTTP 429/offline).
  - Provide a base `@Singleton OkHttpClient` in `NetworkModule` (after PR `#95`) so Retrofit and Coil share a single connection pool and dispatcher.

---

## 3. Compose Quality

### Findings

#### CMP-1 — Unstable action holders (`MatchesActions`, `StandingsActions`) and un-remembered lambdas force unnecessary recompositions
- **Severity:** **Medium**
- **Evidence:**
  - `app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesActions.kt:3-9` and `app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsActions.kt:3-8` are plain classes (not `data class`, not annotated with `@Immutable` or `@Stable`).
  - In `app/src/main/java/com/artsiom/footballpulse/ui/shell/AppNavHost.kt:61-67` and `88-93` (both on `main` and in PR `#95`), a new `MatchesActions(...)` and `StandingsActions(...)` instance is allocated on **every recomposition** of the nav destination without `remember`:
    ```kotlin
    actions = MatchesActions(
        onLeagueSelected = viewModel::loadLeague,
        onPreviousMatchday = viewModel::goToPreviousMatchday,
        onNextMatchday = viewModel::goToNextMatchday,
        onRetry = viewModel::retry,
        onMatchClick = { navController.navigate(Routes.matchDetails(it)) }
    )
    ```
  - Because `MatchesActions` uses reference equality (`===`) and is newly allocated whenever `state`, `leagueCode`, or `navigation` emits, `MatchesScreen` (`MatchesScreen.kt:44-50`) and its child `MatchdaySelector(navigation, actions, ...)` (`MatchesScreen.kt:65`, `96`) can **never be skipped** during recomposition—even when `navigation` did not change (e.g., when `state` transitions from `Success` to `Success` with new matches).
- **Fix suggestion:**
  - Annotate `MatchesActions` (`MatchesActions.kt:3`) and `StandingsActions` (`StandingsActions.kt:3`) with `@Immutable` (and/or make them `data class`), and wrap their creation in `remember(viewModel, navController) { ... }` inside `AppNavHost.kt:61` and `88`.

#### CMP-2 — Domain and UI models holding `List` / `Map` properties lack `@Immutable`, preventing structural skip optimization
- **Severity:** **Medium**
- **Evidence:**
  - `app/src/main/java/com/artsiom/footballpulse/domain/model/League.kt:7-14` contains `val zones: Map<Int, LeagueZone>` and `val seasonZoneOverrides: Map<Int, Map<Int, LeagueZone>>`. Because `kotlin.collections.Map` and `List` are read-only interfaces that can be backed by mutable collections, the Compose compiler marks `League`—and therefore `leagues: List<League>` in `MatchesScreen.kt:46`, `StandingsScreen.kt:48`, and `LeagueSelector.kt:26`—as **unstable**.
  - Likewise, `MatchesUiState.Success` (`MatchesUiState.kt:7`), `StandingsUiState.Success` (`StandingsUiState.kt:7-10`), `StandingUiModel` (`StandingsUiMapper.kt:12-24`, holding `val form: List<FormResult?>`), `MatchDetailsUiModel` (`MatchDetailsUiMapper.kt:13-20`, holding `val scores: List<ScoreRowUiModel>`), and `TeamDetailsUiModel` (`TeamDetailsUiMapper.kt:13-19`, holding `infoRows` and `squadGroups`) all hold `List` properties without `@Immutable`.
- **Fix suggestion:**
  - Add `@Immutable` (`androidx.compose.runtime.Immutable`) to `League`, `StandingUiModel`, `MatchDetailsUiModel`, `TeamDetailsUiModel`, `SquadGroupUiModel`, and the `*UiState` types (or use `kotlinx.collections.immutable.ImmutableList`).

#### CMP-3 — `MatchesScreen` `LazyColumn` uses list index (`"date:$index"`) as the key for `DateHeader` items
- **Severity:** **Low**
- **Evidence:**
  - In `app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesScreen.kt:76-81`:
    ```kotlin
    itemsIndexed(state.items, key = { index, item ->
        when (item) {
            is MatchListItem.DateHeader -> "date:$index"
            is MatchListItem.MatchCard -> "match:${item.card.match.id}"
        }
    }, contentType = { _, item -> item is MatchListItem.DateHeader })
    ```
  - Keying `DateHeader` by its positional `index` in `state.items` means that if the number of matches under the first date changes, every subsequent `DateHeader`'s index shifts, invalidating its key. Since `mapMatches` (`MatchesUiMapper.kt:23-26`) groups by distinct local date, `item.dateLabel ?: "unknown"` is already unique per header.
  - In `app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsScreen.kt:77-82`, the `LazyColumn` provides `key = "header"` and `key = { it.standing.teamId }`, but omits `contentType` on both `item` and `items`, so Compose cannot separate composition reuse pools between the table header and team rows.
- **Fix suggestion:**
  - Replace `itemsIndexed` in `MatchesScreen.kt:76` with `items(state.items, key = { item -> when (item) { is MatchListItem.DateHeader -> "date:${item.dateLabel}"; is MatchListItem.MatchCard -> "match:${item.card.match.id}" } })`, and add `contentType` to `StandingsScreen.kt:78-79`.

#### CMP-4 — Missing `remember` on per-screen `Typography.copy(...)`, static list allocations in `@Composable` bodies, and un-reset scroll state on league/matchday switch
- **Severity:** **Low**
- **Evidence:**
  - All four screens (`MatchesScreen.kt:52-56`, `StandingsScreen.kt:54-58`, `MatchDetailsScreen.kt:53-57`, `TeamDetailsScreen.kt:45-49`) execute `MaterialTheme.typography.copy(...)` and `matchTextStyle(...)` on **every recomposition** without `remember`, whereas `matchDetailsAppearance()` (`MatchDetailsAppearance.kt:25`) is properly wrapped in `remember(base)`.
  - `StandingsModeSelector` (`StandingsScreen.kt:97-98`) and `StandingsLegend` (`StandingsScreen.kt:123-127`) allocate new `listOf(...)` instances on every recomposition instead of storing them as file-level private constants.
  - `MatchesScreen.kt:57` (`val listState = rememberLazyListState()`) and `StandingsScreen.kt:60` (`val tableState = rememberLazyListState()`) never reset scroll position to index `0` when the user switches to a different league (`selectedLeagueCode`) or matchday (`navigation.matchday`), leaving the new league's list scrolled mid-way down.
  - `AppShell.kt:47-50` maintains `var selectedTab by rememberSaveable { mutableStateOf(Tab.Matches) }` and updates it inside `LaunchedEffect(currentRoute)`, causing a two-frame lag (initial composition with old `selectedTab`, followed by effect execution and a second recomposition) when popping back between tabs.
- **Fix suggestion:**
  - Hoist the custom `Typography` and `onSurface` color override into `FootballPulseTheme` (`FootballPulseTheme.kt:96-101`), move static `listOf(...)` definitions in `StandingsScreen.kt` to top-level `private val` constants, and derive `selectedTab` synchronously during composition when `currentRoute` matches a `Tab`.

#### CMP-5 — Preview coverage gaps: no `@Preview` for `AppShell`, `LeagueSelector`, or `MatchCard`, and zero `@PreviewFontScale` previews
- **Severity:** **Low**
- **Evidence:**
  - `AppShell.kt`, `LeagueSelector.kt`, and `MatchCard.kt` have no `@Preview` functions.
  - `MatchesScreenPreviews.kt:12`, `19`, `26`, `37` and `StandingsScreenPreviews.kt:12`, `19`, `26`, `33`, `40` call `FootballPulseTheme { ... }` without explicit `darkTheme` or `uiMode = Configuration.UI_MODE_NIGHT_NO` on the light previews (unlike `TeamDetailsScreenPreviews.kt:15`, `21`, which explicitly passes `darkTheme = false` / `true`).
  - No preview in the codebase uses `@PreviewFontScale` (`fontScale = 2.0f`), which would immediately surface the 200% font-scaling clipping defects documented in §4 (`ACC-5`).
- **Fix suggestion:**
  - Add light/dark previews for `AppShell`, `LeagueSelector`, and `MatchCard`, pass `darkTheme` explicitly in all preview helpers, and add `@PreviewFontScale` (`2.0f`) previews on `StandingsScreen` and `MatchesScreen`.

---

## 4. Accessibility

### Findings

#### ACC-1 — Multiple interactive controls violate the `48.dp × 48.dp` minimum touch target size and lack semantic `Role`s
- **Severity:** **High**
- **Evidence:**
  - Because custom `BasicText` / `Box` with `.clickable` or `.selectable` are used instead of Material 3 interactive components (which enforce `LocalMinimumInteractiveComponentSize` of `48.dp`), four controls fall below the `48.dp` minimum touch target required by WCAG 2.5.5 and Android Accessibility Scanner:
    1. **Standings mode tabs (`app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsScreen.kt:100-108`):** Each tab (`Full`, `Short`, `Form`) hardcodes `Modifier.height(32.dp).background(...).selectable(...)`, resulting in a **`32.dp`** vertical touch target (`16.dp` below minimum).
    2. **League selector chips (`app/src/main/java/com/artsiom/footballpulse/ui/common/LeagueSelector.kt:37-45`):** `BasicText` with `fontSize = 13.sp` and `.padding(horizontal = 16.dp, vertical = 8.dp)` produces a touch target height of **`~34.dp`** (`14.dp` below minimum) and a width of **`~46.dp`** for 2-letter league codes (`"PL"`, `"BL"`, `"L1"`, `"SA"`).
    3. **Standings table rows (`app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsTable.kt:41`, `53`):** Each clickable team row sets `Modifier.fillMaxWidth().height(44.dp)` (**`44.dp`** < `48.dp`).
    4. **Details screen `← Back` buttons (`app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsScreen.kt:74-78` and `app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsScreen.kt:64-68`):** Both use `BasicText(..., modifier = Modifier.clickable(onClick = onBack).padding(16.dp))` **without `role = Role.Button`**, so TalkBack does not announce them as buttons.
- **Fix suggestion:**
  - Add `Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)` (applying the visual pill `.padding(...)` and `.background(...)` *inside* the `48.dp` clickable/selectable bounds) to `LeagueSelector.kt:39-45` and `StandingsScreen.kt:100-108`.
  - Increase `StandingCells` row height in `StandingsTable.kt:53` from `height(44.dp)` to `heightIn(min = 48.dp)`.
  - Pass `role = Role.Button` to `.clickable(role = Role.Button, onClick = onBack)` in `MatchDetailsScreen.kt:76` and `TeamDetailsScreen.kt:66`.

#### ACC-2 — Severe WCAG AA contrast failures across zone indicators, form/status badges, and `#9E9E9E` secondary/header text in Light & Dark themes
- **Severity:** **High**
- **Evidence:**
  - Exact WCAG 2.1 contrast ratios calculated from `app/src/main/res/values/colors.xml:1-44` and `app/src/main/res/values-night/colors.xml:1-25` (where WCAG 1.4.3 requires **≥ `4.5:1`** for normal text `< 18.sp` and WCAG 1.4.11 requires **≥ `3.0:1`** for non-text graphical indicators):

  | Element & Location | Foreground | Background | Actual Contrast | WCAG Minimum | Status |
  |---|---|---|---|---|---|
  | **UEL zone bar & legend dot (Light)** (`StandingsScreen.kt:147`, `colors.xml:37`) | `#FF9800` | `#F5F5F5` / `#FFFFFF` | **`1.98:1` / `2.16:1`** | `3.0:1` (non-text) | **FAIL** |
  | **Playoff zone bar & legend dot (Light)** (`StandingsScreen.kt:150`, `colors.xml:42`) | `#9E9E9E` | `#F5F5F5` / `#FFFFFF` | **`2.46:1` / `2.68:1`** | `3.0:1` (non-text) | **FAIL** |
  | **UECL zone bar & legend dot (Light)** (`StandingsScreen.kt:148`, `colors.xml:41`) | `#4CAF50` | `#F5F5F5` / `#FFFFFF` | **`2.55:1` / `2.78:1`** | `3.0:1` (non-text) | **FAIL** |
  | **UCL zone bar on even row (Dark)** (`StandingsScreen.kt:146`, `values-night/colors.xml:23`) | `#1565C0` | `#1E1E1E` | **`2.90:1`** | `3.0:1` (non-text) | **FAIL** |
  | **Standings table column headers (Light, `11–12.sp`)** (`StandingsTable.kt:47`, `51`, `colors.xml:30`, `35`) | `#9E9E9E` | `#EEEEEE` | **`2.31:1`** | `4.5:1` (text) | **FAIL** |
  | **Matches date headers (Light, `12.sp` Bold)** (`MatchesScreen.kt:130`, `colors.xml:30`, `7`) | `#9E9E9E` | `#F5F5F5` | **`2.46:1`** | `4.5:1` (text) | **FAIL** |
  | **Score / Club Info / Squad labels & ages (Light, `12–13.sp`)** (`MatchDetailsScreen.kt:211`, `TeamDetailsScreen.kt:157`, `167`, `colors.xml:12`) | `#9E9E9E` | `#FFFFFF` | **`2.68:1`** | `4.5:1` (text) | **FAIL** |
  | **Form `D` badge & `LIVE`/`W` badge text (`10–11.sp` Bold)** (`StandingsTable.kt:114-126`, `MatchCard.kt:69-78`) | `#FFFFFF` | `#9E9E9E` / `#4CAF50` | **`2.68:1` / `2.78:1`** | `4.5:1` (text) | **FAIL** |
  | **Status `SCHEDULED` / `FINISHED` / `L` badge text (`10–11.sp` Bold)** (`MatchCard.kt:68-78`, `StandingsTable.kt:116`) | `#FFFFFF` / `#757575` | `#2196F3` / `#E0E0E0` / `#F44336` | **`3.12:1`–`3.68:1`** | `4.5:1` (text) | **FAIL** |

  - Note also that `res/values-night/colors.xml` defines **no dark-theme overrides** for `color_standings_cl_border`, `color_standings_el_border`, `color_standings_rel_border`, or the status/form badge colors, and open Issue `#96` tracks the fact that `colorResource(...)` ignores the in-app `darkTheme` toggle.
- **Fix suggestion:**
  - Darken light-theme `color_text_hint` and `color_date_header_text` from `#9E9E9E` to at least `#616161` (`5.07:1` on `#EEEEEE`, `5.92:1` on `#FFFFFF`).
  - Darken light-theme zone/badge fills (`#2E7D32` for Win/LIVE/UECL = `5.13:1` with white text; `#E65100` or `#EF6C00` for UEL; `#616161` for Draw/Playoff = `5.92:1` with white text; `#C62828` for Loss/Relegation = `5.62:1`; `#1565C0` for Scheduled = `5.75:1`; `#424242` for Finished text on `#E0E0E0` = `7.65:1`), and lighten dark-theme `color_standings_cl_border` to `#42A5F5` (`6.54:1` on `#1E1E1E`).

#### ACC-3 — TalkBack reading order on Standings and Match Details is unlabelled, fragmented, and omits qualification zones completely
- **Severity:** **High**
- **Evidence:**
  - **Standings rows (`app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsTable.kt:40-42`, `52-99`):**
    - Because `Modifier.clickable` is attached to `StandingCells` (`StandingsTable.kt:41`), Compose merges all child `BasicText` nodes in the row into a single accessibility announcement containing **bare numbers with no column labels**: e.g., in `FULL` mode, TalkBack reads **`"1, Liverpool, 10, 6, 2, 2, 18:8, 20"`**; in `FORM` mode (`StandingsTable.kt:111-129`), it reads **`"1, Liverpool, W, W, D, L, W, 20"`**.
    - The qualification/relegation zone (`row.zone`) is rendered solely as a `4.dp` colored `Spacer` (`StandingsTable.kt:56-57`) with **zero semantics**, and `StandingsLegend` (`StandingsScreen.kt:128-136`) uses colored `"● "` characters in an `AnnotatedString`, which TalkBack reads as `"Black circle UCL  Black circle UEL ..."`—making league zones 100% inaccessible to screen-reader and color-blind users.
  - **Match Details screen (`app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsScreen.kt:96-125`, `207-219`):**
    - `MatchHeader` (`MatchDetailsScreen.kt:96-125`) does not merge semantics, forcing 5 separate swipes (`"Arsenal"` → `"2 - 1"` → `"Chelsea"` → `"10 August 2025"` → raw enum `"FINISHED"` or `"IN_PLAY"`).
    - `MatchScoreRow` (`MatchDetailsScreen.kt:207-219`) is an unmerged `Row` of 4 `BasicText` nodes (`label`, `home`, `"  -  "`, `away`), so TalkBack users must swipe 4 times per score row and hear `"First Half"`, `"1"`, `"hyphen"`, `"0"` as disconnected items.
- **Fix suggestion:**
  - On `StandingsTableRow` (`StandingsTable.kt:40-42`), provide a structured `semantics(mergeDescendants = true) { contentDescription = ... }` that includes rank, team name, points, mode-specific stats with labels (e.g., `"10 played, 6 won, 2 drawn, 2 lost, goals 18 to 8"` or `"Form: Win, Win, Draw, Loss, Win"`), and the localized `LeagueZone` name when `row.zone != LeagueZone.NONE`.
  - Add `Modifier.semantics(mergeDescendants = true) {}` to `MatchScoreRow` (`MatchDetailsScreen.kt:208`), `TeamInfoRow` (`TeamDetailsScreen.kt:151`), and `TeamPlayerRow` (`TeamDetailsScreen.kt:168`).

#### ACC-4 — Missing/static content descriptions on `LoadingState`, `ThemeToggleButton`, hero `ClubCrest`, and raw API status strings
- **Severity:** **Medium**
- **Evidence:**
  - `LoadingState.kt:18-20` renders `CircularProgressIndicator()` with no `contentDescription` or `liveRegion` semantics.
  - `AppShell.kt:85-91` (`ThemeToggleButton`) uses a static `stringResource(R.string.toggle_theme)` (`"Toggle theme"`) regardless of whether `darkTheme` is `true` or `false`, instead of announcing the state or target mode.
  - `TeamDetailsScreen.kt:114-120` passes `contentDescription = null` for the primary `80.dp` club crest in the header card (`MatchCard.kt:115`, `MatchDetailsScreen.kt:141, 160`, and `StandingsTable.kt:61` also pass `null`, which is appropriate there because the team name text is immediately adjacent, making `ClubCrest`'s `contentDescription` parameter in `ClubCrest.kt:28` unused across all production callers).
  - `MatchCard.kt:76` and `MatchDetailsScreen.kt:120` display the raw API enum string `match.status` (`"IN_PLAY"`, `"PAUSED"`, `"TIMED"`, `"SCHEDULED"`, `"FINISHED"`) directly in the UI and to TalkBack instead of a human-readable localized string (`"In Play"`, `"Half Time"`, `"Scheduled"`, `"Finished"`).
- **Fix suggestion:**
  - Add a localized `contentDescription` to `LoadingState`, make `ThemeToggleButton`'s description state-aware, and map raw API status strings to localized string resources in `MatchCard` and `MatchDetailsScreen`.

#### ACC-5 — Text clipping and column truncation at 200% font scale (`fontScale = 2.0f`)
- **Severity:** **High**
- **Evidence:**
  - Several components place `sp`-scaled text inside containers with rigid `dp` heights or widths:
    1. **`StandingsModeSelector` (`StandingsScreen.kt:93`, `100-116`):** Each mode pill fixes `Modifier.height(32.dp)` while rendering `13.sp` text (`26.sp` at 200% font scale, ~`35.dp` line height with `includeFontPadding = true`), vertically clipping the top and bottom of `"Full"`, `"Short"`, and `"Form"`.
    2. **`MatchdaySelector` (`MatchesScreen.kt:97-122`):** The bar fixes `Modifier.height(52.dp)` and `MatchdayArrow` fixes `Modifier.size(52.dp)` with `28.sp` arrow text (`56.sp` at 200% scale, ~`72.dp` line height), vertically clipping both the `‹` / `›` glyphs and wrapped `"Matchday 38"` text.
    3. **`StandingsTable` (`StandingsTable.kt:53-128`):** Row height is fixed at `height(44.dp)`, and in `ViewMode.FULL` the non-team columns consume `266.dp` of fixed `dp` widths (`StandingCell` widths `28.dp`, `32.dp`, `54.dp`). At 200% font scale (`12.sp` → `24.sp`, `11.sp` → `22.sp`), 2-digit ranks (`"10"`–`"20"` in `28.dp`), `GF:GA` (`"18:12"` in `54.dp`), and 2–3 digit points (`32.dp`) wrap onto two lines and clip inside the `44.dp` row, while the remaining `~86.dp` for `teamName` (`maxLines = 1`) fits only 5–6 characters (`"Arsen…"`). In `ViewMode.FORM` (`StandingsTable.kt:109`, `125-126`), `11.sp` (`22.sp` at 200%) `"W"` / `"D"` labels overflow the fixed `28.dp` circles.
    4. **`AppShell` floating action button overlap (`AppShell.kt:58`, `StandingsScreen.kt:77`, `TeamDetailsScreen.kt:63`):** `ThemeToggleButton` (`SmallFloatingActionButton`) floats at the bottom-end of the `Scaffold` content area, but `StandingsScreen`'s `LazyColumn` has `contentPadding = PaddingValues(0.dp)` and `TeamDetailsScreen` has `padding(bottom = 16.dp)`, so the FAB covers the points column of the 20th team in Standings and the bottom player's age in Team Details (especially at 200% font scale).
- **Fix suggestion:**
  - Replace fixed `height(32.dp)`, `height(44.dp)`, and `height(52.dp)` with `heightIn(min = ...)` + vertical padding so containers grow with `LocalDensity.current.fontScale`.
  - Use font-relative or adaptive column widths (or horizontal scroll in `ViewMode.FULL` when `fontScale > 1.3f`) in `StandingsTable.kt`, and add `bottom = 72.dp` content padding to `StandingsScreen` and `TeamDetailsScreen` to clear the FAB.

---

## 5. Architecture & Code Health

### Layer Boundaries After the Migration
- **Clean boundaries achieved:**
  - `domain` (`app/src/main/java/com/artsiom/footballpulse/domain/`) has zero Android, Retrofit, or Gson imports (`League` uses only Kotlin standard library types and resource IDs are kept in `ui/`).
  - `ui` (`app/src/main/java/com/artsiom/footballpulse/ui/`) has zero imports from `com.artsiom.footballpulse.data.**`; all four ViewModels depend exclusively on the `com.artsiom.footballpulse.domain.repository.FootballRepository` interface.

### Findings

#### ARCH-1 — `Detekt` baseline (`config/detekt/baseline.xml`) has 66 entries, 45 of which (68%) are stale ghosts from deleted XML Fragments and Adapters
- **Severity:** **Medium**
- **Evidence:**
  - `config/detekt/baseline.xml:4-71` contains **66 suppressed `<ID>` entries**.
  - **45 of those 66 entries** reference classes that no longer exist on `main` (or magic numbers already refactored out):
    - `TeamDetailsFragment.kt` (`12` entries, lines `6, 16, 39-47, 59`) — file deleted in `#78`/`#81`.
    - `MatchDetailsFragment.kt` (`9` entries, lines `14-15, 21-27`) — file deleted in `#77`/`#81`.
    - `StandingsAdapter.kt` (`8` entries, lines `5, 34-37, 50-52`) — file deleted in `#79`.
    - `MatchesFragment.kt` (`3` entries, lines `12, 29-30`) — file deleted in `#80`/`#81`.
    - `StandingsFragment.kt` (`2` entries, lines `13, 38`) — file deleted in `#79`/`#81`.
    - `LeaguesAdapter.kt` (`2` entries, lines `20, 49`) & `MatchesAdapter.kt` (`1` entry, line `28`) — files deleted in `#80`.
    - `ExampleUnitTest.kt` (`2` entries, lines `54, 70`) — file deleted in `#15` (plus `2` entries for `ExampleInstrumentedTest.kt` on lines `53, 69`, deleted in PR `#93`).
    - `MatchesViewModel.kt` (`3` `MagicNumber` entries for `2025`, `34`, `38` on lines `31-33`) & `FootballRepositoryImpl.kt` (`1` `MagicNumber` entry for `7` on line `19`) — already extracted to `FootballSeason.kt` and `Leagues.kt`.
  - Only **21 entries** in `config/detekt/baseline.xml` correspond to existing code:
    - `5 × InstanceOfCheckForException` (lines `7-11`) + `5 × TooGenericExceptionCaught` (lines `60-64`) in `FootballRepositoryImpl` and the 4 ViewModels (`catch (e: Exception) { if (e is CancellationException) throw e }`).
    - `3 × TooGenericExceptionThrown` (lines `65-67`) in `FootballRepositoryImpl.kt` (`throw Exception(...)`, to be replaced by `#12`).
    - `1 × LoopWithTooManyJumpStatements` (line `17`) + `1 × MagicNumber: 5` (line `18`) in `FootballRepositoryImpl.kt:179-200`.
    - `4 × NewLineAtEndOfFile` (lines `55-58`) in `FootballApiService.kt:29`, `MatchesUiState.kt:10`, `RetrofitInstance.kt:48`, `Standing.kt:17`.
    - `1 × MaxLineLength` (line `48`) in `CrestImageLoaderTest.kt:39` and `1 × UseCheckOrError` (line `68`) in `FootballRepositoryInjectionTest.kt:73`.
- **Fix suggestion:**
  - Fix the trivial active findings (replace `if (e is CancellationException) throw e` with a dedicated `catch (e: CancellationException) { throw e }` clause before `catch (e: Exception)`, extract `private const val FORM_MATCH_COUNT = 5` and refactor the `for` loop in `computeForm`, add trailing newlines, wrap line 39 of `CrestImageLoaderTest.kt`, and use `error("Offline")` in `FootballRepositoryInjectionTest.kt:73`). Once `#12` replaces `throw Exception(...)`, **`config/detekt/baseline.xml` can be reduced from 66 entries to 0 (or only `TooGenericExceptionCaught` in ViewModels)**!

#### ARCH-2 — Implicit `Clock.systemDefaultZone()` / `ZoneId.systemDefault()` calls and main-thread UI mapping hidden inside `UiState.Success` default parameters
- **Severity:** **Medium**
- **Evidence:**
  - Presentation mappers are invoked as default parameter expressions inside `UiState.Success` constructors:
    - `app/src/main/java/com/artsiom/footballpulse/ui/matches/MatchesUiState.kt:7`: `data class Success(val matches: List<Match>, val items: List<MatchListItem> = mapMatches(matches))`
    - `app/src/main/java/com/artsiom/footballpulse/ui/standings/StandingsUiState.kt:7-10`: `data class Success(val standings: List<Standing>, val rows: List<StandingUiModel> = mapStandings(standings))`
    - `app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsUiState.kt:7-10`: `data class Success(val match: MatchDetail, val content: MatchDetailsUiModel = mapMatchDetails(match))`
    - `app/src/main/java/com/artsiom/footballpulse/ui/teamdetails/TeamDetailsViewModel.kt:14-17`: `data class Success(val team: TeamDetails, val content: TeamDetailsUiModel = mapTeamDetails(team))`
  - This pattern has three architectural drawbacks:
    1. **Hidden ambient clock/timezone state in production ViewModels:** Although `mapMatches`, `mapMatchDetails`, `mapTeamDetails`, and `FootballSeason.current` accept `ZoneId` / `Clock` for tests, none of the 4 ViewModels inject `Clock` or `ZoneId` (`MatchesViewModel.kt:46` calls `FootballSeason.current()` directly; `StandingsUiMapper.kt:29` defaults to `season: Int = FootballSeason.current()`).
    2. **Wrong-league trap in `StandingsUiState.Success`:** Calling `StandingsUiState.Success(standings)` with 1 argument silently maps zones using `Leagues.DEFAULT` (`"PL"`), even if `standings` belong to `"BL1"`.
    3. **Repeated `DateTimeFormatter` compilation on the main thread:** `UiDates` (`app/src/main/java/com/artsiom/footballpulse/ui/presentation/UiDates.kt:10-12`) compiles `DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)` and `DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())` inside instance properties every time `mapMatches` or `mapMatchDetails` is called.
- **Fix suggestion:**
  - Move `dateFormat` in `UiDates.kt:11` to a `companion object` constant (and cache `timeFormat` per locale), inject `Clock` into `MatchesViewModel`, `StandingsViewModel`, and `TeamDetailsViewModel` via Hilt (`#95`), and remove the `= mapXxx(...)` default parameter trap from `StandingsUiState.Success`.

#### ARCH-3 — Cross-feature package coupling (`ui.matches`, `ui.standings`, `ui.teamdetails` all import `ui.matchdetails`) and duplicated theme/mapper code
- **Severity:** **Medium**
- **Evidence:**
  - `app/src/main/java/com/artsiom/footballpulse/ui/matchdetails/MatchDetailsAppearance.kt:1-47` lives in `package com.artsiom.footballpulse.ui.matchdetails`, yet `MatchesScreen.kt:40-41`, `MatchCard.kt:28-29`, `StandingsScreen.kt:42-43`, `StandingsTable.kt:31`, and `TeamDetailsScreen.kt:33-35` all import `MatchDetailsAppearance`, `matchDetailsAppearance`, and `matchTextStyle` from `ui.matchdetails`.
  - All 4 screens (`MatchesScreen.kt:51-58`, `StandingsScreen.kt:53-61`, `MatchDetailsScreen.kt:52-58`, `TeamDetailsScreen.kt:44-50`) duplicate the exact same 8-line `MaterialTheme(colorScheme = appearance.colors, typography = typography)` wrapper.
  - `MatchCard.kt:66-80` and `MatchDetailsScreen.kt:110-124` duplicate the status badge composable; `MatchCard.kt:95-110` and `MatchDetailsScreen.kt:130-167` duplicate the home/away team crest+name layout.
  - `FootballRepositoryImpl.kt:40-52` and `62-74` duplicate the 12-line `MatchDto` → `Match` mapping.
  - `TeamDetailsUiState` is declared inside `TeamDetailsViewModel.kt:12-19` instead of its own `TeamDetailsUiState.kt` file (unlike the other three screens), and `MatchDetailsUiState.kt:5` uses `sealed class` while the others use `sealed interface`.
- **Fix suggestion:**
  - Move `MatchDetailsAppearance` and `matchTextStyle` into `ui/theme/` (merging the typography and color adjustments into `FootballPulseTheme`), extract a shared `MatchStatusBadge` to `ui/common/`, deduplicate `MatchDto.toDomain()` in `FootballRepositoryImpl.kt`, and extract `TeamDetailsUiState.kt`.

#### ARCH-4 — Dead code, redundant `MatchDetail` fields, unused resources/dependencies, and outdated documentation
- **Severity:** **Low**
- **Evidence:**
  - **Duplicate fields in `MatchDetail` (`app/src/main/java/com/artsiom/footballpulse/domain/model/MatchDetail.kt:7-8`, `13-14`):** `homeScore` / `awayScore` and `fullTimeHome` / `fullTimeAway` are populated with the exact same values (`dto.score.fullTime.home` / `away`) in `FootballRepositoryImpl.kt:133-134` and `139-140`.
  - **Unused XML resources:**
    - `R.string.standings_legend` in `app/src/main/res/values/strings.xml:21` is never referenced.
    - `R.color.black`, `R.color.white`, and `R.color.color_background` in `app/src/main/res/values/colors.xml:3-4, 7` and `app/src/main/res/values-night/colors.xml:4` are never referenced anywhere in Kotlin or XML.
  - **Unused dependencies:**
    - `androidx.room:room-runtime:2.6.1` and `androidx.room:room-ktx:2.6.1` (`app/build.gradle.kts:67-68`, pending `#17` decision in PR `#94`).
    - `androidx.test.espresso:espresso-core:3.7.0` (`gradle/libs.versions.toml:7, 31`, `app/build.gradle.kts:87`) has zero imports in `src/androidTest` (both on `main` and in PR `#98`).
  - **Outdated documentation:**
    - `AGENTS.md` §2 & §3 still list `Java target 11`, `UI: XML Views + findViewById, Material 3, ConstraintLayout, RecyclerView`, `Retrofit 2.9`, `Coil 2.6 (without coil-svg)`, `JUnit 4 (no real tests yet)`, and `Fragment + ViewModel + UiState (+ Adapter)`.
    - `README.md:39` and `63` still state `Networking: Retrofit2 + OkHttp` (actual is Retrofit `3.0.0`) and `git clone https://github.com/artkorzh228/FootballPulse.git` (actual repository owner is `karzhaneuski`).
- **Fix suggestion:**
  - Remove `fullTimeHome` / `fullTimeAway` from `MatchDetail`, delete unused resources (`standings_legend`, `black`, `white`, `color_background`) and unused `espresso-core`, and update `AGENTS.md` and `README.md` to reflect the post-Compose, Hilt, Retrofit 3, and AGP 9.5 stack.

---

## 6. Testing Gaps (Post-#15 & Accounting for PR #98)

### What Is Currently Tested
- **JVM Unit Tests (`app/src/test/`, 80 tests across 7 files, run in CI):**
  - `FootballRepositoryTest.kt` (22 tests) + `FootballRepositoryInjectionTest.kt` (4 tests) + `FormCalculationTest.kt` (7 tests): DTO mapping, current matchday pivot selection, `Clock`-based season cutoff, form calculation with postponed matches, HTTP error propagation (`401`, `403`, `404`, `429`, `500`), `204` empty body, and `CancellationException` propagation.
  - `MatchesViewModelTest.kt` (10 tests), `StandingsViewModelTest.kt` (8 tests), `MatchDetailsViewModelTest.kt` (4 tests, +1 in PR `#95`), `TeamDetailsViewModelTest.kt` (4 tests, +1 in PR `#95`): `Loading → Success`, `Loading → Error`, retry recovery, job cancellation on rapid league/matchday switches, and league boundary limits.
  - `PresentationMappersTest.kt` (21 tests): `mapMatches`, `UiDates` DST/offsets, `mapMatchDetails`, `matchStatusStyle`, `Leagues.zoneAt` (18- vs 20-team leagues, PL Conference League rule, `seasonZoneOverrides`), `mapStandings`, and `mapTeamDetails`.
- **Instrumented Tests (`app/src/androidTest/`):**
  - `CrestImageLoaderTest.kt` (1 test): SVG and PNG decoding + disk cache reload.
  - `SystemBarInsetsTest.kt` (1 test): Status bar and bottom navigation bar window inset bounds.
  - **PR #98 (`test/compose-screen-tests`, 23 tests):** Isolated composable screen tests (`MatchesScreenTest`, `StandingsScreenTest`, `MatchDetailsScreenTest`, `TeamDetailsScreenTest`).

### Remaining Testing Gaps

#### TST-1 — `androidTest` sources are neither compiled nor executed in GitHub Actions CI
- **Severity:** **High**
- **Evidence:**
  - `.github/workflows/android-ci.yml:37` runs:
    ```yaml
    run: ./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug detekt --stacktrace
    ```
  - Neither `assembleDebugAndroidTest` nor `connectedDebugAndroidTest` is executed in CI. Even though `detekt` scans `src/androidTest/java` (`app/build.gradle.kts:94`), the Kotlin compiler never compiles `src/androidTest` during CI runs—meaning a signature change in `MatchesScreen` or `AppShell` can break `SystemBarInsetsTest` or PR `#98`'s 23 Compose UI tests without failing the PR check.
- **Fix suggestion:**
  - Add `assembleDebugAndroidTest` to the main Gradle step in `.github/workflows/android-ci.yml:37` immediately (zero extra runner cost, guarantees `androidTest` always compiles), and add a macOS/KVM emulator job (or Robolectric compose test runner) to execute the Compose UI tests in CI.

#### TST-2 — Zero tests for `AppShell` / `AppNavHost` navigation flows, back-stack popping, and double-back guard
- **Severity:** **Medium**
- **Evidence:**
  - PR `#98` tests the four `*Screen` composables in isolation (`createComposeRule()`), and `SystemBarInsetsTest.kt:24-30` only clicks the `Standings` tab to check window inset coordinates.
  - Consequently, `app/src/main/java/com/artsiom/footballpulse/ui/shell/AppNavHost.kt:29-116`, `AppShell.kt:43-102`, and `Routes.kt:7-23` have **no functional tests** covering:
    1. Navigating from `MatchesScreen` → `MatchDetailsScreen` (`match/{matchId}`) and back via `onBack` (`AppNavHost.kt:32-35`).
    2. Rapid double-tap on `← Back` (`if (navController.previousBackStackEntry != null) navController.popBackStack()` at `AppNavHost.kt:34`) to verify the start destination is never popped to a blank screen.
    3. Navigating from `StandingsScreen` → `TeamDetailsScreen` (`team/{teamId}`) and then tapping the `Matches` or `Standings` bottom navigation item (`AppShell.kt:94-101` `navigateToTab` with `popUpTo(graph.startDestinationId) { saveState = true }`), verifying that the details screen is popped and `selectedTab` updates accurately.
- **Fix suggestion:**
  - Add an `AppShellNavigationTest` in `src/androidTest/java/com/artsiom/footballpulse/ui/shell/` using a `FakeFootballRepository` to verify end-to-end tab switching, details navigation, tab-from-details `popUpTo` behavior, and the double-back guard.

#### TST-3 — Zero tests for theme toggle persistence, `MainActivity` recreation, and `colorResource` night-mode alignment (`#96`)
- **Severity:** **Medium**
- **Evidence:**
  - `MainActivity.kt:28-51` and `AppShell.kt:85-92` implement the in-app theme toggle (`SharedPreferences("settings", MODE_PRIVATE)`, `rememberSaveable`, `enableEdgeToEdge`, `FootballPulseTheme(darkTheme)`), which has **0 tests**.
  - Because no test exercised `ThemeToggleButton` or verified theme colors across device-light/app-dark and device-dark/app-light states, Issue `#96` (`colorResource()` ignoring the in-app dark theme override) went undetected until manual emulator verification on Android 16.
- **Fix suggestion:**
  - Alongside fixing Issue `#96`, add a Compose instrumentation test that clicks `ThemeToggleButton`, asserts that both `MaterialTheme.colorScheme` and `colorResource`-backed components update their colors, and verifies persistence across `ActivityScenario.recreate()`.

#### TST-4 — `RetrofitInstance` auth header injection (`X-Auth-Token`) and malformed `utcDate` in `computeForm` are untested
- **Severity:** **Low**
- **Evidence:**
  - `FootballRepositoryTest.kt:35-39` constructs a standalone `Retrofit.Builder().baseUrl(server.url("/"))` instance in `setUp()` without using `RetrofitInstance` (`app/src/main/java/com/artsiom/footballpulse/data/remote/RetrofitInstance.kt:11-48`). As a result, no test verifies that `RetrofitInstance` attaches the `X-Auth-Token` header (`RetrofitInstance.kt:35`) or redacts it in logs (`RetrofitInstance.kt:22`).
  - In `app/src/main/java/com/artsiom/footballpulse/data/FootballRepositoryImpl.kt:173`, `computeForm` calls `Instant.parse(it.utcDate)` **without `try/catch`** (unlike `UiDates.kt:14-18`). If a single finished match in the season has a malformed `utcDate`, `computeForm` throws `DateTimeParseException` and aborts form calculation for all 20 teams in the league. `FormCalculationTest.kt:10-95` only tests valid ISO timestamps.
  - In PR `#98`, `StandingsScreenTest.kt:83-89` (`modeTab_click_reportsMode`) clicks the `Form` tab while in `StandingsUiState.Loading`, but never renders `StandingsScreen` in `ViewMode.FORM` with `StandingsUiState.Success` to assert that the `"W"` / `"D"` / `"L"` badges are displayed.
- **Fix suggestion:**
  - Make `computeForm` resilient to malformed `utcDate` strings (`runCatching { Instant.parse(it.utcDate) }.getOrNull() ?: Instant.MIN`) and add a unit test in `FormCalculationTest.kt`.
  - Allow passing a custom `baseUrl` / `apiKey` to `RetrofitInstance` (or `NetworkModule`) so a MockWebServer test can assert the `X-Auth-Token` request header.
  - Add a `success_formMode_showsFormBadges` test to `StandingsScreenTest.kt`.

---

## Prioritized Backlog of Proposed Follow-Up Issues

> **Note on existing open issues:** Issues **`#12`** (*Typed `AppError` errors and clear messages on all screens*), **`#17`** (*Offline caching / rate-limit protection*, proposal in PR `#94`), and **`#96`** (*Resource colors ignore the in-app dark theme override*) are already open on GitHub and should be executed first alongside merging tonight's open PRs (`#92`, `#93`, `#94`, `#95`, `#97`, `#98`). Issue **`#1`** can be closed as already implemented on `main`.
>
> Below are **14 net-new proposed issues** (ordered by priority) covering the findings of this second audit.

### 1. `[a11y] Fix WCAG AA contrast ratios for zone indicators, badges, and secondary text in Light and Dark themes`
- **Addresses:** `ACC-2` (Severity: **High**), complements `#96`
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] Light-theme `color_text_hint` and `color_date_header_text` in `res/values/colors.xml` achieve ≥ `4.5:1` contrast against `#FFFFFF`, `#F5F5F5`, and `#EEEEEE` (e.g., `#616161`).
  - [ ] Status badge (`LIVE`, `SCHEDULED`, `FINISHED`) and Form badge (`W`, `D`, `L`) text/background pairs achieve ≥ `4.5:1` contrast in both Light and Dark themes.
  - [ ] Standings qualification/relegation zone colors (`UCL`, `UEL`, `UECL`, `RELEGATION`, `PLAYOFF`) achieve ≥ `3.0:1` non-text contrast against both odd/even row backgrounds and the screen background in Light and Dark themes (including a dark-theme override for `color_standings_cl_border` in `res/values-night/colors.xml`).
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 2. `[a11y] Enforce 48dp minimum touch targets and button roles across LeagueSelector, Standings, and Details screens`
- **Addresses:** `ACC-1` (Severity: **High**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] League chips in `LeagueSelector.kt`, mode tabs in `StandingsScreen.kt` (`StandingsModeSelector`), and team rows in `StandingsTable.kt` (`StandingsTableRow`) all have a minimum interactive touch target of `48.dp × 48.dp` while preserving their visual pill/row appearance.
  - [ ] `← Back` in `MatchDetailsScreen.kt` and `TeamDetailsScreen.kt` includes `role = Role.Button` and ≥ `48.dp` touch target dimensions.
  - [ ] Compose UI tests verify that all interactive tabs, chips, rows, and back buttons remain clickable and meet minimum touch bounds.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 3. `[a11y] Fix 200% font scaling clipping and FAB overlap on Standings, Matches, and Team Details screens`
- **Addresses:** `ACC-5`, `CMP-5` (Severity: **High**)
- **Size:** `size:M`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] Fixed heights in `StandingsModeSelector` (`32.dp`), `StandingsTable` (`44.dp`), and `MatchdaySelector` (`52.dp`) are replaced with `heightIn(min = ...)` and adaptive layout so no text clips vertically at `fontScale = 2.0f`.
  - [ ] `StandingsTable` columns (`#`, `MP`, `W`, `D`, `L`, `GF:GA`, `Pts`, and `Form` badges) adapt to `fontScale = 2.0f` without multi-line number wrapping or clipping team names to unreadable stubs.
  - [ ] `StandingsScreen` and `TeamDetailsScreen` include bottom scroll padding (`≥ 72.dp`) so the `ThemeToggleButton` FAB does not obscure the last row's points or player age.
  - [ ] `@PreviewFontScale` (or explicit `fontScale = 2.0f` `@Preview`) is added to `MatchesScreenPreviews.kt` and `StandingsScreenPreviews.kt`.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 4. `[a11y] Add TalkBack merged semantics, zone announcements, and localized match status labels`
- **Addresses:** `ACC-3`, `ACC-4` (Severity: **High**)
- **Size:** `size:M`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `StandingsTableRow` provides a merged accessibility description announcing rank, team name, points, labeled mode statistics (or full form words `"Win"`, `"Draw"`, `"Loss"`), and the team's `LeagueZone` (when not `NONE`).
  - [ ] `MatchScoreRow`, `TeamInfoRow`, and `TeamPlayerRow` use `semantics(mergeDescendants = true)` so TalkBack reads each row as a single coherent statement instead of 4 separate swipes.
  - [ ] Raw API status strings (`"IN_PLAY"`, `"PAUSED"`, `"TIMED"`, `"SCHEDULED"`, `"FINISHED"`) are mapped to localized labels in `res/values/strings.xml`, and `LoadingState` / `ThemeToggleButton` expose descriptive accessibility labels.
  - [ ] Unit/UI tests cover the new status label mapping and semantics.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 5. `[ci] Compile and run androidTest suite in GitHub Actions CI`
- **Addresses:** `TST-1` (Severity: **High**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `.github/workflows/android-ci.yml` includes `assembleDebugAndroidTest` in the primary build step so `src/androidTest` compilation is enforced on every PR.
  - [ ] CI runs the instrumented test suite (`CrestImageLoaderTest`, `SystemBarInsetsTest`, and the Compose screen tests from `#98`) via a GitHub Actions Android emulator step or equivalent verified runner.
  - [ ] Test results from `androidTest` are included in the uploaded `reports` artifact.

### 6. `[matches] Guard MatchesViewModel.loadLeague against redundant reloads and reset list scroll on league/matchday change`
- **Addresses:** `NET-2`, `CMP-4` (Severity: **Medium**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `MatchesViewModel.loadLeague(leagueCode)` returns early without making a network request when `leagueCode == _currentLeagueCode.value && _uiState.value is MatchesUiState.Success` (matching `StandingsViewModel.loadLeague`).
  - [ ] `MatchesViewModelTest` includes a unit test verifying that re-selecting the active league in `Success` state does not trigger a second repository call.
  - [ ] `MatchesScreen` and `StandingsScreen` reset their `LazyListState` scroll position to the top when `selectedLeagueCode` (or `navigation.matchday`) changes.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 7. `[compose] Stabilize MatchesActions, StandingsActions, League, and UiModels with @Immutable and remember`
- **Addresses:** `CMP-1`, `CMP-2`, `CMP-3` (Severity: **Medium**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `MatchesActions` and `StandingsActions` are annotated with `@Immutable` (or converted to `data class`) and remembered in `AppNavHost.kt`.
  - [ ] `League`, `StandingUiModel`, `MatchDetailsUiModel`, `TeamDetailsUiModel`, `SquadGroupUiModel`, and `*UiState` types are annotated with `@Immutable`.
  - [ ] `MatchesScreen` keys `DateHeader` items by `"date:${item.dateLabel}"` instead of list index, and `StandingsScreen` specifies `contentType` on its `LazyColumn` header and row items.
  - [ ] `AppShell` derives `selectedTab` without a two-frame `LaunchedEffect` delay when `currentRoute` is a top-level tab.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 8. `[refactor] Consolidate FootballPulseTheme typography, move MatchDetailsAppearance out of ui.matchdetails, and deduplicate UI/mapper helpers`
- **Addresses:** `ARCH-3`, `ARCH-4`, `CMP-4` (Severity: **Medium**)
- **Size:** `size:M`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `MatchDetailsAppearance` and `matchTextStyle` are moved from `ui.matchdetails` to `ui.theme` (or folded directly into `FootballPulseTheme`), eliminating all `ui.matchdetails` imports from `ui.matches`, `ui.standings`, and `ui.teamdetails`.
  - [ ] The 4× duplicated `MaterialTheme(colorScheme = appearance.colors, typography = typography)` blocks in `MatchesScreen`, `StandingsScreen`, `MatchDetailsScreen`, and `TeamDetailsScreen` are consolidated into `FootballPulseTheme`.
  - [ ] Shared `MatchStatusBadge` composable is extracted to `ui/common/`, `MatchDto.toDomain()` is deduplicated in `FootballRepositoryImpl.kt`, `TeamDetailsUiState` is extracted to `TeamDetailsUiState.kt`, and duplicate `fullTimeHome`/`fullTimeAway` fields are removed from `MatchDetail`.
  - [ ] Unused resources (`R.string.standings_legend`, `R.color.black`, `R.color.white`, `R.color.color_background`) and unused `androidx-espresso-core` dependency are removed.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 9. `[quality] Clean up Detekt baseline and remove stale XML/Fragment/Adapter suppressions`
- **Addresses:** `ARCH-1` (Severity: **Medium**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] All 45 stale entries referencing deleted `*Fragment.kt`, `*Adapter.kt`, and `Example*Test.kt` files are removed from `config/detekt/baseline.xml`.
  - [ ] Active `InstanceOfCheckForException`, `LoopWithTooManyJumpStatements`, `MagicNumber`, `NewLineAtEndOfFile`, `MaxLineLength`, and `UseCheckOrError` findings in `FootballRepositoryImpl`, ViewModels, and tests are resolved in code so `config/detekt/baseline.xml` is reduced to ≤ 5 entries (or 0 after `#12`).
  - [ ] `./gradlew detekt` passes with zero new warnings.

### 10. `[arch] Inject Clock and ZoneId into ViewModels/mappers and cache DateTimeFormatter instances in UiDates`
- **Addresses:** `ARCH-2`, `TST-4` (Severity: **Medium**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `Clock` and `ZoneId` are provided via Hilt (`NetworkModule` / `AppModule`) and injected into `FootballRepositoryImpl`, `MatchesViewModel`, `StandingsViewModel`, `MatchDetailsViewModel`, and `TeamDetailsViewModel` instead of calling `Clock.systemDefaultZone()` / `ZoneId.systemDefault()` inside `UiState.Success` default parameters.
  - [ ] `StandingsUiState.Success` no longer defaults `rows` to `mapStandings(standings)` with `Leagues.DEFAULT`.
  - [ ] `UiDates` caches its `DateTimeFormatter` instances rather than re-compiling patterns on every call, and `computeForm` safely handles malformed `utcDate` strings without aborting the entire league's form calculation.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 11. `[tests] Add Compose instrumentation tests for AppShell navigation, back stack, and theme persistence`
- **Addresses:** `TST-2`, `TST-3`, `TST-4` (Severity: **Medium**)
- **Size:** `size:M`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `AppShellNavigationTest` verifies navigating `Matches → MatchDetails → Back`, `Standings → TeamDetails → bottom tab switch` (`popUpTo` behavior), and rapid double-tap on `← Back` (ensuring the start destination is never popped).
  - [ ] Theme toggle test verifies clicking `ThemeToggleButton` flips the theme, persists `dark_mode` in `SharedPreferences`, and survives `Activity` recreation.
  - [ ] `StandingsScreenTest` includes a test for `ViewMode.FORM` rendering `"W"`, `"D"`, `"L"` badges, and `RetrofitInstance` has a unit test verifying `X-Auth-Token` header attachment.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` and `connectedDebugAndroidTest` pass.

### 12. `[release] Narrow R8 keep rules, preserve stack-trace line numbers, and configure release signing & network security`
- **Addresses:** `SEC-1`, `SEC-2`, `SEC-3`, `SEC-4` (Severity: **Medium**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] `app/proguard-rules.pro` narrows `-keep class com.artsiom.footballpulse.data.remote.** { *; }` so `RetrofitInstance` can be shrunk/obfuscated while DTOs remain protected, adds `TypeToken` keep rules, and enables `-keepattributes SourceFile,LineNumberTable` + `-renamesourcefileattribute SourceFile`.
  - [ ] `app/build.gradle.kts` sanitizes `footballApiKey`, adds `applicationIdSuffix = ".debug"` on `debug`, supports env/property-driven release signing and `versionCode`/`versionName`, and CI verifies `bundleRelease` / `lintRelease`.
  - [ ] `AndroidManifest.xml` sets `android:usesCleartextTraffic="false"` and `res/xml/backup_rules.xml` / `data_extraction_rules.xml` replace template `TODO`s with explicit `settings.xml` rules.
  - [ ] Issue `#1` is closed as superseded/completed.

### 13. `[compose] Add missing @Preview functions for AppShell, LeagueSelector, and MatchCard`
- **Addresses:** `CMP-5` (Severity: **Low**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:codex`
- **Definition of Done (DoD):**
  - [ ] Light and Dark `@Preview` functions are added for `AppShell`, `LeagueSelector`, and `MatchCard` (covering `FINISHED`, `IN_PLAY`, and `SCHEDULED` states).
  - [ ] Existing previews in `MatchesScreenPreviews.kt` and `StandingsScreenPreviews.kt` pass `darkTheme` explicitly to `FootballPulseTheme` to stay consistent with `TeamDetailsScreenPreviews.kt`.
  - [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug detekt` passes.

### 14. `[docs] Update AGENTS.md and README.md to reflect Compose, Hilt, Retrofit 3, and AGP 9.5 architecture`
- **Addresses:** `ARCH-4` (Severity: **Low**)
- **Size:** `size:S`
- **Best-fit agent:** `agent:antigravity`
- **Definition of Done (DoD):**
  - [ ] `AGENTS.md` §2 (Stack), §3 (Architecture), and §5 (Code style) are updated after PRs `#92`, `#95`, and `#97` merge to reflect Jetpack Compose, Navigation Compose, Hilt `2.59.2` + KSP `2.3.13`, AGP `9.5.1`, `compileSdk 37`, `targetSdk 36`, Java `17`, Retrofit `3.0.0`, Coil `2.7.0` with `coil-svg`, and the unit/Compose test suites.
  - [ ] `README.md` Tech Stack, Architecture, and clone URL (`https://github.com/karzhaneuski/FootballPulse.git`) are updated to match the actual codebase.
