[![Java Version](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/projects/jdk/25)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Tobischlick/gamedates)
![GitHub](https://img.shields.io/github/license/Tobischlick/gamedates)
![GitHub top language](https://img.shields.io/github/languages/top/Tobischlick/gamedates)

# 🎾 Gamedates

## 📖 Introduction

**nuLiga Game Date Sync** is an automated tool designed to bridge the gap between
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
* **`posting-enabled`**: Set to `false` for dry runs. Set to `true` to allow the app to write to the Google Calendar
  API.

### 3. Google API Credentials

The application requires an authorized `credentials.json` file from
the [Google Cloud Console](https://console.cloud.google.com/):

1. Place the `credentials.json` in `src/main/resources/`.
2. On first execution, the app will prompt for OAuth2 authorization in your browser.
3. A `tokens/` directory will be created to store the session.

> ⚠️ **Security Warning:** Never check in your `credentials.json`, `application.yml`, or the `tokens/` folder to a
> public repository. Ensure they are listed in your `.gitignore`.

## 🚀 How to Run

### Local Execution

Since this project uses **Java 25** and **Lombok**, ensure your environment is set up correctly before starting.

#### 1. Install Dependencies

Ensure you have Maven installed and run:

   ```bash
   mvn clean install
   ```

#### 2. Prepare Configuration

Ensure ```src/main/resources/application.yml``` and
```src/main/resources/credentials.json``` are present

#### 3. Set Up Environment Variables

Set ```CALENDAR_ID``` and ```HOME_TEAM``` in your run config.

#### 4. Launch the tool with 
```bash
mvn exec:java -Dexec.mainClass="api.FetchGameDates"
```