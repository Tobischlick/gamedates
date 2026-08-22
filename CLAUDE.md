# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**Gamedates** is a Java 25/Maven tool that scrapes tennis league fixtures from
the [nuLiga (Baden)](https://baden.liga.nu/) portal, filters them by club
name, and syncs them into a Google Calendar (creating new entries and
updating rescheduled ones). It's a scheduled scrape-and-sync job, not a
long-running service.

## Structure

- [src/main/java/api/FetchGameDates.java](src/main/java/api/FetchGameDates.java)
  — entrypoint. Loads config, parses all configured pages into `Game`s, then
  drives calendar sync.
- [src/main/java/api/OAuthCalendar.java](src/main/java/api/OAuthCalendar.java)
  — Google Calendar API wrapper (OAuth2 flow, create/update events).
- [src/main/java/config/ConfigReader.java](src/main/java/config/ConfigReader.java)
  — reads `application.yml` plus the `CALENDAR_ID`/`HOME_TEAM` env vars.
- [src/main/java/content/JsoupHelper.java](src/main/java/content/JsoupHelper.java)
  and [Parser.java](src/main/java/content/Parser.java) — fetch and parse the
  nuLiga HTML tables into `Game` objects.
- [src/main/java/model/Game.java](src/main/java/model/Game.java) — fixture
  data model.
- [src/main/java/security/CalendarHelper.java](src/main/java/security/CalendarHelper.java)
  — calendar credential/token handling.
- [src/main/resources/application.yml](src/main/resources/application.yml) —
  `pages`, `teams-with-index`, `posting-enabled` (set `false` for dry runs).
- `src/main/resources/credentials.json` and `tokens/` — gitignored, local
  Google OAuth secrets/session; never commit these.
- `src/test/java` — JUnit 5 + AssertJ, mirrors the main package structure.

## Running things locally

```bash
mvn clean install
```

```bash
mvn test
```

```bash
mvn exec:java -Dexec.mainClass="api.FetchGameDates"
```

Requires `CALENDAR_ID` and `HOME_TEAM` env vars, plus a valid
`src/main/resources/credentials.json` (see [README.md](README.md)).

## Conventions to preserve

- **Dry runs are the default-safe path**: `posting-enabled: false` in
  `application.yml` must still scrape and log planned creates/updates without
  writing to the calendar. Don't regress that when touching the sync logic.
- **Secrets stay out of git**: `credentials.json`, `tokens/`, and any real
  `application.yml` values (`CALENDAR_ID`, `HOME_TEAM`) are gitignored/env-only
  — never hardcode or commit them.
- Package-by-layer structure (`api`, `config`, `content`, `model`, `security`,
  `utils`) — put new code in the matching package rather than introducing new
  top-level packages for small additions.
- Tests mirror `src/main/java` package-for-package; keep new tests under the
  matching `src/test/java/<package>` path.

## Branching & commit conventions

**Branches**

- If a GitHub issue exists: `feature/ISSUE-XX-short-description` (e.g.
  `feature/ISSUE-21-log-changes-when-updating`).
- If there is no issue: `feature/NO-ISSUE-short-description`.

**Commits**

- Clean, short one-liners. No multi-paragraph bodies, no bullet-point
  changelogs in the commit message.

## Before starting any task

Always pull the latest `main` before doing anything in this repo (creating a
branch, editing files, etc.):

```bash
git checkout main && git pull
```
