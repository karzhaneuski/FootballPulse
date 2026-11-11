# Proposal: Offline Cache and Rate-Limit Protection (#17)

- **Issue:** #17
- **Author:** Antigravity (`agent:antigravity`)
- **Date:** 2026-11-11
- **Status:** Proposed

## Context

FootballPulse fetches all football data from the `football-data.org` v4 REST API on the **free tier**, which enforces a strict rate limit of **10 requests per minute**. Currently, the app has **no caching layer** (neither in-memory nor on disk), and every screen or tab navigation issues fresh HTTP requests through `FootballRepositoryImpl`:

1. **Redundant full-season downloads (`GET v4/competitions/{code}/matches?season=YYYY`):**
   - `getCurrentMatchdayResult(leagueCode)` downloads the **entire season of ~306–380 matches** (~250–400 KB JSON) just to determine the current matchday and return the ~9–10 matches for that matchday, discarding the other ~370 matches.
   - `getMatchesByMatchday(leagueCode, matchday, season)` (`GET v4/competitions/{code}/matches?matchday=N&season=YYYY`) fires a new network call on every `‹` / `›` matchday click, even though the full season's matches were already downloaded in `getCurrentMatchdayResult`.
   - `getStandings(leagueCode)` fires **two** sequential requests every time a league chip is tapped on the Standings screen:
     1. `api.getStandings(leagueCode)` (`GET v4/competitions/{code}/standings`)
     2. `api.getMatches(leagueCode, season)` (`GET v4/competitions/{code}/matches?season=YYYY` — all ~380 matches again) solely to compute each team's last-5 form via `computeForm()`.
2. **Immediate `HTTP 429` under normal navigation:**
   - Opening the app loads Matches for the Premier League (1 request). Clicking through 3 matchdays (3 requests), switching to Standings (2 requests), and tapping 2 other leagues on Standings (4 requests) reaches **10 requests in under 30 seconds**, triggering `HTTP 429 Too Many Requests` and leaving screens in an error state.
   - Launching the app without internet access (or after process death while offline) shows an error state even if the user viewed the exact same league table a minute ago.
3. **Unused Room dependencies (`D-006`):**
   - `androidx.room:room-runtime:2.6.1` and `androidx.room:room-ktx:2.6.1` are declared in `gradle/libs.versions.toml` and `app/build.gradle.kts` (without `room-compiler` or KSP), adding ~150 KB to the APK with zero `@Entity`, `@Dao`, or `@Database` classes in the codebase.
   - Per `D-006`, `androidx.room:*` updates are frozen in `.github/dependabot.yml` pending this architectural decision.
4. **Synergy with `#34` (`D-005`):**
   - Decision `D-005` (Issue `#16` / implementation Issue `#34`) already approved adding the **KSP plugin** (`com.google.devtools.ksp` `2.3.13`) in Phase 2 for Dagger Hilt under AGP 9's built-in Kotlin support. If Room is kept, it shares the exact same KSP plugin setup.

---

## What Is Worth Caching & Staleness (TTL) Rules

Not all endpoints have the same access frequency, payload size, or volatility. Regardless of the storage mechanism chosen, caching should follow these data-specific rules:

| Resource | Current API Call(s) | Cache Granularity | Freshness TTL | Rationale & Staleness Policy |
|---|---|---|---|---|
| **Season Matches** (used by Matches screen *and* Standings form calculation) | `getMatches(leagueCode, season)` & `getMatchesByMatchday(leagueCode, matchday, season)` | Per `(leagueCode, season)` (all ~306–380 matches of the season) | **5 min** if any match in the league is `IN_PLAY` / `PAUSED` or scheduled within ±2 h of `Clock.instant()`; **15 min** otherwise | Caching the full-season match list once per `(leagueCode, season)` allows: (a) instant `‹` / `›` matchday browsing with **0 extra network calls**, and (b) reusing the same cached matches inside `getStandings()` to compute last-5 form, cutting Standings network requests in half. On network failure or `HTTP 429`, serve stale cached matches if available. |
| **Standings Table** | `getStandings(leagueCode)` | Per `leagueCode` (18–20 rows + season year) | **15 min** | League tables only change when a match finishes. Combined with cached season matches for form calculation, switching between already-visited league tabs costs **0 requests** within the 15-minute window. Serve stale cache when offline or rate-limited. |
| **Team Details & Squad** | `getTeamDetails(teamId)` | Per `teamId` (club metadata + squad list) | **24 hours** | Club crest, stadium, coach, Founded year, and squad list are quasi-static (changing primarily during transfer windows). Caching for 24 h prevents repeat requests when tapping teams from Matches, Standings, or Match Details. |
| **Match Details** | `getMatchById(matchId)` | Per `matchId` | **1 min** if `IN_PLAY` / `PAUSED`; **15 min** if `SCHEDULED` / `TIMED`; **24 hours** if `FINISHED` | Finished match scores (`halfTime` / `fullTime`) are immutable. Live matches require short TTL so scores stay reasonably current while still protecting against rapid back-and-forth navigation. |

> **Key architectural insight:** Before even considering disk persistence, unifying `getCurrentMatchdayResult()`, `getMatchesByMatchday()`, and `getStandings()`'s `computeForm()` around a **single cached full-season match list per `(leagueCode, season)`** eliminates the single biggest source of `HTTP 429` errors in the app.

---

## Options Considered

### Option 1: Room Offline-First Database (+ Shared Season-Matches Table)

Keep Room, upgrade it from `2.6.1` to the current stable `2.8.x` (unfreezing `.github/dependabot.yml`), add `androidx.room:room-compiler` via KSP (shared with `#34` Hilt), and implement a local SQLite cache in `data/local/`:

- **Schema (`FootballDatabase`):**
  - `MatchEntity` (`id`, `leagueCode`, `seasonYear`, `matchday`, `utcDate`, `status`, `homeTeamId`, `homeTeamName`, `homeTeamShortName`, `homeTeamCrest`, `awayTeamId`, `awayTeamName`, `awayTeamShortName`, `awayTeamCrest`, `homeScore`, `awayScore`, `halfTimeHome`, `halfTimeAway`) — indexed by `(leagueCode, seasonYear, matchday)`.
  - `StandingEntity` (`leagueCode`, `seasonYear`, `position`, `teamId`, `teamName`, `teamCrest`, `playedGames`, `won`, `draw`, `lost`, `points`, `goalsFor`, `goalsAgainst`, `goalDifference`).
  - `TeamDetailsEntity` + `PlayerEntity` (or JSON-serialized squad column via `@TypeConverter` for simplicity).
  - `CacheMetadataEntity` (`cacheKey: String`, `fetchedAtMillis: Long`) to track TTL per resource (`"matches:PL:2025"`, `"standings:PL"`, `"team:65"`, `"match:12345"`).
- **Read-through / fallback flow in `FootballRepositoryImpl`:**
  1. Check `CacheMetadataDao` for `cacheKey` against `clock.instant().toEpochMilli()`.
  2. If cached data exists and `now - fetchedAtMillis < ttlMillis`, return mapped domain models directly from Room without touching the network.
  3. Otherwise, fetch from `FootballApiService`, upsert into Room within a transaction, update `CacheMetadataEntity`, and return mapped domain models.
  4. If the network call fails (`IOException` or `HTTP 429`) and stale data exists in Room, return the stale cached data (or surface it according to the error policy).
- **Matchday navigation (`getMatchesByMatchday`):**
  - Queries `MatchDao.getMatches(leagueCode, seasonYear, matchday)` directly from the full-season matches stored during `getCurrentMatchdayResult()`. Only hits the network if the season is missing or expired.

**Pros:**
- **Highest portfolio value:** Room + KSP + Coroutines + Hilt is a core requirement in almost every Junior/Mid Android Engineer job description. Room is already mentioned in the project dependencies and audit.
- **True offline support:** Survives process death, app restarts, and airplane mode.
- **Relational querying:** Filtering by `(leagueCode, season, matchday)` or querying a single `matchId` is native SQL without re-parsing a 350 KB JSON blob in memory.
- **KSP alignment:** `#34` already adds KSP `2.3.13` (`D-005`), so Room adds zero extra build-plugin complexity if sequenced after `#34`.

**Cons:**
- **Highest implementation scope (`size:L`, or two `size:M` tasks):** Requires ~6–8 new files in `data/local/` (`FootballDatabase`, DAOs, Entities, Entity↔Domain mappers), `Context` injection into `AppContainer` (or Hilt `@Provides`), and DAO/repository unit tests.
- **Schema maintenance:** Requires `fallbackToDestructiveMigration(dropAllTables = true)` (appropriate for a pure cache database) so schema tweaks do not crash upgrades.

---

### Option 2: Jetpack DataStore / JSON File Cache (+ In-Memory Cache)

Remove Room and persist raw DTO JSON (via Gson) or serialized domain models in `context.cacheDir` files (or Jetpack DataStore) keyed by endpoint (`matches_PL_2025.json`, `standings_PL.json`, `team_65.json`), paired with an in-memory `ConcurrentHashMap` cache in `FootballRepositoryImpl`.

**Pros:**
- No SQLite schema, Entities, or KSP annotation processing needed.
- Survives app restarts and process death on disk.

**Cons:**
- **Poor fit for structured football data:** Loading a single matchday or computing form requires deserializing the entire 350 KB `matches_PL_2025.json` file into memory unless also cached in RAM.
- **Manual concurrency and eviction:** Requires manual `Mutex` file locking, corrupt-file recovery, and cache cleanup.
- **Low portfolio value:** Custom JSON file caching looks like a workaround compared to Room on an Android resume, while DataStore is designed for small key-value preferences rather than hundreds of match records.
- Requires removing unused Room dependencies (`libs.versions.toml`, `app/build.gradle.kts`, `.github/dependabot.yml`).

---

### Option 3: OkHttp HTTP Disk Cache + In-Memory Repository Cache (Remove Room)

Remove Room and combine two lightweight mechanisms:
1. **In-memory repository cache (`FootballRepositoryImpl`):** Cache the parsed season matches (`Map<Pair<String, Int>, TimedCacheEntry<List<MatchDto>>>`), standings, and team details in memory using `Clock` and a `Mutex`. `getCurrentMatchdayResult()`, `getMatchesByMatchday()`, and `getStandings()` all share the cached season `List<MatchDto>`, eliminating duplicate `getMatches()` calls and `‹` / `›` matchday network requests during a session.
2. **OkHttp `Cache` (`10 MB` in `context.cacheDir`):** Add response and offline cache interceptors to `OkHttpClient` in `RetrofitInstance` / `AppContainer` to rewrite/override `Cache-Control` headers (`max-age=900` online, `max-stale=86400` when offline).

**Pros:**
- **Small scope (`size:S` / `size:M`):** No new dependencies, no KSP required, no Entities or DAOs.
- Immediately solves both the `HTTP 429` rate-limit bottleneck (via in-memory season-match sharing) and basic cold-start / offline survival (via OkHttp disk cache).
- Cleans up unused Room dependencies (`~150 KB` smaller APK) and removes the Dependabot Room freeze.

**Cons:**
- **URL-keyed HTTP cache limitations:** OkHttp caches by exact URL. Without the in-memory repository cache, `GET matches?season=2025` and `GET matches?matchday=10&season=2025` are distinct cache entries. Even with the in-memory layer, cold restarts must re-parse the raw 350 KB JSON response.
- **Brittle header rewriting:** `football-data.org` does not send offline-friendly `Cache-Control` headers by default; forcing caching via OkHttp network interceptors is a known sharp edge (especially around error responses or Dynamic TTLs like live vs. finished matches).
- **Lower portfolio signal than Room:** Does not showcase local database architecture (`@Dao`, `@Entity`, offline-first repository testing).

---

### Option 4: In-Memory Repository Cache Only / No Persistent Cache (Remove Room)

Remove Room and add **only** an in-memory `Clock`-aware cache inside `FootballRepositoryImpl` (sharing season matches across `getCurrentMatchdayResult`, `getMatchesByMatchday`, and `getStandings`, plus caching standings and team details in memory for the session), with no disk persistence.

**Pros:**
- **Smallest possible implementation (`size:S`, 1–2 files):** Only touches `FootballRepositoryImpl.kt` and `FootballRepositoryImplTest.kt` (plus removing Room from Gradle and Dependabot).
- Uses the `java.time.Clock` already injected into `FootballRepositoryImpl` (`#72`) for 100% deterministic unit testing in `FootballRepositoryImplTest`.
- Eliminates ~80% of redundant network calls during an active session (matchday browsing and Standings form calculation reuse the in-memory season matches).

**Cons:**
- **No offline support across app restarts:** Cold starts and process death always require network requests.
- **No Room in the portfolio:** Drops a flagship Android Jetpack component that was originally planned for the project.

---

## Comparison Summary

| Criterion | Option 1: Room Offline-First | Option 2: JSON File / DataStore | Option 3: OkHttp Cache + In-Memory | Option 4: In-Memory Only |
|---|---|---|---|---|
| **Eliminates `HTTP 429` in session** | Yes (shared season matches in DB) | Yes | Yes (shared season matches in RAM) | Yes (shared season matches in RAM) |
| **Offline across app restart** | Yes (SQLite) | Yes (files) | Partial (HTTP URL cache) | No |
| **Dynamic TTL (live vs. finished)** | Easy (`CacheMetadataEntity` + `Clock`) | Medium | Hard in OkHttp headers; Easy in RAM | Easy in RAM (`Clock`) |
| **Implementation size** | `size:L` (or 2× `size:M`) | `size:M` | `size:M` | `size:S` |
| **Build / KSP impact** | Uses KSP (already approved in `D-005` / `#34`) | None (remove Room) | None (remove Room) | None (remove Room) |
| **Junior portfolio value** | **High** (Room + KSP + Coroutines) | Low | Medium | Low |
| **Fate of declared Room deps** | Keep, upgrade to `2.8.x`, unfreeze Dependabot | Remove from Gradle & Dependabot | Remove from Gradle & Dependabot | Remove from Gradle & Dependabot |

---

## What Happens If Room Is Rejected (Options 2, 3, or 4)

If the owner chooses **Option 2, Option 3, or Option 4** (rejecting Room), the implementation issue(s) **must include removing Room completely**:
1. `gradle/libs.versions.toml`: Remove `room = "2.6.1"`, `androidx-room-runtime`, and `androidx-room-ktx`.
2. `app/build.gradle.kts`: Remove `implementation(libs.androidx.room.runtime)` and `implementation(libs.androidx.room.ktx)`.
3. `.github/dependabot.yml`: Remove the `ignore` rule for `androidx.room:*` (added in `D-006`).
4. `AGENTS.md`: Update §2 Stack table to remove Room from "Declared but unused".

---

## Recommendation

**Recommended: A two-phase approach combining Option 4 (immediate `size:S` in-memory season-matches cache) and Option 1 (Room persistence in `size:M` after `#34` Hilt/KSP).**

Specifically:

1. **Phase 1 (`size:S`, immediate, no new dependencies): Shared in-memory cache in `FootballRepositoryImpl`**
   - Cache the full-season match list per `(leagueCode, season)`, standings per `leagueCode`, team details per `teamId`, and match details per `matchId` in `FootballRepositoryImpl` with TTLs validated against the already-injected `java.time.Clock` (5 min for live/today matches, 15 min for standings and non-live matches, 24 h for team details and finished matches).
   - Route `getCurrentMatchdayResult()`, `getMatchesByMatchday()`, and `getStandings()`'s form calculation through the same cached season match list so `‹` / `›` matchday navigation and Standings form calculation make **zero redundant network calls**.
   - Serve stale in-memory entries on `IOException` or `HTTP 429`.

2. **Phase 2 (`size:M`, sequenced after `#34` Hilt + KSP): Room offline-first persistence**
   - Once `#34` adds KSP (`2.3.13`) and Hilt (`D-005`), upgrade Room to `2.8.x`, unfreeze `androidx.room:*` in `.github/dependabot.yml`, add `androidx.room:room-compiler` via `ksp`, and persist `MatchEntity`, `StandingEntity`, `TeamDetailsEntity`, and `CacheMetadataEntity` in `FootballDatabase` (`fallbackToDestructiveMigration(dropAllTables = true)`).
   - This turns the Phase 1 repository cache into a persistent disk cache that survives process death and offline launches while delivering strong portfolio value (Room + Hilt + KSP + Coroutines + Compose).

> **Alternative if keeping scope minimal is preferred:** If the owner prefers not to maintain a SQLite layer in this project, choose **Option 4 (In-memory repository cache only + remove Room)** (or **Option 3** adding a 10 MB OkHttp cache) as a single `size:S`/`size:M` task and delete the unused Room dependencies immediately.

---

## Proposed Entry for `docs/DECISIONS.md`

Once approved, record the following in `docs/DECISIONS.md`:

```markdown
## D-008: Two-phase caching strategy — shared season-matches cache + Room offline persistence

- **Date:** 2026-11-11
- **Status:** Accepted
- **Issue / PR:** #17
- **Decided by:** owner

**Context.** `football-data.org` free tier allows only 10 requests/min. `FootballRepositoryImpl` currently downloads all ~380 season matches in `getCurrentMatchdayResult` and again in `getStandings` (for form), plus 1 request per `getMatchesByMatchday` click, causing rapid `HTTP 429` errors. Meanwhile, `room-runtime` and `room-ktx` (2.6.1) are declared in Gradle and frozen in Dependabot (`D-006`) but unused. See architectural proposal: [docs/proposals/17-offline-cache.md](proposals/17-offline-cache.md).

**Options.**
1. Room offline-first database + shared season-matches query — highest portfolio value and true offline support across restarts / largest scope (`size:L` or 2 × `size:M`), requires KSP.
2. DataStore / JSON file cache (remove Room) — no SQLite schema or KSP / poor fit for relational queries, manual file locking, low portfolio value.
3. OkHttp HTTP disk cache + in-memory repository cache (remove Room) — small scope, no KSP / URL-keyed HTTP cache requires brittle header rewriting and re-parsing 350 KB JSON on cold starts.
4. In-memory repository cache only (remove Room) — smallest scope (`size:S`), fixes in-session `HTTP 429` immediately / no offline support across restarts, drops Room from portfolio.

**Decision.** Two-phase caching strategy combining Option 4 (immediate in-memory season-matches cache) and Option 1 (Room offline persistence after `#34` Hilt/KSP):
- **Step 1 (`size:S`, before KSP):** Add a `Clock`-based in-memory cache in `FootballRepositoryImpl` that shares the full-season match list across `getCurrentMatchdayResult`, `getMatchesByMatchday`, and `getStandings` (form computation), with TTLs of 5 min (live/today matches), 15 min (standings and non-live matches), and 24 h (team details and finished matches), serving stale data on network/rate-limit failure.
- **Step 2 (`size:M`, after `#34` adds KSP):** Upgrade Room to 2.8.x, unfreeze `androidx.room:*` in `.github/dependabot.yml`, wire `room-compiler` via KSP, and persist matches, standings, team details, and cache timestamps in `FootballDatabase` (`fallbackToDestructiveMigration(dropAllTables = true)`).

**Why.**
- Unifying `getCurrentMatchdayResult`, `getMatchesByMatchday`, and `getStandings` around a single cached season match list eliminates ~80% of redundant API calls immediately in Step 1 with zero new dependencies.
- Using the already-injected `java.time.Clock` (`#72`) makes TTL expiration deterministic in unit tests.
- Sequencing Room in Step 2 after `#34` reuses the KSP 2.3.13 toolchain setup (`D-005`) and delivers a complete offline-first Room + Hilt + Coroutines + Compose architecture for the portfolio.

**Consequences.**
- Implementation is split into two issues (`agent:codex`): Step 1 (`size:S`, in-memory cache in `FootballRepositoryImpl`, unblocked now) and Step 2 (`size:M`, Room persistence in `data/local/`, blocked by `#34`).
- Dependabot ignore rule for `androidx.room:*` is removed in Step 2 (or Room is removed from `gradle/libs.versions.toml`, `app/build.gradle.kts`, `.github/dependabot.yml`, and `AGENTS.md` if the owner selects Option 3 or Option 4 instead).
```
