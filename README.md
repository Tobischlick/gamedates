[![Java Version](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/projects/jdk/25)
![Tests](https://github.com/Tobischlick/gamedates/actions/workflows/gradle.yml/badge.svg)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Tobischlick/gamedates)
![GitHub](https://img.shields.io/github/license/Tobischlick/gamedates)
![GitHub top language](https://img.shields.io/github/languages/top/Tobischlick/gamedates)

# 🎾 Gamedates

## 📖 Introduction

**Gamedates** is an automated tool designed to bridge the gap between
the [nuLiga (Baden)](https://baden.liga.nu/) tennis portal and your digital calendar.

Manually entering league fixtures for multiple teams into a club calendar is time-consuming and prone to errors. This
tool solves that by:

1. **Scraping** live group data directly from nuLiga.
2. **Filtering** matches based on your specific club name.
3. **Synchronizing** those matches into a **Google Calendar** with full details (Time, Location, Opponent).
4. **Updating** existing entries, if a game is rescheduled.

## ⚙️ Configuration

The application uses a dual-layer configuration strategy: **Static metadata** in a YAML file and **Sensitive secrets**
via Environment Variables.

### 1. Sensitive Data (Environment Variables)

To prevent accidental leaks, the following values must be set in your operating system or CI/CD environment (GitHub
Actions/GitLab CI):

| Variable      | Description                             | Example                               |
|:--------------|:----------------------------------------|:--------------------------------------|
| `CALENDAR_ID` | The unique ID of your Google Calendar.  | `abc...xyz@group.calendar.google.com` |
| `HOME_TEAM`   | Your club name as it appears on nuLiga. | `My super-awesome club`               |

### 2. League Settings (`application.yml`)

Located at `src/main/resources/application.yml`. This file defines which leagues to scrape:

* **`pages`**: A list of URLs pointing to the nuLiga "Group" or "Table/Fixtures" pages.
    * *Note: Ensure URLs use `/` instead of `%2F` for proper parsing.*
* **`teams-with-index`**: A list of specific team names that have extra columns in their nuLiga table (like "Spielort"
  or "Platz"). This ensures the parser correctly offsets the data.
* **`posting-enabled`**: Set to `false` for dry runs. The app still compares scraped games against the calendar and
  prints planned creates and updates, but does not write to Google Calendar. Set to `true` to allow writes.

### 3. Google API Credentials

The application requires an authorized `credentials.json` file from
the [Google Cloud Console](https://console.cloud.google.com/):

1. Place the `credentials.json` in `src/main/resources/`.
2. On first execution, the app will prompt for OAuth2 authorization in your browser.
3. A `tokens/` directory will be created to store the session.

> ⚠️ **Security Warning:** Never check in your `credentials.json`, `application.yml`, or the `tokens/` folder to a
> public repository. Ensure they are listed in your `.gitignore`.

> ℹ️ **Headless / scheduled execution:** The OAuth flow opens a local browser popup
> (`LocalServerReceiver` on port 8888), so it cannot complete on a headless server or CI runner.
> To run this unattended (e.g. via cron), authorize once interactively on a machine with a browser,
> then copy the resulting `tokens/` directory alongside `credentials.json` to the headless
> environment before scheduling the job there.

## 🚀 How to Run

### Local Execution

Since this project uses **Java 25** and **Lombok**, ensure your environment is set up correctly before starting.

#### 1. Install Dependencies

Run the bundled Gradle wrapper (no local Gradle install required):

   ```bash
   ./gradlew build
   ```

#### 2. Prepare Configuration

Ensure ```src/main/resources/credentials.json``` is present.

#### 3. Set Up Environment Variables

Set ```CALENDAR_ID``` and ```HOME_TEAM``` in your run config.

#### 4. Launch the tool with

```bash
./gradlew run
```

Or, for a scheduled/unattended run, build the standalone jar once and reuse it:

```bash
./gradlew shadowJar
java -jar build/libs/gamedates-1.0.0-all.jar
```

### Running on GitHub Actions

[.github/workflows/daily-sync.yml](.github/workflows/daily-sync.yml) runs the sync once a day
(`workflow_dispatch` also lets you trigger it manually from the Actions tab). Since the runner is
headless and stateless, it restores `credentials.json` and an already-authorized `tokens/` store
from repo secrets on every run instead of doing the interactive OAuth flow.

**Before enabling the schedule:**

1. Make sure the OAuth consent screen for this app is in **Production** status in the
   [Google Cloud Console](https://console.cloud.google.com/) (APIs & Services → OAuth consent
   screen), not **Testing**. Refresh tokens for apps left in Testing expire after 7 days, which
   would silently break the daily run.
2. Authorize once locally as usual (see above) so `tokens/StoredCredential` exists, then get its
   base64:
   ```bash
   base64 -w0 tokens/StoredCredential
   ```
3. Set the following repo secrets (Settings → Secrets and variables → Actions, or via `gh secret set`):

   | Secret                              | Value                                              |
   |:-------------------------------------|:----------------------------------------------------|
   | `CALENDAR_ID`                        | Your Google Calendar ID                             |
   | `HOME_TEAM`                          | Your club name, as in the env var                   |
   | `GOOGLE_CREDENTIALS_JSON`            | Raw contents of `src/main/resources/credentials.json` |
   | `GOOGLE_TOKEN_STORED_CREDENTIAL_B64` | Output of the `base64 -w0 tokens/StoredCredential` command above |

   ```bash
   gh secret set CALENDAR_ID
   gh secret set HOME_TEAM
   gh secret set GOOGLE_CREDENTIALS_JSON < src/main/resources/credentials.json
   base64 -w0 tokens/StoredCredential | gh secret set GOOGLE_TOKEN_STORED_CREDENTIAL_B64
   ```
4. Trigger the workflow once manually (Actions tab → Daily Calendar Sync → Run workflow) to
   confirm it authenticates and runs cleanly before relying on the daily schedule.

The refresh token doesn't need to be rotated on a regular basis — as long as the consent screen
stays in Production, it keeps working via automatic access-token refresh on each run.