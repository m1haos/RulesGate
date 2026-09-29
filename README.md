# RulesGate

Server rules in a native Minecraft dialog, shown before the player enters the world. Players who decline are disconnected; those who accept never see the rules again until you change them.

[Русская версия](README.ru.md)

## Features

- Rules appear during connection, before the world loads, so there is nothing to freeze or restrict
- Accept and decline buttons; the dialog can't be closed with Escape
- Rules in the player's client language: `rules/ru.yml` for Russian clients, `rules/de.yml` for German, and so on
- Change the rules and raise `rules-version`: everyone is asked again, online players right away after `/rules reload`
- Every acceptance is stored with its version and time; `/rules check <player>` shows it
- `/rules` opens the rules in game at any time
- No database: acceptances are kept in the player's own data file
- MiniMessage formatting, English and Russian messages

## How it compares to the vanilla code of conduct

Since 1.21.9 vanilla servers can show a plain-text code of conduct. RulesGate adds formatting, a record of who accepted which version and when, asking again after the rules change, `/rules` in game and a clear message for players who decline.

## Installation

1. Download the jar from [Releases](https://github.com/m1haos/RulesGate/releases) and put it into `plugins/`.
2. Start the server once. `config.yml`, `rules/` and `lang/` appear in `plugins/RulesGate/`.
3. Write your rules in `rules/en.yml` (and other languages if you need them), then run `/rules reload`.

## Commands and permissions

| Command | Permission | Default | What it does |
|---|---|---|---|
| `/rules` | `rulesgate.command.rules` | everyone | Read the rules |
| `/rules reload` | `rulesgate.command.reload` | op | Reload settings, rules and messages; ask online players again if `rules-version` went up |
| `/rules check <player>` | `rulesgate.command.check` | op | Which rules version a player accepted and when |

`rulesgate.admin` gives both admin permissions.

## Changing the rules

1. Edit the files in `rules/`.
2. Raise `rules-version` in `config.yml`, for example from `1` to `2`.
3. Run `/rules reload`. Online players get the new rules immediately, everyone else on their next join.

Fixing a typo without asking everyone again: edit the text and leave `rules-version` as it is.

## Requirements

- Paper 26.2
- Java 25

On a Velocity or BungeeCord network each backend server keeps its own acceptances. Install RulesGate on the server players join first.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`.

## License

[MIT](LICENSE)
