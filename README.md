# Gilded Spade Sync

A RuneLite plugin that exposes your Old School RuneScape player data over a local WebSocket, enabling a companion web app to read and interact with your in-game progress in real time.

## What it does

Gilded Spade Sync bridges OSRS and the browser. It runs a localhost WebSocket server inside RuneLite that a companion web app can connect to, providing:

- **Player progress** -- quests, skills, achievement diaries, combat achievements, collection log
- **Live game state** -- inventory, equipment, bank, slayer task, daily tasks, world location
- **Interactive bank sorting** -- the web app can filter your bank and guide you through a step-by-step sorting workflow

All communication stays on localhost. Data is saved to your browsers local storage on the web app. 
You have the option to sync to a cloud save on the web app (by logging in).
