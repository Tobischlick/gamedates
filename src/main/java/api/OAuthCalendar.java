package api;

import annotations.ForTestingOnly;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import model.Game;
import security.CalendarHelper;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

public class OAuthCalendar {

    private final Calendar service;
    private final String calendarId;
    private boolean postingEnabled = false;

    private static final String CLUB_MATCH_ID_KEY = "clubMatchId";

    @ForTestingOnly
    public OAuthCalendar(Calendar service, String calendarId) {
        this.service = service;
        this.calendarId = calendarId;
    }

    public OAuthCalendar(String calendarId, boolean postingEnabled) throws GeneralSecurityException, IOException {
        this.service = new CalendarHelper().buildService();
        this.calendarId = calendarId;
        this.postingEnabled = postingEnabled;
    }

    protected void generateAndPostEvents(List<Game> games) throws IOException {
        // Fetch all existing events including their clubMatchId
        Map<String, Event> existingEvens = getExistingEventsMap();
        System.out.printf("Found %s existing events in calendar%n", existingEvens.size());

        games.forEach(game -> {
            // Create a new event
            String clubMatchId = game.clubMatchId();
            Event newEvent = createEvent(game);

            if (!postingEnabled) {
                System.out.printf("Skipped processing %s: Posting disabled%n", clubMatchId);
                return;
            }

            try {
                // If game already exists in calendar
                if (existingEvens.containsKey(clubMatchId)) {
                    Event existingEvent = existingEvens.get(clubMatchId);

                    // Compare time
                    if (timeHasChanged(existingEvent, newEvent)) {
                        String oldTime = existingEvent.getStart().getDateTime().toString();
                        String newTime = newEvent.getStart().getDateTime().toString();
                        updateEventTime(existingEvent.getId(), newEvent);
                        System.out.printf("Updated time for event: %s (%s -> %s)%n",
                                game.clubMatchId() + " - " + game.createSummary(), oldTime, newTime);
                    } else {
                        System.out.printf("No changes for event: %s%n", game.clubMatchId() + " - " + game.createSummary());
                    }
                } else {
                    // New game
                    Event createdEvent = service.events().insert(calendarId, newEvent).execute();
                    System.out.printf("Created new event: %s (%s)%n", game.clubMatchId() + " - " + game.createSummary(), createdEvent.getHtmlLink());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    boolean timeHasChanged(Event existingEvent, Event newEvent) {
        String existingStart = existingEvent.getStart().getDateTime().toString();
        String newStart = newEvent.getStart().getDateTime().toString();
        return !existingStart.equals(newStart);
    }

    private void updateEventTime(String googleEventId, Event newEvent) throws IOException {
        // We only need to send the fields that changed
        Event patchData = new Event();
        patchData.setStart(newEvent.getStart());
        patchData.setEnd(newEvent.getEnd());

        service.events().patch(calendarId, googleEventId, patchData).execute();
    }

    Map<String, Event> getExistingEventsMap() throws IOException {
        return service.events()
                .list(calendarId)
                .setSingleEvents(true)
                .execute()
                .getItems()
                .stream()
                .filter(e -> e.getExtendedProperties() != null &&
                        e.getExtendedProperties().getShared() != null &&
                        e.getExtendedProperties().getShared().containsKey(CLUB_MATCH_ID_KEY))
                .collect(Collectors.toMap(
                        e -> e.getExtendedProperties().getShared().get(CLUB_MATCH_ID_KEY),
                        e -> e,
                        (existing, _) -> existing
                ));
    }

    Event createEvent(Game game) {
        Event event = new Event();

        Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
        extendedProperties.setShared(Map.of(CLUB_MATCH_ID_KEY, game.clubMatchId()));
        event.setExtendedProperties(extendedProperties);

        DateTime startDateTime = getDateTime(game, false);
        DateTime endDateTime = getDateTime(game, true);
        EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("Europe/Berlin");
        EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("Europe/Berlin");
        event.setStart(start);
        event.setEnd(end);
        event.setSummary(game.createSummary());
        event.setColorId("10");
        event.setTransparency("transparent");
        return event;
    }

    DateTime getDateTime(Game game, boolean isEnd) {
        String[] date = game.date().split("\\.");
        String[] time = game.time().split(":");
        // Day pattern: 18.06.2023
        String day = date[0];
        String month = date[1];
        String year = date[2];
        // Time pattern: 09:30
        String hour = time[0];
        if (isEnd) {
            // We calculate each game day with 6 hours
            hour = String.valueOf(Integer.parseInt(hour) + 6);
        }
        String minute = time[1];
        return new DateTime(String.format("%s-%s-%sT%s:%s:00+02:00", year, month, day, hour, minute));
    }
}