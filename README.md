# Praefectus — Dota 2 Draft Assistant

A desktop application that helps with hero drafting in Dota 2, providing real-time counter-pick recommendations, team synergy analysis, and a tier list based on current meta statistics.

Built with **Kotlin + Compose Desktop**, powered by **STRATZ GraphQL API**.

## Features

- **Smart Draft Recommendations** — suggests optimal picks based on counter-matchups, team synergies, and current meta winrates
- **All Pick & Captains Mode** — supports both draft formats with full pick/ban phase tracking
- **Tier List** — auto-generated hero rankings by role, filterable by rank bracket
- **Team Synergy Analysis** — evaluates how well your team composition works together
- **Rank Bracket Filter** — statistics tailored to your MMR range (Herald to Immortal)
- **Bilingual UI** — Russian and English interface

## Screenshots

<!-- TODO: Add screenshots after first working version -->

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.x (JDK 25) |
| UI | Compose Desktop (JetBrains) |
| Build | Gradle 9.2 (Kotlin DSL) |
| HTTP | Ktor Client |
| Data | STRATZ GraphQL API |
| Cache | SQLite + Exposed ORM |
| Async | Kotlin Coroutines + Flow |

## Getting Started

### Prerequisites
- JDK 25+
- STRATZ API token ([get one here](https://stratz.com/api))

### Run
```bash
./gradlew run
```

On first launch, the app will ask for your STRATZ API token.

## How This Project Was Built

This project was developed collaboratively with AI (Claude by Anthropic). The process:

1. **Prompt engineering** — I designed a detailed specification covering architecture, data sources, algorithms, and UI requirements
2. **Code generation** — Claude Code generated the initial codebase from the prompt
3. **Iteration & refinement** — I reviewed, tested, and refined the code manually

The original prompt that generated v1 of this project is available in [`prompts/initial-prompt.md`](prompts/initial-prompt.md). I believe transparency about AI-assisted development is important — it shows the ability to effectively collaborate with AI tools, which is a valuable modern engineering skill.

## Architecture

```
me.anlear.praefectus/
├── data/          # API client, cache, repositories
├── domain/        # Draft engine, recommendation algorithm
├── ui/            # Compose Desktop screens & components
└── util/          # Config, localization
```

**Draft Score Algorithm:**
```
Score(hero) = Σ(vs_enemy_advantage) × 1.5
            + Σ(with_ally_synergy)  × 1.0
            + (meta_winrate - 50%)  × 0.5
```

## Roadmap

- [x] v1: Draft recommendations, counter-picks, synergies, tier list
- [ ] v2: Item build recommendations based on draft composition
- [ ] v2: Personal stats integration (your hero pool & winrates)
- [ ] v3: Real-time draft tracking via Dota 2 GSI

## License

This project is licensed under the BSL License — see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [STRATZ](https://stratz.com) — Dota 2 statistics API
- [Valve](https://www.dota2.com) — hero icons and game data