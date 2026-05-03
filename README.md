# Welcome to gamedates

## Config in [application.yml](src/main/resources/application.yml)

- pages: Link to the leagues (
  i.e.: https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+S+2023&group=15)
    - if there is a '%2F' in the url, you have to fix it to '/'
- teams-with-index: List all teams with a special index (since some leagues do have two additional fields
  in their table, 'Spielort', 'Platz')
- posting-enabled: if set to true, the posting to the google api is enabled (is false by default)
- home-team: Name (or at least part of the name) of the home team

## Config in system environment

- The calendar id has to be set as an environment variable, called "CALENDAR_ID"
- The home team has to be set as an environment variabe, called "HOME_TEAM"

## Authentication

You have to download a credentials.json from google cloud console, therefore you have to be an authorized person.
Read the documentation by google (https://developers.google.com/calendar/api/quickstart/java) for more informations.

If there is a problem regarding authentication delete the "tokens" folder and run it again. Refresh token from google
cloud is only valid for 6 months.

## Run

Just run the main method in [FetchGameDates](src/main/java/api/FetchGameDates.java)

## General Information

- Feel free to contact me in case of questions, bug's for ideas for features.

## Open issues

See [Issues](https://gitlab.com/Tobischlick/gamedates/-/issues). Feel free to add an issue.
