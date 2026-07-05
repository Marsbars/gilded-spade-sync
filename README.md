# Gilded Spade Sync

A RuneLite plugin that exposes your Old School RuneScape player data over a local WebSocket, enabling a companion web app to read and interact with your in-game progress in real time.

## What it does

Gilded Spade Sync bridges OSRS and the browser. It runs a localhost WebSocket server inside RuneLite that a companion web app can connect to, providing:

- **Player progress** -- quests, skills, achievement diaries, combat achievements, collection log
- **Live game state** -- inventory, equipment, bank, slayer task, daily tasks, world location
- **Interactive bank sorting** -- the web app can filter your bank and guide you through a step-by-step sorting workflow

All communication stays on localhost. Data is saved to your browsers local storage on the web app. 
You have the option to sync to a cloud save on the web app (by logging in).

## Companion web app features

When used with the Gilded Spade web app, the plugin unlocks live RuneLite-powered views and tools:

- **Dashboard** -- Build a modular overview of your current account state. Add, remove, resize, and reorder panels for live player location, slayer task progress, boosted stats, daily task readiness, inventory, and equipped items. Connected modules poll RuneLite automatically so the dashboard stays current while you play.
- **Recommended Equipment** -- Pick an OSRS Wiki strategy page and compare its recommended gear and inventories against your synced inventory, worn equipment, and bank. The web app highlights owned items, shows your best available setup, tracks slot and inventory coverage, copies RuneLite Bank Tags import strings, and can filter your in-game bank to the relevant owned items.
- **Bank Organizer** -- Plan bank tab layouts from your synced bank in the browser. Search items, drag or bulk-move them between tabs (the filter maps your bank items against wiki categories), save and load layouts, undo moves, reset the view from your live RuneLite bank, and start a RuneLite sorting assistant that highlights each in-game move with skip, confirm, and stop controls.
- **Quest Calculator** -- Choose a target quest or popular account goal and see the full prerequisite path using your synced quest completion, skill levels, quest points, and combat level. It shows missing skill and quest requirements, readiness badges, expandable quest details, rewards, total quest points in the path, and manual completion toggles.
