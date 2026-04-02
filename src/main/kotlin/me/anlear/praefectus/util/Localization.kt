package me.anlear.praefectus.util

enum class Lang { RU, EN }

object Strings {
    private val strings = mapOf(
        "app_title" to mapOf(Lang.RU to "Praefectus — Помощник по драфту Dota 2", Lang.EN to "Praefectus — Dota 2 Draft Assistant"),
        "draft" to mapOf(Lang.RU to "Драфт", Lang.EN to "Draft"),
        "tier_list" to mapOf(Lang.RU to "Тир-лист", Lang.EN to "Tier List"),
        "settings" to mapOf(Lang.RU to "Настройки", Lang.EN to "Settings"),
        "recommend" to mapOf(Lang.RU to "Рекомендации", Lang.EN to "Recommendations"),
        "radiant" to mapOf(Lang.RU to "Свет", Lang.EN to "Radiant"),
        "dire" to mapOf(Lang.RU to "Тьма", Lang.EN to "Dire"),
        "ban" to mapOf(Lang.RU to "Бан", Lang.EN to "Ban"),
        "bans" to mapOf(Lang.RU to "Баны", Lang.EN to "Bans"),
        "pick" to mapOf(Lang.RU to "Пик", Lang.EN to "Pick"),
        "reset_draft" to mapOf(Lang.RU to "Сбросить драфт", Lang.EN to "Reset Draft"),
        "undo" to mapOf(Lang.RU to "Отменить", Lang.EN to "Undo"),
        "search_hero" to mapOf(Lang.RU to "Поиск героя...", Lang.EN to "Search hero..."),
        "winrate" to mapOf(Lang.RU to "Винрейт", Lang.EN to "Win Rate"),
        "pickrate" to mapOf(Lang.RU to "Пикрейт", Lang.EN to "Pick Rate"),
        "banrate" to mapOf(Lang.RU to "Банрейт", Lang.EN to "Ban Rate"),
        "synergy" to mapOf(Lang.RU to "Синергия", Lang.EN to "Synergy"),
        "counter" to mapOf(Lang.RU to "Контрпик", Lang.EN to "Counter"),
        "meta" to mapOf(Lang.RU to "Мета", Lang.EN to "Meta"),
        "all_pick" to mapOf(Lang.RU to "All Pick", Lang.EN to "All Pick"),
        "captains_mode" to mapOf(Lang.RU to "Captains Mode", Lang.EN to "Captains Mode"),
        "rank_bracket" to mapOf(Lang.RU to "Ранг", Lang.EN to "Rank"),
        "update_data" to mapOf(Lang.RU to "Обновить данные", Lang.EN to "Update Data"),
        "api_token" to mapOf(Lang.RU to "API Токен", Lang.EN to "API Token"),
        "language" to mapOf(Lang.RU to "Язык", Lang.EN to "Language"),
        "save" to mapOf(Lang.RU to "Сохранить", Lang.EN to "Save"),
        "loading" to mapOf(Lang.RU to "Загрузка...", Lang.EN to "Loading..."),
        "error" to mapOf(Lang.RU to "Ошибка", Lang.EN to "Error"),
        "no_data" to mapOf(Lang.RU to "Нет данных. Обновите кэш.", Lang.EN to "No data. Update cache."),
        "hero" to mapOf(Lang.RU to "Герой", Lang.EN to "Hero"),
        "tier" to mapOf(Lang.RU to "Тир", Lang.EN to "Tier"),
        "matches" to mapOf(Lang.RU to "Матчи", Lang.EN to "Matches"),
        "all_roles" to mapOf(Lang.RU to "Все роли", Lang.EN to "All Roles"),
        "carry" to mapOf(Lang.RU to "Керри", Lang.EN to "Carry"),
        "mid" to mapOf(Lang.RU to "Мид", Lang.EN to "Mid"),
        "offlane" to mapOf(Lang.RU to "Оффлейн", Lang.EN to "Offlane"),
        "support" to mapOf(Lang.RU to "Поддержка", Lang.EN to "Support"),
        "hard_support" to mapOf(Lang.RU to "Полная поддержка", Lang.EN to "Hard Support"),
        "loading_matchups" to mapOf(Lang.RU to "Загрузка матчапов", Lang.EN to "Loading matchups"),
        "score" to mapOf(Lang.RU to "Оценка", Lang.EN to "Score"),
        "team_synergy" to mapOf(Lang.RU to "Синергия команды", Lang.EN to "Team Synergy"),
        "draft_score" to mapOf(Lang.RU to "Оценка драфта", Lang.EN to "Draft Score"),
        "filter_str" to mapOf(Lang.RU to "Сила", Lang.EN to "Str"),
        "filter_agi" to mapOf(Lang.RU to "Ловкость", Lang.EN to "Agi"),
        "filter_int" to mapOf(Lang.RU to "Интеллект", Lang.EN to "Int"),
        "filter_uni" to mapOf(Lang.RU to "Универсал", Lang.EN to "Uni"),
        "filter_melee" to mapOf(Lang.RU to "Ближний бой", Lang.EN to "Melee"),
        "filter_ranged" to mapOf(Lang.RU to "Дальний бой", Lang.EN to "Ranged"),
        "all" to mapOf(Lang.RU to "Все", Lang.EN to "All"),
        "token_prompt_title" to mapOf(Lang.RU to "Требуется API токен", Lang.EN to "API Token Required"),
        "token_prompt_text" to mapOf(
            Lang.RU to "Введите ваш STRATZ API токен.\nПолучить токен можно на:",
            Lang.EN to "Enter your STRATZ API token.\nGet your token at:"
        ),
        "min_matches" to mapOf(Lang.RU to "Мин. матчей", Lang.EN to "Min Matches"),
        "updating" to mapOf(Lang.RU to "Обновление...", Lang.EN to "Updating..."),
        "data_updated" to mapOf(Lang.RU to "Данные обновлены", Lang.EN to "Data updated"),
        "network_error" to mapOf(Lang.RU to "Ошибка сети. Показаны кэшированные данные.", Lang.EN to "Network error. Showing cached data."),
        "no_token" to mapOf(Lang.RU to "Токен API не задан. Откройте настройки.", Lang.EN to "API token not set. Open settings."),
        "current_step" to mapOf(Lang.RU to "Текущий шаг", Lang.EN to "Current Step"),
        "phase" to mapOf(Lang.RU to "Фаза", Lang.EN to "Phase"),
        "your_side" to mapOf(Lang.RU to "Ваша сторона", Lang.EN to "Your Side"),
        "forces_of_light" to mapOf(Lang.RU to "Силы Света", Lang.EN to "Radiant"),
        "forces_of_dark" to mapOf(Lang.RU to "Силы Тьмы", Lang.EN to "Dire"),
        "rank_herald" to mapOf(Lang.RU to "Рекрут", Lang.EN to "Herald"),
        "rank_guardian" to mapOf(Lang.RU to "Страж", Lang.EN to "Guardian"),
        "rank_crusader" to mapOf(Lang.RU to "Рыцарь", Lang.EN to "Crusader"),
        "rank_archon" to mapOf(Lang.RU to "Герой", Lang.EN to "Archon"),
        "rank_legend" to mapOf(Lang.RU to "Легенда", Lang.EN to "Legend"),
        "rank_ancient" to mapOf(Lang.RU to "Властелин", Lang.EN to "Ancient"),
        "rank_divine" to mapOf(Lang.RU to "Божество", Lang.EN to "Divine"),
        "rank_immortal" to mapOf(Lang.RU to "Титан", Lang.EN to "Immortal"),
        "help" to mapOf(Lang.RU to "Помощь", Lang.EN to "Help"),
        "help_title" to mapOf(Lang.RU to "Как пользоваться Praefectus", Lang.EN to "How to use Praefectus"),
        "help_text" to mapOf(
            Lang.RU to """
1. Выберите режим драфта: All Pick или Captains Mode.

2. Выберите вашу сторону: Силы Света (Radiant) или Силы Тьмы (Dire).

3. В режиме All Pick кликните на слот (пик или бан), затем выберите героя из пула внизу.

4. В режиме Captains Mode герои назначаются автоматически по последовательности драфта.

5. Рекомендации внизу показывают лучших героев на основе:
   • Контрпик — насколько герой силён против вражеских пиков
   • Синергия — насколько герой сочетается с союзниками
   • Мета — текущий винрейт героя в выбранном ранге

6. Подсвеченные герои в пуле — лучшие по комбинированной оценке (мета + рекомендации). Подсвечиваются только герои тира A и выше.

7. Значки с номерами на иконках героев показывают их позицию в топ-10 рекомендаций.

8. Нажмите на уже выбранного/забаненного героя в пуле, чтобы отменить его выбор.

9. Под пиками каждой команды отображается анализ драфта: контрпик, синергия и мета.

10. Тир-лист (вкладка слева) показывает всех героев отсортированных по винрейту для текущего ранга.

11. В настройках можно изменить API токен, язык, ранг и обновить данные.
            """.trimIndent(),
            Lang.EN to """
1. Select draft mode: All Pick or Captains Mode.

2. Choose your side: Radiant or Dire.

3. In All Pick mode, click a slot (pick or ban), then select a hero from the pool below.

4. In Captains Mode, heroes are assigned automatically following the CM draft sequence.

5. Recommendations at the bottom show the best heroes based on:
   • Counter — how strong the hero is against enemy picks
   • Synergy — how well the hero works with your allies
   • Meta — the hero's current win rate at the selected rank

6. Highlighted heroes in the pool are the best by combined score (meta + recommendations). Only tier A and above heroes are highlighted.

7. Numbered badges on hero icons show their position in the top 10 recommendations.

8. Click on a picked/banned hero in the pool to undo their selection.

9. Draft analysis is shown under each team's picks: counter, synergy, and meta scores.

10. The Tier List tab (left sidebar) shows all heroes sorted by win rate for the current rank.

11. In Settings, you can change the API token, language, rank bracket, and update data.
            """.trimIndent()
        ),
        "close" to mapOf(Lang.RU to "Закрыть", Lang.EN to "Close"),
        "draft_advantage" to mapOf(Lang.RU to "Преимущество драфта", Lang.EN to "Draft Advantage"),
        "win_probability" to mapOf(Lang.RU to "Вероятность победы", Lang.EN to "Win Probability"),
        "better_draft" to mapOf(Lang.RU to "Лучший драфт", Lang.EN to "Better Draft"),
        "support_bonus" to mapOf(Lang.RU to "Бонус поддержки", Lang.EN to "Support Bonus"),
        "support_bonus_desc" to mapOf(
            Lang.RU to "Добавить +3.0 балла героям поддержки в первых 2 пиках",
            Lang.EN to "Add +3.0 score to support heroes in first 2 picks"
        ),
    )

    fun get(key: String, lang: Lang): String = strings[key]?.get(lang) ?: key
}
