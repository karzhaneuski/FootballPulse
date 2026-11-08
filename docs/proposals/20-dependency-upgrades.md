# Proposal #20: Dependency upgrades and Java target 11 → 17

- **Issue:** #20
- **Date:** 2026-11-08
- **Author:** Claude Code (Opus 5.5)
- **Status:** Proposed (awaiting owner decision)

## Context

The libraries are 1–3 years behind (`docs/AUDIT.md` §1.2, §3.1 item 3), and `compileOptions` targets Java 11.
D-005 makes this issue a blocker for DI step 2 (#34), which needs **Hilt ≥ 2.59.2 and KSP ≥ 2.3.6** on AGP 9.
AGP (9.1.0), Gradle (9.3.1) and the SDK levels are out of scope (issue #20, `AGENTS.md` §7).

Constraints checked against the code and official sources (Maven Central, Google Maven,
release notes) on 2026-11-08:

| Constraint | Source | Effect |
|---|---|---|
| `compileSdk = 35` | `app/build.gradle.kts` | AAR metadata: `core` ≥ 1.17.0 needs `minCompileSdk=36` (1.19.x needs 37 and AGP 9.1); `activity` ≥ 1.11.0 needs 36. **Upper limits: core-ktx 1.16.0, activity 1.10.1.** |
| Kotlin is built into AGP 9.1.0 | AGP 9.1.0 POM: `kotlin-gradle-plugin` **2.2.10** | Libraries built with Kotlin ≤ 2.3 are fine (the compiler reads metadata one minor version ahead). Hilt 2.60+ moves to Kotlin 2.3.21 (flagged "potential breaking" in its release notes) and runs at that edge. |
| Hilt on AGP 9 | Dagger 2.59 release notes: "adds AGP 9 support for the Hilt Gradle plugin"; 2.59.2 fixes slow incremental builds (HiltSyncTask) | Confirms D-005: **2.59.2** is the minimum. The unused `hilt 2.50` plugin in the root `build.gradle.kts` stays until #34. |
| KSP on AGP 9 | KSP 2.3.6 release notes: "Improved the detection of built-in Kotlin" | Confirms D-005: **≥ 2.3.6**. KSP 2.3.x is versioned independently of Kotlin; latest is 2.3.13. |
| Retrofit 2.9 → 3.0 | Retrofit CHANGELOG 3.0.0: only change is OkHttp 3.14 → 4.12; "3.x maintain forward binary-compatibility with 2.x" | We already resolve OkHttp 4.12 through `logging-interceptor`, so the runtime change is close to none. Retrofit ≥ 2.11 ships its own R8 rules (helps #1). |
| Pending decisions | #17 (Room), #19 (Compose vs Views) | Room 2.6.1 is unused and its fate depends on #17, so **don't upgrade it**. Views libraries are needed until at least the #19 decision, and their upgrades are cheap. |

## Options Considered

### Option 1: Everything in one PR
- **Pros:** fastest; a single CI run.
- **Cons:** a regression (R8, lint, resolution conflict) cannot be traced to a group; it mixes the Java target change with library changes; it is hard to review.
- **Impact:** every version in `libs.versions.toml` at once.

### Option 2: Grouped PRs in a fixed order, then Dependabot (recommended)
- **Pros:** each PR is small (`size:S`) with one failure cause; the order unblocks #34 early; the groups match the issue's own split.
- **Cons:** 5 small PRs instead of one; libraries get upgraded in more than one pass.
- **Impact:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, new `.github/dependabot.yml`.

### Option 3: Dependabot/Renovate only
- **Pros:** no manual version picking; ongoing freshness.
- **Cons:** with `compileSdk = 35` it **will propose versions that break the build** (core 1.19, activity 1.13) unless we add ignore rules; no control over order; Java 17 is not a dependency bump at all.
- **Impact:** `.github/dependabot.yml` only.

## Recommendation

**Option 2.** Do #9 first (move all dependencies to the catalog, same versions), then the PRs below in order.
Target versions are the newest stable ones that fit `compileSdk = 35` and the AGP 9.1.0 built-in Kotlin 2.2.10:

| # | PR (group) | Change | Main risk |
|---|---|---|---|
| a | Java target | `compileOptions` 11 → 17 (CI already runs JDK 21) | Kotlin/Java JVM-target mismatch; check that the built-in Kotlin follows `compileOptions` |
| b | Network | retrofit + converter-gson 2.9.0 → **3.0.0**; logging-interceptor stays **4.12.0** | Low. OkHttp 5.x is a separate step, not needed now |
| c | Coroutines + Lifecycle | coroutines 1.7.3 → **1.11.0**; lifecycle 2.7.0 → **2.11.0** | Low. Verify `repeatOnLifecycle` and `viewModelScope` behavior in the smoke test |
| d | AndroidX UI | core-ktx 1.10.1 → **1.16.0** (cap); activity 1.8.0 → **1.10.1** (cap); appcompat 1.6.1 → **1.7.1**; material 1.10.0 → **1.13.0**; constraintlayout 2.1.4 → **2.2.1** | New lint warnings/deprecations; Material theming tweaks |
| e | Coil | 2.6.0 → **2.7.0** (stay on 2.x) | Low. Coil 3 is a migration (new package, network module), so leave it out |
| — | Test libs | androidx junit 1.1.5 → 1.3.0, espresso 3.5.1 → 3.7.0 (with d) | None at runtime |
| — | Not touched | Room (waits for #17), Hilt plugin + KSP (part of #34), AGP/Gradle/SDK | — |
| f | Dependabot | `.github/dependabot.yml` for `gradle` + `github-actions`, weekly, grouped; **ignore** `androidx.core:*` ≥ 1.17 and `androidx.activity:*` ≥ 1.11 while `compileSdk = 35` | Noise from PRs. Ignore rules must be removed when the SDK is raised |

For **#34**, pin **Hilt 2.59.2** and **KSP 2.3.13** (the newest 2.3.x). Hilt 2.60.x requires Kotlin 2.3.21,
which is one minor version ahead of the AGP-bundled 2.2.10. Move to it only after a deliberate KGP/AGP upgrade.

Every PR must pass `./gradlew assembleDebug testDebugUnitTest lintDebug`. It also needs a manual release
smoke test (`assembleRelease` + open each screen), because R8 problems only appear in release.

**Follow-ups (separate issues, not part of #20):** raise `compileSdk`/`targetSdk` to 36 (unlocks the newest core/activity;
targetSdk 36 changes behavior); upgrade AGP 9.1 → 9.5 (newer bundled Kotlin, unlocks Hilt 2.60+).

## Proposed Entry for docs/DECISIONS.md

```markdown
## D-006: Dependency upgrades in grouped PRs, then Dependabot

- **Date:** YYYY-MM-DD
- **Status:** Accepted
- **Issue / PR:** #20
- **Decided by:** owner

**Context.** Libraries are 1–3 years old, Java target is 11. DI step 2 (#34, D-005) needs Hilt ≥ 2.59.2
and KSP ≥ 2.3.6 on AGP 9. `compileSdk = 35` caps core-ktx at 1.16.0 and activity at 1.10.1; AGP 9.1.0
bundles Kotlin 2.2.10. See [docs/proposals/20-dependency-upgrades.md](proposals/20-dependency-upgrades.md).

**Options.**
1. Everything in one PR — fast / regressions are hard to localize.
2. Grouped PRs (Java 17, network, coroutines+lifecycle, AndroidX UI, Coil), then Dependabot — small reviewable PRs / more PRs.
3. Dependabot only — no manual work / proposes versions that break on compileSdk 35, no control over order.

**Decision.** Option 2, after #9, in the order Java 17 → Retrofit 3.0.0 → coroutines 1.11.0 + lifecycle 2.11.0
→ AndroidX UI (capped for compileSdk 35) → Coil 2.7.0 → Dependabot with ignore rules for the capped libraries.
Room is not upgraded (waits for #17). #34 pins Hilt 2.59.2 and KSP 2.3.13.

**Why.** One cause of failure per PR; #34 is unblocked without touching AGP/Gradle/SDK; no work is spent on
dependencies that #17/#19 may remove.

**Consequences.** #20 is split into implementation issues (`agent:codex`, `size:S`). Separate follow-ups:
compileSdk/targetSdk 36 and an AGP upgrade (needed before Hilt 2.60+).
```
