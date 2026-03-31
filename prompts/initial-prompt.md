# Промпт для Claude Code: Praefectus — Dota 2 Draft Assistant v1

## Задача

В текущей папке уже создан пустой Kotlin + Gradle проект **Praefectus** через IntelliJ IDEA со следующими параметрами:
- **GroupId:** `me.anlear`
- **ArtifactId:** `praefectus`
- **JDK:** 25 (Oracle OpenJDK 25.0.1, LTS)
- **Gradle:** 9.2.0, Wrapper, Kotlin DSL
- **Без sample code, без multi-module**

Преврати этот пустой проект в полноценное десктопное приложение **Praefectus — Dota 2 Draft Assistant**. Приложение помогает с драфтами в Dota 2 для патча 7.41a, поддерживает режимы All Pick и Captains Mode, и использует реальные данные из STRATZ GraphQL API.

---

## Технический стек

- **Язык:** Kotlin 2.x (JDK 25 LTS)
- **UI:** Compose Desktop (JetBrains Compose Multiplatform)
- **Сборка:** Gradle 9.2.0, Kotlin DSL (`build.gradle.kts`), уже настроен через IntelliJ
- **HTTP:** Ktor Client (CIO engine)
- **GraphQL:** Ktor Client + ручные GraphQL-запросы (JSON строки; не используй Apollo — он избыточен)
- **JSON:** kotlinx.serialization
- **Кэш:** SQLite через `org.xerial:sqlite-jdbc` + Exposed ORM (JetBrains)
- **Корутины:** kotlinx-coroutines
- **Локализация:** RU + EN (через resource bundle или простой enum/map)
- **Иконки героев:** загружай из CDN Valve: `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/{hero_name}.png` (hero_name — это internal name без "npc_dota_hero_" префикса, например `anti_mage`, `crystal_maiden`)

---

## Источник данных: STRATZ GraphQL API

**Endpoint:** `https://api.stratz.com/graphql`  
**Auth:** Bearer token в заголовке `Authorization`. Токен хранится в локальном конфиге `~/.praefectus/config.properties`. При первом запуске показывай окно с просьбой ввести токен и ссылку на https://stratz.com/api для его получения.

### Ключевые GraphQL-запросы, которые нужно реализовать:

#### 1. Список всех героев с ролями и атрибутами
```graphql
query {
  constants {
    heroes {
      id
      name
      displayName
      shortName
      primaryAttribute
      attackType
      roles {
        roleId
        level
      }
    }
  }
}
```

#### 2. Статистика героев (винрейт, пикрейт) с фильтром по бракету
```graphql
query HeroStats($bracketIds: [RankBracket]) {
  heroStats {
    stats(bracketIds: $bracketIds, gameVersionId: CURRENT_PATCH_ID) {
      heroId
      matchCount
      winCount
      pickCount
      banCount
    }
  }
}
```
Значения `bracketIds`: `HERALD_GUARDIAN`, `CRUSADER_ARCHON`, `LEGEND_ANCIENT`, `DIVINE_IMMORTAL`, `IMMORTAL`.

#### 3. Матчапы героев (контрпики)
```graphql
query HeroMatchups($heroId: Short!, $bracketIds: [RankBracket]) {
  heroStats {
    heroVsHeroMatchup(heroId: $heroId, bracketIds: $bracketIds, gameVersionId: CURRENT_PATCH_ID) {
      advantage {
        heroId
        with {
          heroId2
          matchCount
          winCount
        }
        vs {
          heroId2
          matchCount
          winCount
        }
      }
    }
  }
}
```

#### 4. Текущая версия патча
```graphql
query {
  constants {
    gameVersions {
      id
      name
    }
  }
}
```
Используй последнюю версию из списка как `CURRENT_PATCH_ID`.

### Важные замечания по API:
- STRATZ использует GraphQL — запрашивай только нужные поля
- Используй batch-запросы где возможно, чтобы экономить лимит (2000 req/hour)
- Кэшируй ответы в SQLite. TTL кэша: 6 часов для статистики, 24 часа для констант (герои, предметы)
- При ошибке API показывай понятное сообщение пользователю, не крашься
- Предусмотри кнопку "Обновить данные" для принудительного обновления кэша

---

## Архитектура приложения

### Структура пакетов:
```
me.anlear.praefectus/
├── Main.kt                    # Точка входа, Application window
├── data/
│   ├── api/
│   │   ├── StratzApiClient.kt      # HTTP + GraphQL запросы
│   │   └── models/                  # DTO модели ответов API
│   ├── cache/
│   │   ├── DatabaseFactory.kt       # SQLite setup (Exposed)
│   │   └── tables/                  # Таблицы: Heroes, HeroStats, HeroMatchups
│   └── repository/
│       ├── HeroRepository.kt        # Основной репозиторий (API + кэш)
│       └── DraftRepository.kt       # Логика оценки драфта
├── domain/
│   ├── models/                      # Domain модели: Hero, HeroMatchup, DraftState
│   ├── DraftEngine.kt              # Алгоритм оценки драфта
│   └── RecommendationEngine.kt     # Движок рекомендаций пиков
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                 # Цвета, типографика (тёмная тема в стиле Dota 2)
│   │   └── Strings.kt              # Локализация RU/EN
│   ├── components/                  # Переиспользуемые компоненты
│   │   ├── HeroCard.kt             # Карточка героя с иконкой
│   │   ├── HeroGrid.kt            # Сетка героев для выбора
│   │   ├── DraftPanel.kt          # Панель текущего драфта (Radiant/Dire)
│   │   ├── RecommendationList.kt   # Список рекомендованных пиков
│   │   └── RankSelector.kt        # Выпадающий список бракетов
│   ├── screens/
│   │   ├── DraftScreen.kt          # Основной экран драфта
│   │   ├── TierListScreen.kt       # Экран тир-листа
│   │   └── SettingsScreen.kt       # Настройки (токен, язык, бракет)
│   └── navigation/
│       └── AppNavigation.kt        # Навигация между экранами
└── util/
    ├── Config.kt                   # Работа с config.properties
    └── Localization.kt             # Мультиязычность
```

### Паттерн: MVVM
- Каждый экран имеет свой ViewModel (на основе `kotlinx.coroutines.flow.StateFlow`)
- Repository — единая точка доступа к данным (API → кэш → UI)
- Не используй Hilt/Dagger — для десктопа достаточно простой ручной DI (передавай зависимости через конструктор)

---

## Функционал v1 (по приоритету)

### 1. Рекомендации пиков/контрпиков (ГЛАВНАЯ ФИЧА)

**Алгоритм оценки:**
Для каждого кандидата на пик рассчитывается **Draft Score**, который учитывает:

```
DraftScore(hero) = 
  Σ(winrate_vs_enemy[i] - 50) * COUNTER_WEIGHT     // контрпик: насколько герой хорош против вражеских
  + Σ(winrate_with_ally[j] - 50) * SYNERGY_WEIGHT   // синергия: насколько герой хорош с союзными
  + (hero_winrate - 50) * META_WEIGHT                // текущая сила героя в мете
```

Веса по умолчанию: `COUNTER_WEIGHT = 1.5`, `SYNERGY_WEIGHT = 1.0`, `META_WEIGHT = 0.5`  
Показывай score как число и как цвет (зелёный = хорошо, красный = плохо).

Фильтруй рекомендации:
- Убери уже выбранных/забаненных героев
- Позволь фильтровать по роли (Carry, Mid, Offlane, Support, Hard Support)
- Сортируй по DraftScore (лучшие сверху)

**Для каждого рекомендованного героя показывай:**
- Иконку героя
- DraftScore (число + цветовой индикатор)
- Мини-бар с breakdown: сколько дали контрпики, сколько синергии, сколько мета
- Винрейт героя в текущей мете
- Tooltip или expandable секция с деталями: против каких вражеских героев хорош/плох

### 2. Синергии между героями в команде

В панели текущего драфта показывай:
- **Team Synergy Score** — среднее значение попарных winrate_with для всех пар в команде
- Цветовой индикатор: хорошая/средняя/плохая синергия
- При наведении на конкретную пару героев — их попарный винрейт

### 3. Тир-лист героев по ролям

Отдельный экран с таблицей:
- Колонки: герой, винрейт, пикрейт, банрейт, тир (S/A/B/C/D)
- Фильтр по роли (Carry, Mid, Offlane, Soft Support, Hard Support)
- Фильтр по бракету
- Тир рассчитывается автоматически: S = топ-10% по винрейту, A = следующие 20%, и т.д.
- Минимум матчей для включения в тир-лист: 100 (настраиваемо)
- Сортировка по клику на заголовок колонки

### 4. Трекинг драфта

**All Pick режим:**
- Два столбца: Radiant (зелёный) и Dire (красный)
- 5 слотов на каждую команду
- Клик по герою в сетке → выбираешь, куда он идёт (Radiant/Dire/Ban)
- Список банов отдельно внизу
- Кнопка "Сбросить драфт"

**Captains Mode режим:**
- Стандартная последовательность CM драфта Dota 2:
    - Ban Phase 1: Radiant ban, Dire ban, Radiant ban, Dire ban
    - Pick Phase 1: Radiant pick, Dire pick, Dire pick, Radiant pick
    - Ban Phase 2: Radiant ban, Dire ban, Radiant ban, Dire ban
    - Pick Phase 2: Radiant pick, Dire pick, Dire pick, Radiant pick
    - Ban Phase 3: Radiant ban, Dire ban
    - Pick Phase 3: Radiant pick, Dire pick
- Индикатор текущего шага (подсвечивается)
- Авто-переключение между Radiant/Dire по очерёдности
- Кнопка "Отмена последнего действия" (undo)
- Кнопка "Сбросить драфт"

---

## UI/UX Требования

### Тёмная тема в стиле Dota 2:
- Основной фон: `#1a1a2e` (тёмно-синий)
- Вторичный фон: `#16213e`
- Акцент Radiant: `#4ade80` (зелёный)
- Акцент Dire: `#ef4444` (красный)
- Текст: `#e0e0e0`
- Вторичный текст: `#9ca3af`
- Карточки/панели: `#1e293b` с лёгким border `#334155`
- Accent/highlight: `#3b82f6` (синий)

### Размер окна:
- Минимум: 1280x720
- Рекомендуемый: 1440x900
- Resizable

### Сетка героев:
- Показывай всех героев Dota 2 (126+ героев) в скроллируемой сетке
- Иконки героев 48x48 px
- Поиск героя по имени (текстовое поле сверху)
- Фильтр по атрибуту (Str/Agi/Int/Uni) и по типу атаки (Melee/Ranged)
- Уже выбранные/забаненные герои визуально затемняются
- При наведении — tooltip с именем и ролями

### Навигация:
- Боковая панель слева с иконками: Draft, Tier List, Settings
- Или табы сверху — на твоё усмотрение, главное чтобы удобно

---

## Локализация (RU + EN)

Реализуй через простой enum + map:

```kotlin
enum class Lang { RU, EN }

object Strings {
    private val strings = mapOf(
        "draft" to mapOf(Lang.RU to "Драфт", Lang.EN to "Draft"),
        "tier_list" to mapOf(Lang.RU to "Тир-лист", Lang.EN to "Tier List"),
        "settings" to mapOf(Lang.RU to "Настройки", Lang.EN to "Settings"),
        "recommend" to mapOf(Lang.RU to "Рекомендации", Lang.EN to "Recommendations"),
        "radiant" to mapOf(Lang.RU to "Свет", Lang.EN to "Radiant"),
        "dire" to mapOf(Lang.RU to "Тьма", Lang.EN to "Dire"),
        "ban" to mapOf(Lang.RU to "Бан", Lang.EN to "Ban"),
        "pick" to mapOf(Lang.RU to "Пик", Lang.EN to "Pick"),
        "reset_draft" to mapOf(Lang.RU to "Сбросить драфт", Lang.EN to "Reset Draft"),
        "undo" to mapOf(Lang.RU to "Отменить", Lang.EN to "Undo"),
        "search_hero" to mapOf(Lang.RU to "Поиск героя...", Lang.EN to "Search hero..."),
        "winrate" to mapOf(Lang.RU to "Винрейт", Lang.EN to "Win Rate"),
        "pickrate" to mapOf(Lang.RU to "Пикрейт", Lang.EN to "Pick Rate"),
        "banrate" to mapOf(Lang.RU to "Банрейт", Lang.EN to "Ban Rate"),
        "synergy" to mapOf(Lang.RU to "Синергия", Lang.EN to "Synergy"),
        "counter" to mapOf(Lang.RU to "Контрпик", Lang.EN to "Counter"),
        "all_pick" to mapOf(Lang.RU to "All Pick", Lang.EN to "All Pick"),
        "captains_mode" to mapOf(Lang.RU to "Captains Mode", Lang.EN to "Captains Mode"),
        "rank_bracket" to mapOf(Lang.RU to "Бракет ранга", Lang.EN to "Rank Bracket"),
        "update_data" to mapOf(Lang.RU to "Обновить данные", Lang.EN to "Update Data"),
        "api_token" to mapOf(Lang.RU to "API Токен", Lang.EN to "API Token"),
        "language" to mapOf(Lang.RU to "Язык", Lang.EN to "Language"),
        // ... добавь все нужные строки
    )
    
    fun get(key: String, lang: Lang): String = strings[key]?.get(lang) ?: key
}
```

---

## SQLite схема кэша

```sql
CREATE TABLE IF NOT EXISTS heroes (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    display_name TEXT NOT NULL,
    short_name TEXT NOT NULL,
    primary_attribute TEXT NOT NULL,
    attack_type TEXT NOT NULL,
    roles TEXT NOT NULL,  -- JSON массив
    updated_at INTEGER NOT NULL  -- epoch millis
);

CREATE TABLE IF NOT EXISTS hero_stats (
    hero_id INTEGER NOT NULL,
    bracket TEXT NOT NULL,
    patch_id INTEGER NOT NULL,
    match_count INTEGER NOT NULL,
    win_count INTEGER NOT NULL,
    pick_count INTEGER NOT NULL,
    ban_count INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (hero_id, bracket, patch_id)
);

CREATE TABLE IF NOT EXISTS hero_matchups (
    hero_id INTEGER NOT NULL,
    against_hero_id INTEGER NOT NULL,
    bracket TEXT NOT NULL,
    patch_id INTEGER NOT NULL,
    match_count INTEGER NOT NULL,
    win_count INTEGER NOT NULL,
    is_with BOOLEAN NOT NULL,  -- true = ally, false = enemy
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (hero_id, against_hero_id, bracket, patch_id, is_with)
);

CREATE TABLE IF NOT EXISTS cache_metadata (
    key TEXT PRIMARY KEY,
    updated_at INTEGER NOT NULL
);
```

---

## Порядок реализации

Реализуй поэтапно, проверяя компиляцию после каждого шага:

1. **Gradle конфигурация** — добавь в существующий build.gradle.kts плагин Compose Desktop, все зависимости (Ktor, Exposed, kotlinx.serialization, SQLite), настрой `compose.desktop { application { mainClass = "me.anlear.praefectus.MainKt" } }`
2. **Data layer** — API client, модели, SQLite кэш
3. **Domain layer** — DraftEngine, RecommendationEngine
4. **UI Theme + Components** — тема, переиспользуемые компоненты
5. **Draft Screen** — основной экран с All Pick
6. **Captains Mode** — добавь поддержку CM
7. **Tier List Screen** — отдельный экран
8. **Settings Screen** — настройки (токен, язык, бракет)
9. **Навигация** — собери всё вместе
10. **Polish** — проверь edge cases, ошибки сети, пустые состояния

---

## Важные ограничения и требования

- **Не используй** Android-специфичные зависимости (AndroidX, R.drawable и т.д.)
- **Используй** `org.jetbrains.compose` а НЕ `androidx.compose` для десктопа
- **JDK 25** — уже настроен в проекте, укажи `jvmToolchain(25)` в build.gradle.kts
- **Kotlin 2.x** / Compose BOM последней стабильной версии, совместимой с Kotlin версией
- Все сетевые вызовы — через корутины, не блокируй UI thread
- Грациозная обработка ошибок: нет сети → покажи кэшированные данные; нет кэша → покажи сообщение
- Загрузка иконок героев — через `AsyncImage` или ленивую загрузку с кэшем на диск
- **Код и комментарии — на английском**, UI строки — на двух языках через локализацию
- Приложение должно компилироваться и запускаться командой `./gradlew run`
- **Не создавай новых модулей Gradle** — используй существующий корневой модуль, добавь в него Compose Desktop plugin
- GroupId: `me.anlear`, пакет: `me.anlear.praefectus`