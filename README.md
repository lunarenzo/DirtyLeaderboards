<div align="center">
  
  [![romix42 - DirtyLeaderboards](https://img.shields.io/static/v1?label=romix42&message=DirtyLeaderboards&color=blue&logo=github)](https://github.com/romix42/DirtyLeaderboards "Go to GitHub repo")
  [![stars - DirtyLeaderboards](https://img.shields.io/github/stars/romix42/DirtyLeaderboards?style=social)](https://github.com/romix42/DirtyLeaderboards)

</div>
<br>

<div align="center">

[![Build](https://github.com/romix42/DirtyLeaderboards/actions/workflows/build.yml/badge.svg)](../../actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-CC_BY--NC_4.0-blue)](https://creativecommons.org/licenses/by-nc/4.0/)

</div>
<br>

[![donation](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate/?hosted_button_id=KPXD92CM944RL)
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/L3L71Q7HGY)

<br>
<div align="center">

![Banner](https://cdn.modrinth.com/data/cached_images/8e85c2461fca1bbe7a97c7e1bd3b186c283c6fae_0.webp)

</div>

# DirtyLeaderboards

> The ultimate™ leaderboards plugin. Allows you to make modern leaderboards easily.

## 📖 How It Works

Everything you see is just vanilla `BlockDisplay`/`TextDisplay` entities, positioned and animated
by the plugin every tick. Ten rows that slide in and out with
real player heads (`<head:uuid>`) and item/particle icons (`<sprite:atlas:path>`).

Displays cycle through however many leaderboards you configure, one at a time, with slide
animations between them. You can run as many separate displays as you want, each with its own
location and its own rotation.

## ⚠️ Important Notice

> **Compatibility:** Requires **Paper 1.21.9 or newer**. The row
> icons rely on the vanilla `sprite`/`head` text components added in 1.21.9, so this won't work
> on Spigot or older versions (They will show as ` ` ⬅ a space). 
>
> __[Click to view the list of sprites and their path.](https://www.gamergeeks.net/apps/minecraft/list-of-atlas-sprites/java/1.21.9)__

## ✨ Features

- 🖼️ **Fully entity-based displays**
- 🔁 **Multiple leaderboards per display**, rotating on a timer you control
- 🔢 **Three data sources** - vanilla Bukkit **statistics**, a custom **Skript** syntax and **PlaceholderAPI** placeholder
- 🎨 **Custom styling** - colors, icons (including animated ones, like the playtime clock), rank colors, number/time formatting are customizable for each leaderboard
- 🧩 **Skript integration** - effects, expressions, a condition and a rotate event
- 🔗 **PlaceholderAPI expansion**
- 📡 **A Bukkit event** (`LeaderboardRotateEvent`) for plugin developers
- ♻️ **Live reload** - `/dlb reload` reloads all three config files without a restart
- 🏷️ **Multiple displays**, each independently started, stopped, skipped or moved

## 🗃️ Leaderboard Sources

Each leaderboard in `leaderboards.yml` picks one `source`:

- **`statistic`** - pulls straight from a vanilla Bukkit statistic (kills, deaths, playtime, you
  name it). Can't be manipulated without another plugin.
- **`custom`** - Most commonly changed by our custom skript syntax.
- **`placeholder`** - modified by PlaceholderAPI placeholders. Offline players are resolved in
  small batches per tick (`placeholder-players-per-tick` in `config.yml`) so it doesn't slow down the
  main thread on servers with lots of players.

## 🛠️ Commands

Base command is `/dirtyleaderboards`.<br>
Aliases: `dlb`, `leaderboards`, `dirtyboards`, `boards`, `db`, `dirtyboard`, `leaderboard`, `lb`, `dlbs`.

If you only have one display configured, you can leave `<display>` empty and it'll default to your only leaderboard.

| Command | Description |
|---|---|
| `/dlb help` | Lists the commands below |
| `/dlb list` | Lists every display and whether it's running |
| `/dlb reload` | Reloads `config.yml`, `leaderboards.yml` and `messages.yml` |
| `/dlb start <display>` | Starts a display |
| `/dlb stop <display>` | Stops a display |
| `/dlb skip <display>` | Skips to the next leaderboard in the rotation |
| `/dlb move <display>` | Moves a display to where you're standing and facing |

## 🔐 Permission

| Permission | Description | Default |
|---|---|---|
| `dirtyleaderboards.admin` | Access to every `/dlb` subcommand | OP |

## 🧩 Skript

[![SkriptHubViewTheDocs](http://skripthub.net/static/addon/ViewTheDocsButton.png)](http://skripthub.net/docs/?addon=DirtyLeaderboards)

```ruby
# control a display
start the leaderboard display "main"
stop the leaderboard display "main"
skip the leaderboard display "main"

# check state
current leaderboard of display "main"
leaderboard display "main" is running

# read/write a single player's score on a "custom" source board
set leaderboard data of player in "points" to 100
add 5 to leaderboard data of player in "points"
remove 2 from leaderboard data of player in "points"
reset leaderboard data of player in "points"

# wipe an entire board for every player (not just yours)
clear the leaderboard data for leaderboard "points"

# react whenever any display rotates to a new leaderboard
on leaderboard rotate:
    broadcast "%rotated display id% just switched to %rotated leaderboard id%"
```

## 🔗 PlaceholderAPI

Registers as `%dirtyleaderboards_...%` once PlaceholderAPI is installed:

| Placeholder | Returns |
|---|---|
| `%dirtyleaderboards_top_<board>_<rank>_name%` | Name of the player at that rank |
| `%dirtyleaderboards_top_<board>_<rank>_value%` | Their value, formatted (e.g. `15.2k`) |
| `%dirtyleaderboards_top_<board>_<rank>_raw%` | Their value, unformatted |
| `%dirtyleaderboards_value_<board>%` | The viewing player's own value, formatted |
| `%dirtyleaderboards_raw_<board>%` | The viewing player's own value, unformatted |

## ⚙️ Configuration

- **`config.yml`** - global settings: caching, number/time formatting, and one `displays:` entry
  per billboard (location, rotation order, colors, block types).
- **`leaderboards.yml`** - the leaderboards themselves: source, title, icon, formatting, and
  stored values for `custom` boards.
- **`messages.yml`** - every message the plugin sends. Uses MiniMessage.

A single leaderboard entry looks like this:

```yaml
leaderboards:
  kills:
    enabled: true
    source: statistic
    statistic: player_kills
    title: "TOP KILLS"
    title-color: "#ee2a2a"
    value-color: "#FFD263"
    icon: "<sprite:items:item/diamond_sword>"
    icon-width: 12
    format: number
    minimum-value: 1
    duration-seconds: 15
```
---
<div align="center">

## 📄 License

This project uses the [CC BY-NC 4.0](LICENSE) license.
<br>

[![License](https://img.shields.io/badge/License-CC_BY--NC_4.0-blue)](LICENSE)
</div>
