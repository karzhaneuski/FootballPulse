# AGENTS.md — FootballPulse

Единый источник инструкций для всех ИИ-агентов (Claude Code, Codex, Gemini / Antigravity и др.).
`CLAUDE.md` и `GEMINI.md` только импортируют этот файл: правки вносить **здесь**.

## 1. Проект

Android-приложение: матчи, турнирные таблицы, детали матча и команды для топ-5 европейских лиг.
Данные: REST API `https://api.football-data.org/v4/` (бесплатный тариф, **10 запросов/мин**).

## 2. Стек (фактический, см. `docs/AUDIT.md` §1)

| Что | Версия / значение |
|---|---|
| Gradle Wrapper | 9.3.1 (daemon toolchain JDK 21, `gradle/gradle-daemon-jvm.properties`) |
| AGP | 9.1.0 (Kotlin встроен в AGP, отдельного Kotlin-плагина нет) |
| Java target | 11 (`compileOptions`) |
| SDK | compile/target 35, min 26 |
| UI | XML Views + `findViewById`, Material 3, ConstraintLayout, RecyclerView |
| Асинхронность | Kotlin Coroutines + `StateFlow` |
| Сеть | Retrofit 2.9 + Gson, OkHttp logging-interceptor 4.12 |
| Изображения | Coil 2.6 (без `coil-svg`) |
| Тесты | JUnit 4 (реальных тестов пока нет) |
| Объявлены, но **не используются** | Room 2.6.1, плагин Hilt 2.50 (`apply false`) — судьба решается в issues `needs-decision` |

Версии библиотек — в `gradle/libs.versions.toml` (частично) и строками в `app/build.gradle.kts`.

## 3. Архитектура

Один модуль `:app`, MVVM, Single Activity.

```
com.artsiom.footballpulse
├── MainActivity            # BottomNavigation, ручная навигация фрагментов (add/hide/show + replace)
├── data/
│   ├── FootballRepository  # единственный репозиторий, напрямую дергает RetrofitInstance.api
│   └── remote/             # FootballApiService, RetrofitInstance, *Dto
├── domain/model/           # неизменяемые data class + объект Leagues (UseCase и интерфейсов нет)
└── ui/<screen>/            # Fragment + ViewModel + UiState (+ Adapter): matches, standings,
                            # matchdetails, teamdetails
```

Поток данных: `Fragment` ← `StateFlow<XxxUiState>` (`Loading` / `Success` / `Error`) ← `ViewModel` ← `FootballRepository` ← Retrofit.

Известные отступления от «чистой» архитектуры (ViewModel сами создают репозиторий, логика в адаптерах
и фрагментах, нет DI) описаны в `docs/AUDIT.md` §3 и разбиты на issues. **Не исправляй их попутно**,
только в рамках соответствующей задачи.

## 4. Команды

```bash
./gradlew assembleDebug        # сборка debug APK
./gradlew testDebugUnitTest    # unit-тесты
./gradlew lintDebug            # Android Lint (ошибки валят сборку, предупреждения — нет)
./gradlew assembleDebug testDebugUnitTest lintDebug   # то же, что в CI
```

Перед PR обязательно прогони команду CI локально. Нужен Android SDK (`ANDROID_HOME` или `sdk.dir`
в `local.properties`) и JDK 21.

### API-ключ

`FOOTBALL_API_KEY` берётся по порядку: `local.properties` → переменная окружения `FOOTBALL_API_KEY`
(в CI — секрет репозитория) → пустая строка. Без ключа проект **собирается**, но запросы к API
вернут 401/403. Ключ никогда не коммитится.

## 5. Стиль кода

- Kotlin official code style (`kotlin.code.style=official`), 4 пробела, без wildcard-импортов.
- Новый код следует существующему паттерну экрана: `XxxFragment` + `XxxViewModel` + `sealed XxxUiState`.
- Состояние UI — только через `StateFlow` в ViewModel; фрагмент подписывается через
  `repeatOnLifecycle(Lifecycle.State.STARTED)`.
- DTO живут в `data/remote/`, каждое поле DTO — с `@SerializedName` (нужно для R8 в release).
- DTO не выходят за пределы `data`: в `ui` попадают только модели из `domain/model`.
- В `catch` для корутин никогда не глотай `CancellationException` (перебрасывай её).
- Новые строки, видимые пользователю, — в `res/values/strings.xml`, не хардкодом.
- Даты в новом коде — только `java.time` (minSdk 26 позволяет).
- Новые зависимости — только через `gradle/libs.versions.toml`.
- Комментарии — по делу, объясняют «почему», а не «что».

## 6. Workflow

1. **Issue.** Любая работа начинается с issue по шаблону `.github/ISSUE_TEMPLATE/agent-task.md`
   (контекст, задача, критерии готовности, затрагиваемые файлы, вне рамок).
2. **Ветка** от свежего `main`: `agent/<номер-issue>-<кратко-латиницей>`,
   например `agent/12-retry-button-matches`.
3. **Коммиты** — маленькие, осмысленные, в стиле Conventional Commits:
   `fix: ...`, `feat: ...`, `refactor: ...`, `test: ...`, `chore: ...`, `docs: ...`.
   В сообщении указывай issue: `fix: keep DTO fields for R8 (#3)`.
4. **PR в `main`.** В описании: `Closes #<номер>`, что сделано, как проверено, что осталось.
   CI (сборка + unit-тесты + lint) должен быть зелёным.
5. **Handoff.** Если работа не закончена (лимит, блокер, развилка) — обнови `docs/HANDOFF.md`
   и оставь ссылку на него в PR или issue.
6. **Решения.** Любое архитектурное решение (выбор библиотеки, подхода, отказ от чего-то)
   записывается в `docs/DECISIONS.md`.

## 7. Запреты

- ❌ Не пушить в `main` напрямую, не делать force-push в чужие ветки. Только PR.
- ❌ Не коммитить секреты: API-ключи, `local.properties`, keystore, токены. Ни в коде, ни в логах,
  ни в тестовых фикстурах.
- ❌ Не менять версии зависимостей, AGP, Gradle, JDK, SDK без задачи, которая это явно требует.
- ❌ Не добавлять новые библиотеки без задачи.
- ❌ Не отключать, не удалять и не помечать `@Ignore` тесты и lint-проверки, чтобы позеленеть CI.
- ❌ Не выходить за рамки issue: замеченные проблемы — отдельным issue, а не попутным фиксом.
- ❌ Не принимать архитектурные решения молча: развилка → метка `needs-decision` и вопрос владельцу.

## 8. Маршрутизация

Каждое issue получает метку размера и метку исполнителя.

| Метка | Когда ставить |
|---|---|
| `agent:codex` | Задача размера **S** с чёткими проверяемыми критериями готовности: 1–3 файла, без выбора подхода, результат проверяется сборкой/тестом/конкретным поведением. |
| `agent:antigravity` | Крупные многофайловые изменения (**M/L**): рефакторинг через несколько слоёв, миграции, массовые механические правки, новая фича на несколько экранов. Подход уже выбран. |
| `needs-decision` | Есть архитектурная развилка (какую библиотеку/подход выбрать, менять ли структуру). Агент **не начинает** реализацию, пока владелец не примет решение и оно не записано в `docs/DECISIONS.md`. После решения метка снимается и ставится `agent:*`. |

Размеры:

| Метка | Ориентир |
|---|---|
| `size:S` | до ~1–2 часов, 1–3 файла, без новых зависимостей или с одной очевидной |
| `size:M` | полдня–день, 4–10 файлов или несколько слоёв |
| `size:L` | несколько дней, затрагивает большую часть приложения |

Если задача размера M маленькая по числу файлов и не требует выбора подхода, её можно отдать
`agent:codex`; если сомневаешься — `agent:antigravity`.

## 9. Полезные файлы

- `docs/AUDIT.md` — технический аудит (источник задач).
- `docs/DECISIONS.md` — журнал архитектурных решений.
- `docs/HANDOFF.md` — передача незавершённой работы между агентами.
- `.github/workflows/android-ci.yml` — CI.
