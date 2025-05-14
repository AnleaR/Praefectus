# Praefectus — AI Dota 2 Draft Assistant

Praefectus is an advanced AI-powered assistant for Dota 2 hero selection, designed to enhance the drafting experience by providing real-time recommendations directly in-game.

Unlike traditional static hero suggestion tools, Praefectus leverages a custom-trained AI model that is updated for each Dota 2 patch and takes into account:
- The current picks and bans of both teams.
- The player's personal hero preferences and comfort picks.
- Live meta analytics and win rate trends (in future updates).

The application runs as an Overwolf Electron app, providing a seamless in-game overlay experience without requiring the player to tab out of the game.

For more information about the Overwolf Electron framework, refer to the official [Overwolf Electron documentation](https://overwolf.github.io/tools/ow-electron).

---

## Features
- 🧠 AI-driven hero recommendations based on live draft state.
- 🎯 Personalized suggestions based on player's favorite heroes.
- 🔄 Patch-specific AI models.
- 💻 Works in all Dota 2 game modes (Turbo, All Pick, Ranked All Pick, Captains Mode, etc.).
- 🖥️ Overlay UI integrated on top of the Dota 2 interface (via Overwolf).

---

## Setup

1. Install the dependencies using your preferred package manager ([Yarn](https://yarnpkg.com/), [npm](https://www.npmjs.com/)):

```shell
# Using npm
npm install

# Using yarn
yarn install
```

## Quick start 

To run the app in development mode:

```shell
# Using npm
npm run build
npm run start

# Using yarn
yarn build
yarn start
```

This will launch the app using Overwolf Electron in development mode.

## Quick Build (Production Mode)

To build the app for production deployment:

```shell
# Using npm
npm run build
npm run build:ow-electron

# Using yarn
yarn build
yarn build:ow-electron
```

The resulting installer will be located in the `/build` folder.

## Customizing Overwolf Packages

To add or remove Overwolf Electron packages (such as Game Events Provider, Overlay, etc.), simply edit the `overwolf.packages` array inside the [package.json](/package/json) file:

```json
"overwolf": {
  "packages": [
    "gep",
    "overlay"
  ]
}
```

## Roadmap

- ☑ Writing core application logic (version from 0.1.0 to 0.2.0). 

- ☐ Overlay integration with Dota 2 using Overwolf (version from 0.2.0 to 0.3.0).

- ☐ Hero suggestion system based on team compositions (version from 0.3.0 to 0.4.0).

- ☐ AI model integration (version from 0.4.0 to 1.0.0).

- ☐ Player profile preferences and hero pool customization (version from 1.0.0 to 2.0.0).

- ☐ Multi-language support (version from 2.0.0 to 3.0.0).

- ☐ Publication (version from 3.0.0 to current).

## License

Praefectus is © 2025 AnleaR.
All rights reserved.

This software is licensed under the Custom License.
You can use, copy, and modify the code only for personal, non-commercial purposes.
Commercial use is strictly prohibited unless granted permission by the author (AnleaR).

See the [LICENSE](/LICENSE) file for more details.